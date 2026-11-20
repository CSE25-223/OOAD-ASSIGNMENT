import dao.AccountDAO;
import dao.CustomerDAO;
import dao.Database;
import model.Account;
import model.ChequeAccount;
import model.Customer;
import model.IndividualCustomer;
import model.InterestBearing;
import model.InvestmentAccount;
import model.PayInterest;
import model.PaymentStatus;
import model.SavingsAccount;
import model.Transaction;
import model.TransactionType;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.YearMonth;
import java.util.List;

public class AccountDAOTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws SQLException {
        Database database = Database.inMemory("accounttest");
        CustomerDAO customerDAO = new CustomerDAO(database);
        AccountDAO dao = new AccountDAO(database, customerDAO);

        IndividualCustomer owner = new IndividualCustomer("1001", "Thato", "Kgosi",
                "123456789", "Plot 12, Gaborone", "71234567", "Acme Ltd", "Plot 5, Gaborone");
        customerDAO.create(owner);

        SavingsAccount savings = new SavingsAccount("453678203", owner, "Gaborone Main", new BigDecimal("1000.00"));
        InvestmentAccount investment = new InvestmentAccount("567890123", owner, "Gaborone Main", new BigDecimal("500.00"));
        ChequeAccount cheque = new ChequeAccount("678901234", owner, "Gaborone Main", new BigDecimal("2000.00"));
        dao.create(savings);
        dao.create(investment);
        dao.create(cheque);
        check("Three accounts were saved", dao.findAll().size() == 3);

        Account loadedSavings = dao.findByNumber("453678203").orElse(null);
        check("Savings account is read back as a savings account", loadedSavings instanceof SavingsAccount);
        check("Balance is read back", loadedSavings != null && isEqual(loadedSavings.getBalance(), "1000.00"));
        check("Branch and opening date are read back",
                loadedSavings != null && "Gaborone Main".equals(loadedSavings.getBranch())
                        && savings.getDateOpened().equals(loadedSavings.getDateOpened()));
        check("Opening transaction is read back",
                loadedSavings != null && loadedSavings.getTransactionHistory().size() == 1
                        && loadedSavings.getTransactionHistory().get(0).getType() == TransactionType.ACCOUNT_OPENING);
        check("Owner is read back",
                loadedSavings != null && "1001".equals(loadedSavings.getOwner().getCustomerId()));
        check("Investment and cheque types are read back",
                dao.findByNumber("567890123").orElse(null) instanceof InvestmentAccount
                        && dao.findByNumber("678901234").orElse(null) instanceof ChequeAccount);
        check("Unknown account is not found", dao.findByNumber("000000000").isEmpty());

        savings.deposit(new BigDecimal("250.00"), "Birthday gift");
        check("Save reports success", dao.save(savings));
        Account afterDeposit = dao.findByNumber("453678203").orElseThrow();
        check("New balance is saved", isEqual(afterDeposit.getBalance(), "1250.00"));
        check("Deposit is added to the history", afterDeposit.getTransactionHistory().size() == 2);
        Transaction deposit = afterDeposit.getTransactionHistory().get(1);
        check("Deposit details are read back",
                deposit.getType() == TransactionType.DEPOSIT && isEqual(deposit.getAmount(), "250.00")
                        && "Birthday gift".equals(deposit.getReference())
                        && isEqual(deposit.getResultingBalance(), "1250.00"));
        dao.save(savings);
        check("Saving twice does not copy the history",
                dao.findByNumber("453678203").orElseThrow().getTransactionHistory().size() == 2);

        cheque.withdraw(new BigDecimal("500.00"));
        dao.save(cheque);
        Account loadedCheque = dao.findByNumber("678901234").orElseThrow();
        check("Cheque withdrawal is saved", isEqual(loadedCheque.getBalance(), "1500.00"));
        expectRejected("Reloaded cheque account still refuses an overdraft",
                () -> loadedCheque.withdraw(new BigDecimal("1500.01")));
        check("Cheque balance unchanged after the refused overdraft", isEqual(loadedCheque.getBalance(), "1500.00"));
        expectRejected("Reloaded savings account still refuses withdrawals",
                () -> afterDeposit.withdraw(new BigDecimal("10.00")));
        check("Reloaded investment account earns 5%",
                isEqual(((InterestBearing) dao.findByNumber("567890123").orElseThrow()).calculateMonthlyInterest(), "25.00"));

