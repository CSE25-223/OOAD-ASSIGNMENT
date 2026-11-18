import model.ChequeAccount;
import model.CompanyCustomer;
import model.IndividualCustomer;
import model.InvestmentAccount;
import model.SavingsAccount;
import model.Transaction;

import java.math.BigDecimal;

/**
 * Console test for the core model. Each check prints PASS or FAIL.
 */
public class Main {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        // Customers
        IndividualCustomer employed = new IndividualCustomer("C001", "Thato", "Kgosi",
                "123456789", "Plot 12, Gaborone", "71234567", "Acme Ltd", "Plot 5, Gaborone");
        IndividualCustomer unemployed = new IndividualCustomer("C002", "Neo", "Sebina",
                "987654321", "Plot 8, Francistown", "72345678");
        CompanyCustomer company = new CompanyCustomer("C003", "Delta Traders",
                "BW00012345", "Main Mall, Gaborone", "3901234");

        System.out.println("Display names: " + employed.getDisplayName()
                + " | " + company.getDisplayName());

        // Opening accounts
        SavingsAccount savings = new SavingsAccount("SAV-001", employed, "Gaborone Main");
        InvestmentAccount investment = new InvestmentAccount("INV-001", employed,
                "Gaborone Main", new BigDecimal("500.00"));
        ChequeAccount cheque = new ChequeAccount("CHQ-001", employed, "Gaborone Main");
        check("Customer holds all three account types", employed.getAccounts().size() == 3);

        // Deposits
        savings.deposit(new BigDecimal("1000.00"));
        check("Savings deposit updates balance", isEqual(savings.getBalance(), "1000.00"));
        savings.deposit(new BigDecimal("50.00"), "Birthday gift");
        check("Deposit with a reference works", isEqual(savings.getBalance(), "1050.00"));

        // Savings rules
        expectRejected("Savings withdrawal is rejected", () -> savings.withdraw(new BigDecimal("10.00")));
        check("Savings balance unchanged after rejected withdrawal",
                isEqual(savings.getBalance(), "1050.00"));
        check("Savings interest is 0.05% (1050.00 -> 0.53)",
                isEqual(savings.calculateMonthlyInterest(), "0.53"));

        // Investment rules
        expectRejected("Investment below BWP 500.00 is rejected",
                () -> new InvestmentAccount("INV-002", employed, "Gaborone Main", new BigDecimal("499.99")));
        check("Rejected investment account was not added to the customer",
                employed.getAccounts().size() == 3);
        check("Investment interest is 5% (500.00 -> 25.00)",
                isEqual(investment.calculateMonthlyInterest(), "25.00"));
        investment.withdraw(new BigDecimal("200.00"));
        check("Investment withdrawal updates balance", isEqual(investment.getBalance(), "300.00"));

        // Cheque rules
        cheque.deposit(new BigDecimal("2500.00"));
        cheque.withdraw(new BigDecimal("500.00"));
        check("Cheque deposit and withdrawal work", isEqual(cheque.getBalance(), "2000.00"));
        expectRejected("Cheque overdraft is rejected", () -> cheque.withdraw(new BigDecimal("2000.01")));
        check("Cheque balance unchanged after rejected overdraft", isEqual(cheque.getBalance(), "2000.00"));
        expectRejected("Cheque account for unemployed customer is rejected",
                () -> new ChequeAccount("CHQ-002", unemployed, "Gaborone Main"));

        // Invalid amounts
        expectRejected("Zero deposit is rejected", () -> cheque.deposit(BigDecimal.ZERO));
        expectRejected("Negative deposit is rejected", () -> cheque.deposit(new BigDecimal("-5.00")));
        expectRejected("Deposit with 3 decimal places is rejected", () -> cheque.deposit(new BigDecimal("1.005")));
        expectRejected("Missing amount is rejected", () -> cheque.deposit(null));

        // Missing customer details
        expectRejected("Customer with a blank address is rejected",
                () -> new CompanyCustomer("C004", "Echo Ltd", "BW000999", "  ", "3900000"));

        // Company customer can hold accounts too
        SavingsAccount companySavings = new SavingsAccount("SAV-002", company, "Gaborone Main",
                new BigDecimal("5000.00"));
        check("Company can open a savings account", isEqual(companySavings.getBalance(), "5000.00"));

        // Transaction history
        System.out.println();
        System.out.println("Cheque account history:");
        for (Transaction t : cheque.getTransactionHistory()) {
            System.out.println("  " + t.getDetails());
        }

        System.out.println();
        System.out.println("Passed: " + passed + "   Failed: " + failed);
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

    /** A check passes when the action is refused with an error. */
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