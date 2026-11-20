package model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public abstract class Account {

    private static final AtomicLong transactionCounter = new AtomicLong(100000);

    private final String accountNumber;
    private final Customer owner;
    private final String branch;
    private final LocalDate dateOpened;
    private BigDecimal balance;
    private final List<Transaction> history = new ArrayList<>();

    protected Account(String accountNumber, Customer owner, String branch, BigDecimal openingDeposit) {
        if (owner == null) {
            throw new IllegalArgumentException("An account must belong to a customer.");
        }
        BigDecimal opening = (openingDeposit == null) ? BigDecimal.ZERO : openingDeposit;
        if (opening.signum() < 0) {
            throw new IllegalArgumentException("Opening deposit cannot be negative.");
        }
        checkDecimalPlaces(opening);

        this.accountNumber = Customer.requireText(accountNumber, "Account number");
        this.branch = Customer.requireText(branch, "Branch");
        this.owner = owner;
        this.dateOpened = LocalDate.now();
        this.balance = opening.setScale(2, RoundingMode.HALF_UP);

        owner.addAccount(this);
        record(TransactionType.ACCOUNT_OPENING,
                opening.signum() == 0 ? null : this.balance, null, null);
    }

    protected Account(String accountNumber, Customer owner, String branch, LocalDate dateOpened,
                      BigDecimal balance, List<Transaction> storedHistory) {
        if (owner == null) {
            throw new IllegalArgumentException("An account must belong to a customer.");
        }
        if (dateOpened == null || balance == null || balance.signum() < 0 || storedHistory == null) {
            throw new IllegalArgumentException("The stored account details are not valid.");
        }
        this.accountNumber = Customer.requireText(accountNumber, "Account number");
        this.branch = Customer.requireText(branch, "Branch");
        this.owner = owner;
        this.dateOpened = dateOpened;
        this.balance = balance.setScale(2, RoundingMode.HALF_UP);
        this.history.addAll(storedHistory);
        for (Transaction transaction : storedHistory) {
            advanceCounter(transaction.getTransactionId());
        }
        owner.addAccount(this);
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public Customer getOwner() {
        return owner;
    }

    public String getBranch() {
        return branch;
    }

    public LocalDate getDateOpened() {
        return dateOpened;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public List<Transaction> getTransactionHistory() {
        return Collections.unmodifiableList(history);
    }

    public abstract String getAccountType();

    public void deposit(BigDecimal amount) {
        deposit(amount, null);
    }

    public void deposit(BigDecimal amount, String reference) {
        BigDecimal value = validateAmount(amount);
        balance = balance.add(value);
        record(TransactionType.DEPOSIT, value, reference, null);
    }

    public abstract void withdraw(BigDecimal amount);

    protected void applyWithdrawal(BigDecimal amount) {
        BigDecimal value = validateAmount(amount);
        if (value.compareTo(balance) > 0) {
            throw new IllegalStateException(
                    "Insufficient funds. The withdrawal amount is more than the available balance.");
        }
        balance = balance.subtract(value);
        record(TransactionType.WITHDRAWAL, value, null, null);
    }

    Transaction creditInterest(BigDecimal interest, YearMonth period) {
        if (interest == null || interest.signum() < 0) {
            throw new IllegalArgumentException("Interest cannot be missing or negative.");
        }
        BigDecimal value = interest.setScale(2, RoundingMode.HALF_UP);
        balance = balance.add(value);
        return record(TransactionType.INTEREST_CREDIT, value, null, period);
    }

    protected static BigDecimal validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }
        checkDecimalPlaces(amount);
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private static void checkDecimalPlaces(BigDecimal amount) {
        if (amount.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("Amount cannot have more than two decimal places.");
        }
    }

    private static void advanceCounter(String transactionId) {
        try {
            long value = Long.parseLong(transactionId);
            transactionCounter.accumulateAndGet(value, Math::max);
        } catch (NumberFormatException ignored) {
        }
    }

    private Transaction record(TransactionType type, BigDecimal amount, String reference, YearMonth period) {
        String id = String.valueOf(transactionCounter.incrementAndGet());
        Transaction transaction = new Transaction(id, accountNumber, type, amount,
                LocalDateTime.now(), balance, reference, period);
        history.add(transaction);
        return transaction;
    }
}