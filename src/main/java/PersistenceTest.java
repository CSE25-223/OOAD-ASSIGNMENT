import dao.AccountDAO;
import dao.CustomerDAO;
import dao.Database;
import dao.UserAccountDAO;
import model.Account;
import model.AuthenticationService;
import model.BankingService;
import model.Customer;
import model.IndividualCustomer;
import model.Transaction;
import model.UserAccount;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.HashSet;
import java.util.Set;

public class PersistenceTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        Database database = Database.inMemory("restarttest");

        CustomerDAO customerDAO1 = new CustomerDAO(database);
        AuthenticationService auth1 = new AuthenticationService(
                new UserAccountDAO(database, customerDAO1), customerDAO1);
        BankingService bank1 = new BankingService(new AccountDAO(database, customerDAO1));

        IndividualCustomer neo = new IndividualCustomer(auth1.nextCustomerId(), "Neoyame", "Dibotelo",
                "244357893", "Plot 142, Gaborone", "76983170", "Arilyx Ltd", "Plot 5, Gaborone");
        auth1.register(neo, "neoyame", "passneo");
        Account savings = bank1.openAccount(neo, "Savings", "Gaborone Main", new BigDecimal("1000.00"));
        Account cheque = bank1.openAccount(neo, "Cheque", "Gaborone Main", new BigDecimal("2000.00"));
        bank1.deposit(savings, new BigDecimal("250.00"));
        bank1.withdraw(cheque, new BigDecimal("500.00"));
        YearMonth november = YearMonth.of(2026, 11);
        bank1.processMonthlyInterest(november);

        IndividualCustomer sebina = new IndividualCustomer(auth1.nextCustomerId(), "Neo", "Sebina",
                "987654321", "Plot 8, Francistown", "72345678");
        auth1.register(sebina, "sebina", "sebinapass");
        expectRejected("Failed login 1", () -> auth1.authenticate("sebina", "wrong"));
        expectRejected("Failed login 2", () -> auth1.authenticate("sebina", "wrong"));
        expectRejected("Failed login 3 locks the login", () -> auth1.authenticate("sebina", "wrong"));

        CustomerDAO customerDAO2 = new CustomerDAO(database);
        AuthenticationService auth2 = new AuthenticationService(
                new UserAccountDAO(database, customerDAO2), customerDAO2);
        BankingService bank2 = new BankingService(new AccountDAO(database, customerDAO2));

        UserAccount user = auth2.authenticate("neoyame", "passneo");
        check("Login works after the restart", "Neoyame Dibotelo".equals(user.getCustomer().getDisplayName()));
        check("Accounts are empty until they are loaded", user.getCustomer().getAccounts().isEmpty());
        bank2.loadAccounts(user.getCustomer());
        check("Both accounts are remembered", user.getCustomer().getAccounts().size() == 2);

        Account savings2 = find(user.getCustomer(), "Savings");
        Account cheque2 = find(user.getCustomer(), "Cheque");
        check("Savings balance includes the deposit and the interest", isEqual(savings2.getBalance(), "1250.63"));
        check("Cheque balance includes the withdrawal", isEqual(cheque2.getBalance(), "1500.00"));
        check("Savings history is complete", savings2.getTransactionHistory().size() == 3);
        check("Cheque history is complete", cheque2.getTransactionHistory().size() == 2);
        check("Opening date is remembered", savings.getDateOpened().equals(savings2.getDateOpened()));

        expectRejected("Savings still refuses withdrawals", () -> bank2.withdraw(savings2, new BigDecimal("10.00")));
        expectRejected("Cheque still refuses an overdraft", () -> bank2.withdraw(cheque2, new BigDecimal("1500.01")));
        bank2.deposit(savings2, new BigDecimal("10.00"));
        check("A deposit after the restart is saved",
                isEqual(bank2.findAccount(savings2.getAccountNumber()).getBalance(), "1260.63"));
        check("Transaction IDs stay unique", uniqueIds(savings2));

        bank2.processMonthlyInterest(november);
        check("Interest is not paid twice for the same month",
                isEqual(bank2.findAccount(savings2.getAccountNumber()).getBalance(), "1260.63"));

        expectRejected("A locked login is still locked after the restart",
                () -> auth2.authenticate("sebina", "sebinapass"));

        System.out.println();
        System.out.println("Passed: " + passed + "   Failed: " + failed);
    }

    private static Account find(Customer customer, String type) {
        for (Account account : customer.getAccounts()) {
            if (account.getAccountType().equals(type)) {
                return account;
            }
        }
        throw new IllegalStateException("No " + type + " account found.");
    }

    private static boolean uniqueIds(Account account) {
        Set<String> ids = new HashSet<>();
        for (Transaction t : account.getTransactionHistory()) {
            ids.add(t.getTransactionId());
        }
        return ids.size() == account.getTransactionHistory().size();
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