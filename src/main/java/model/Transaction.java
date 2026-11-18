package model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Objects;

/**
 * One recorded event in an account's history.
 * A transaction never changes after it is created.
 */
public class Transaction {

    private final String transactionId;
    private final String accountNumber;
    private final TransactionType type;
    private final BigDecimal amount;            // null for events with no amount
    private final LocalDateTime dateTime;
    private final BigDecimal resultingBalance;
    private final String reference;             // optional note, may be null
    private final YearMonth interestPeriod;     // only set for interest credits

    public Transaction(String transactionId, String accountNumber, TransactionType type,
                       BigDecimal amount, LocalDateTime dateTime, BigDecimal resultingBalance,
                       String reference, YearMonth interestPeriod) {
        this.transactionId = Objects.requireNonNull(transactionId, "Transaction ID is required.");
        this.accountNumber = Objects.requireNonNull(accountNumber, "Account number is required.");
        this.type = Objects.requireNonNull(type, "Transaction type is required.");
        this.amount = amount == null ? null : amount.setScale(2, RoundingMode.HALF_UP);
        this.dateTime = Objects.requireNonNull(dateTime, "Date and time are required.");
        this.resultingBalance = Objects.requireNonNull(resultingBalance, "Resulting balance is required.")
                .setScale(2, RoundingMode.HALF_UP);
        this.reference = reference;
        this.interestPeriod = interestPeriod;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public TransactionType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public BigDecimal getResultingBalance() {
        return resultingBalance;
    }

    public String getReference() {
        return reference;
    }

    public YearMonth getInterestPeriod() {
        return interestPeriod;
    }

    /** Returns a one-line summary suitable for showing in a history list. */
    public String getDetails() {
        String amountText = (amount == null) ? "-" : "BWP " + amount.toPlainString();
        return transactionId + " | " + dateTime + " | " + type
                + " | " + amountText
                + " | Balance: BWP " + resultingBalance.toPlainString();
    }
}