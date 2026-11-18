package model;

/**
 * The stages of one monthly interest payment, as shown in the PayInterest state diagram.
 */
public enum PaymentStatus {
    PENDING,
    VALIDATING,
    CALCULATING,
    POSTING,
    COMPLETED,
    FAILED,
    SKIPPED
}