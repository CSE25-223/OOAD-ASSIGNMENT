package model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

public class InvestmentAccount extends Account implements InterestBearing {

    private static final BigDecimal MIN_OPENING_DEPOSIT = new BigDecimal("500.00");
    private static final BigDecimal MONTHLY_INTEREST_RATE = new BigDecimal("0.05");

    public InvestmentAccount(String accountNumber, Customer owner, String branch,
                             BigDecimal openingDeposit) {
        super(accountNumber, owner, branch, requireMinimumDeposit(openingDeposit));
    }

    private InvestmentAccount(String accountNumber, Customer owner, String branch, LocalDate dateOpened,
                              BigDecimal balance, List<Transaction> history) {
        super(accountNumber, owner, branch, dateOpened, balance, history);
    }

    public static InvestmentAccount restore(String accountNumber, Customer owner, String branch,
                                            LocalDate dateOpened, BigDecimal balance,
                                            List<Transaction> history) {
        return new InvestmentAccount(accountNumber, owner, branch, dateOpened, balance, history);
    }

    private static BigDecimal requireMinimumDeposit(BigDecimal openingDeposit) {
        if (openingDeposit == null || openingDeposit.compareTo(MIN_OPENING_DEPOSIT) < 0) {
            throw new IllegalArgumentException(
                    "An investment account needs an opening deposit of at least BWP 500.00.");
        }
        return openingDeposit;
    }

    @Override
    public String getAccountType() {
        return "Investment";
    }

    @Override
    public void withdraw(BigDecimal amount) {
        applyWithdrawal(amount);
    }

    @Override
    public BigDecimal calculateMonthlyInterest() {
        return getBalance().multiply(MONTHLY_INTEREST_RATE).setScale(2, RoundingMode.HALF_UP);
    }
}