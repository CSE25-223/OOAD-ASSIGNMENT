import dao.AccountDAO;
import dao.CustomerDAO;
import dao.Database;
import model.Account;
import model.BankingService;
import model.CompanyCustomer;
import model.IndividualCustomer;
import model.Transaction;

import java.math.BigDecimal;
import java.time.YearMonth;

public class BankingServiceTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        Database database = Database.inMemory("bankingtest");
        CustomerDAO customerDAO = new CustomerDAO(database);
        BankingService service = new BankingService(new AccountDAO(database, customerDAO));

        IndividualCustomer employed = new IndividualCustomer("1001", "Thato", "Kgosi",
                "123456789", "Plot 12, Gaborone", "71234567", "Acme Ltd", "Plot 5, Gaborone");
        IndividualCustomer unemployed = new IndividualCustomer("1002", "Neo", "Sebina",
                "987654321", "Plot 8, Francistown", "72345678");
        CompanyCustomer company = new CompanyCustomer("1003", "Delta Traders",
                "BW00012345", "Main Mall, Gaborone", "3901234");
        customerDAO.create(employed);
        customerDAO.create(unemployed);
        customerDAO.create(company);

        Account savings = service.openAccount(employed, "Savings", "Gaborone Main", new BigDecimal("1000.00"));
        Account investment = service.openAccount(employed, "investment", "Gaborone Main", new BigDecimal("500.00"));
        Account cheque = service.openAccount(employed, "Cheque", "Gaborone Main");

        check("Account numbers are unique",
                !savings.getAccountNumber().equals(investment.getAccountNumber())
                        && !investment.getAccountNumber().equals(cheque.getAccountNumber()));
        check("Account numbers are nine digits without leading zeros",
                savings.getAccountNumber().matches("[1-9][0-9]{8}"));
        check("Customer holds all three account types", employed.getAccounts().size() == 3);
        check("The three accounts were saved to the database", service.getAccounts().size() == 3);
        check("Account can be found by number",
                service.findAccount(savings.getAccountNumber()) != null
                        && savings.getAccountNumber().equals(service.findAccount(savings.getAccountNumber()).getAccountNumber()));
        check("Unknown account number is not found", service.findAccount("000000000") == null);

        service.deposit(cheque, new BigDecimal("2500.00"));
        service.withdraw(cheque, new BigDecimal("500.00"));
        check("Cheque deposit and withdrawal work", isEqual(cheque.getBalance(), "2000.00"));
        check("Cheque balance is saved", isEqual(saved(service, cheque).getBalance(), "2000.00"));
        check("Cheque history is saved", saved(service, cheque).getTransactionHistory().size() == 3);

        expectRejected("Savings withdrawal is rejected", () -> service.withdraw(savings, new BigDecimal("10.00")));
        expectRejected("Cheque overdraft is rejected", () -> service.withdraw(cheque, new BigDecimal("5000.00")));
        check("Rejected withdrawals leave the saved balances unchanged",
                isEqual(saved(service, savings).getBalance(), "1000.00")
                        && isEqual(saved(service, cheque).getBalance(), "2000.00"));
        expectRejected("Investment without an opening deposit is rejected",
                () -> service.openAccount(employed, "Investment", "Gaborone Main"));
        expectRejected("Cheque account for unemployed customer is rejected",
                () -> service.openAccount(unemployed, "Cheque", "Gaborone Main"));
        expectRejected("Cheque account for a company is rejected",
                () -> service.openAccount(company, "Cheque", "Gaborone Main"));
        expectRejected("Unknown account type is rejected",
                () -> service.openAccount(employed, "Gold", "Gaborone Main"));
        expectRejected("Missing customer is rejected",
                () -> service.openAccount(null, "Savings", "Gaborone Main"));
        check("Rejected accounts were not saved",
                service.getAccounts().size() == 3 && employed.getAccounts().size() == 3);

        YearMonth november = YearMonth.of(2026, 11);
        service.processMonthlyInterest(november);
        check("Savings got 0.05% interest", isEqual(saved(service, savings).getBalance(), "1000.50"));
        check("Investment got 5% interest", isEqual(saved(service, investment).getBalance(), "525.00"));
        check("Cheque got no interest", isEqual(saved(service, cheque).getBalance(), "2000.00"));

        service.processMonthlyInterest(november);
        check("Running the same month again pays nothing twice",
                isEqual(saved(service, savings).getBalance(), "1000.50"));

        System.out.println();
        System.out.println("Savings account history:");
        for (Transaction t : saved(service, savings).getTransactionHistory()) {
            System.out.println("  " + t.getDetails());
        }

        System.out.println();
        System.out.println("Passed: " + passed + "   Failed: " + failed);
    }

    private static Account saved(BankingService service, Account account) {
        return service.findAccount(account.getAccountNumber());
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