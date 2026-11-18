import model.ChequeAccount;
import model.IndividualCustomer;
import model.InvestmentAccount;
import model.PayInterest;
import model.PaymentStatus;
import model.SavingsAccount;
import model.Transaction;
import model.TransactionType;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/**
 * Console test for monthly interest payments. Each check prints PASS or FAIL.
 */
public class InterestTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        IndividualCustomer owner = new IndividualCustomer("C001", "Thato", "Kgosi",
                "123456789", "Plot 12, Gaborone", "71234567", "Acme Ltd", "Plot 5, Gaborone");
        SavingsAccount savings = new SavingsAccount("SAV-001", owner, "Gaborone Main",
                new BigDecimal("1000.00"));
        InvestmentAccount investment = new InvestmentAccount("INV-001", owner, "Gaborone Main",
                new BigDecimal("500.00"));
        ChequeAccount cheque = new ChequeAccount("CHQ-001", owner, "Gaborone Main",
                new BigDecimal("2000.00"));

        YearMonth november = YearMonth.of(2026, 11);
        YearMonth december = YearMonth.of(2026, 12);

        // Savings: first payment for November
        PayInterest p1 = new PayInterest("PAY-001", savings, november);
        check("Savings interest is completed", p1.process() == PaymentStatus.COMPLETED);
        check("Savings balance is 1000.50", isEqual(savings.getBalance(), "1000.50"));
        check("Interest credit is recorded in history",
                lastType(savings) == TransactionType.INTEREST_CREDIT);

        // Savings: same month again must not pay twice
        PayInterest p2 = new PayInterest("PAY-002", savings, november);
        check("Same month again is skipped", p2.process() == PaymentStatus.SKIPPED);
        System.out.println("      Reason: " + p2.getStatusReason());
        check("No duplicate interest was added", isEqual(savings.getBalance(), "1000.50"));

        // Investment: 5%
        PayInterest p3 = new PayInterest("PAY-003", investment, november);
        check("Investment interest is completed", p3.process() == PaymentStatus.COMPLETED);
        check("Investment balance is 525.00", isEqual(investment.getBalance(), "525.00"));

        // Cheque: no interest
        PayInterest p4 = new PayInterest("PAY-004", cheque, november);
        check("Cheque account is skipped", p4.process() == PaymentStatus.SKIPPED);
        System.out.println("      Reason: " + p4.getStatusReason());
        check("Cheque balance is unchanged", isEqual(cheque.getBalance(), "2000.00"));

        // Savings: the next month is a new payment
        PayInterest p5 = new PayInterest("PAY-005", savings, december);
        check("Next month's interest is completed", p5.process() == PaymentStatus.COMPLETED);
        check("Savings balance is 1001.00", isEqual(savings.getBalance(), "1001.00"));

        // A payment can only be processed once
        try {
            p5.process();
            failed++;
            System.out.println("FAIL: Processing a payment twice is rejected (no error was thrown)");
        } catch (IllegalStateException e) {
            passed++;
            System.out.println("PASS: Processing a payment twice is rejected  ->  " + e.getMessage());
        }

        System.out.println();
        System.out.println("Savings account history:");
        for (Transaction t : savings.getTransactionHistory()) {
            System.out.println("  " + t.getDetails());
        }

        System.out.println();
        System.out.println("Passed: " + passed + "   Failed: " + failed);
    }

    private static TransactionType lastType(SavingsAccount account) {
        List<Transaction> history = account.getTransactionHistory();
        return history.get(history.size() - 1).getType();
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
}