package model;

import java.math.BigDecimal;
import java.time.YearMonth;

/**
 * One interest payment for one account and one month.
 * It moves through the stages Pending, Validating, Calculating and Posting,
 * and ends as Completed, Skipped or Failed.
 */
public class PayInterest {

    private final String paymentId;
    private final Account account;
    private final YearMonth period;
    private PaymentStatus status = PaymentStatus.PENDING;
    private BigDecimal calculatedAmount;
    private String statusReason;

    public PayInterest(String paymentId, Account account, YearMonth period) {
        if (account == null) {
            throw new IllegalArgumentException("An interest payment needs an account.");
        }
        if (period == null) {
            throw new IllegalArgumentException("An interest payment needs a month.");
        }
        this.paymentId = Customer.requireText(paymentId, "Payment ID");
        this.account = account;
        this.period = period;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public Account getAccount() {
        return account;
    }

    public YearMonth getPeriod() {
        return period;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public BigDecimal getCalculatedAmount() {
        return calculatedAmount;
    }

    /** Explains why a payment was skipped or failed. Null if it completed normally. */
    public String getStatusReason() {
        return statusReason;
    }

    /** Runs the payment from Pending to its final state. A payment can only be processed once. */
    public PaymentStatus process() {
        if (status != PaymentStatus.PENDING) {
            throw new IllegalStateException(
                    "This payment has already been processed. Use retry() if it failed.");
        }
        return run();
    }

    /** Runs a failed payment again. Validation checks the history first, so interest is never paid twice. */
    public PaymentStatus retry() {
        if (status != PaymentStatus.FAILED) {
            throw new IllegalStateException("Only a failed payment can be retried.");
        }
        status = PaymentStatus.PENDING;
        statusReason = null;
        return run();
    }

    private PaymentStatus run() {
        try {
            status = PaymentStatus.VALIDATING;
            if (!validateEligibility()) {
                status = PaymentStatus.SKIPPED;
                return status;
            }
            status = PaymentStatus.CALCULATING;
            calculate();
            status = PaymentStatus.POSTING;
            postAtomically();
            status = PaymentStatus.COMPLETED;
        } catch (RuntimeException e) {
            status = PaymentStatus.FAILED;
            statusReason = e.getMessage();
        }
        return status;
    }

    /** An account is eligible if it earns interest and has not already been credited for this month. */
    public boolean validateEligibility() {
        if (!(account instanceof InterestBearing)) {
            statusReason = account.getAccountType() + " accounts do not earn interest.";
            return false;
        }
        for (Transaction t : account.getTransactionHistory()) {
            if (t.getType() == TransactionType.INTEREST_CREDIT && period.equals(t.getInterestPeriod())) {
                statusReason = "Interest for " + period + " has already been credited.";
                return false;
            }
        }
        return true;
    }

    /** Asks the account for its monthly interest, using that account type's own rate. */
    public BigDecimal calculate() {
        calculatedAmount = ((InterestBearing) account).calculateMonthlyInterest();
        return calculatedAmount;
    }

    /** Adds the interest to the balance and records it in the history together. */
    public void postAtomically() {
        if (calculatedAmount == null) {
            throw new IllegalStateException("Interest must be calculated before it is posted.");
        }
        account.creditInterest(calculatedAmount, period);
    }
}