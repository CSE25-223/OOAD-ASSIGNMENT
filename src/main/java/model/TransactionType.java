package model;

public enum TransactionType {
    ACCOUNT_OPENING("Account Opening"),
    DEPOSIT("Deposit"),
    WITHDRAWAL("Withdrawal"),
    INTEREST_CREDIT("Interest Credit");

    private final String label;

    TransactionType(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}