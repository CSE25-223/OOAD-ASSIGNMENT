package model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

public class SavingsAccount extends Account implements InterestBearing {

    private static final BigDecimal MONTHLY_INTEREST_RATE = new BigDecimal("0.0005");

    public SavingsAccount(String accountNumber, Customer owner, String branch) {
        super(accountNumber, owner, branch, BigDecimal.ZERO);
    }

    public SavingsAccount(String accountNumber, Customer owner, String branch,
                          BigDecimal openingDeposit) {
        super(accountNumber, owner, branch, openingDeposit);
    }

    private SavingsAccount(String accountNumber, Customer owner, String branch, LocalDate dateOpened,
                           BigDecimal balance, List<Transaction> history) {
        super(accountNumber, owner, branch, dateOpened, balance, history);
    }

    public static SavingsAccount restore(String accountNumber, Customer owner, String branch,
                                         LocalDate dateOpened, BigDecimal balance,
                                         List<Transaction> history) {
        return new SavingsAccount(accountNumber, owner, branch, dateOpened, balance, history);
    }

    @Override
    public String getAccountType() {
        return "Savings";
    }

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