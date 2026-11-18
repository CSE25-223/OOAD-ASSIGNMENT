package model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * A savings account: accepts deposits, earns 0.05% interest each month,
 * and does not allow withdrawals.
 */
public class SavingsAccount extends Account implements InterestBearing {

    private static final BigDecimal MONTHLY_INTEREST_RATE = new BigDecimal("0.0005"); // 0.05%

    /** Opens a savings account with no opening deposit. */
    public SavingsAccount(String accountNumber, Customer owner, String branch) {
        super(accountNumber, owner, branch, BigDecimal.ZERO);
    }

    /** Opens a savings account with an opening deposit. */
    public SavingsAccount(String accountNumber, Customer owner, String branch,
                          BigDecimal openingDeposit) {
        super(accountNumber, owner, branch, openingDeposit);
    }

    @Override
    public String getAccountType() {
        return "Savings";
    }

    /** Savings accounts never allow withdrawals, so the balance is left unchanged. */
    @Override
    public void withdraw(BigDecimal amount) {
        throw new IllegalStateException(
                "Withdrawals are not allowed from a savings account. Your balance has not changed.");
    }

    @Override
    public BigDecimal calculateMonthlyInterest() {
        return getBalance().multiply(MONTHLY_INTEREST_RATE).setScale(2, RoundingMode.HALF_UP);
    }
}