        YearMonth november = YearMonth.of(2026, 11);
        check("Interest payment completes",
                new PayInterest("100001", savings, november).process() == PaymentStatus.COMPLETED);
        dao.save(savings);
        Account afterInterest = dao.findByNumber("453678203").orElseThrow();
        check("Interest is saved", isEqual(afterInterest.getBalance(), "1250.63"));
        List<Transaction> savingsHistory = afterInterest.getTransactionHistory();
        Transaction interest = savingsHistory.get(savingsHistory.size() - 1);
        check("Interest transaction and month are read back",
                interest.getType() == TransactionType.INTEREST_CREDIT && november.equals(interest.getInterestPeriod()));
        check("Same month is skipped after reloading",
                new PayInterest("100002", afterInterest, november).process() == PaymentStatus.SKIPPED);

        Customer fresh = customerDAO.findById("1001").orElseThrow();
        List<Account> accounts = dao.findByCustomer(fresh);
        check("Customer has three accounts", accounts.size() == 3);
        check("Accounts are linked to the customer", fresh.getAccounts().size() == 3);
        check("All accounts belong to that customer", accounts.stream().allMatch(a -> a.getOwner() == fresh));

        savings.deposit(new BigDecimal("100.00"));
        List<Transaction> pendingHistory = savings.getTransactionHistory();
        Transaction pending = pendingHistory.get(pendingHistory.size() - 1);
        try (Connection connection = database.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("INSERT INTO transactions (transaction_id, account_number, transaction_type, "
                    + "amount, date_time, resulting_balance) VALUES ('" + pending.getTransactionId()
                    + "', '678901234', 'DEPOSIT', 1.00, CURRENT_TIMESTAMP, 1.00)");
        }
        expectRejected("Save fails when a history row cannot be written", () -> dao.save(savings));
        check("Balance in the database is unchanged after the failed save",
                isEqual(dao.findByNumber("453678203").orElseThrow().getBalance(), "1250.63"));

        expectRejected("Saving the same account number twice is rejected", () -> dao.create(savings));

        IndividualCustomer unsaved = new IndividualCustomer("5555", "Neo", "Sebina",
                "987654321", "Plot 8, Francistown", "72345678");
        SavingsAccount orphan = new SavingsAccount("999999999", unsaved, "Gaborone Main", new BigDecimal("10.00"));
        expectRejected("Account for a customer that is not saved is rejected", () -> dao.create(orphan));
        check("Rejected account was not saved", dao.findByNumber("999999999").isEmpty());
        check("Saving an account that was never created reports failure", !dao.save(orphan));

        check("Delete reports success", dao.delete("678901234"));
        check("Deleted account is gone", dao.findByNumber("678901234").isEmpty());
        check("Its history is gone too", countTransactions(database, "678901234") == 0);
        check("Deleting an unknown account reports failure", !dao.delete("678901234"));
        check("Two accounts remain", dao.findAll().size() == 2);
        check("The customer is not deleted with the account", customerDAO.findById("1001").isPresent());

        System.out.println();
        System.out.println("Passed: " + passed + "   Failed: " + failed);
    }

    private static int countTransactions(Database database, String accountNumber) throws SQLException {
        try (Connection connection = database.getConnection(); Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(
                     "SELECT COUNT(*) FROM transactions WHERE account_number = '" + accountNumber + "'")) {
            rows.next();
            return rows.getInt(1);
        }
    }

    private static boolean isEqual(BigDecimal actual, String expected) {
        return actual.compareTo(new BigDecimal(expected)) == 0;
    }

    private static void check(String name, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("PASS: " + name);
        } else {
            failed++;
            System.out.println("FAIL: " + name);
        }
    }

    private static void expectRejected(String name, Runnable action) {
        try {
            action.run();
            failed++;
            System.out.println("FAIL: " + name + " (no error was thrown)");
        } catch (RuntimeException e) {
            passed++;
            System.out.println("PASS: " + name + "  ->  " + e.getMessage());
        }
    }
}