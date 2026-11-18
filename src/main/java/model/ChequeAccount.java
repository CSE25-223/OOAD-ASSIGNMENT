package model;

import java.math.BigDecimal;

/**
 * A cheque account: used for salaries, allows deposits and withdrawals of any
 * amount (never below zero), and earns no interest. It can only be opened for an
 * employed individual customer.
 */
public class ChequeAccount extends Account {

    /** Opens a cheque account with no opening deposit. */
    public ChequeAccount(String accountNumber, IndividualCustomer owner, String branch) {
        super(accountNumber, requireEmployed(owner), branch, BigDecimal.ZERO);
    }

    /** Opens a cheque account with an opening deposit. */
    public ChequeAccount(String accountNumber, IndividualCustomer owner, String branch,
                         BigDecimal openingDeposit) {
        super(accountNumber, requireEmployed(owner), branch, openingDeposit);
    }

    /** Runs before the account is created, so an invalid account never exists. */
    private static IndividualCustomer requireEmployed(IndividualCustomer owner) {
        if (owner == null) {
            throw new IllegalArgumentException("A cheque account must belong to a customer.");
        }
        if (!owner.isEmployed()) {
            throw new IllegalArgumentException(
                    "A cheque account can only be opened for an employed customer "
                            + "who provides the employer's name and address.");
        }
        return owner;
    }

    @Override
    public String getAccountType() {
        return "Cheque";
    }

    @Override
    public void withdraw(BigDecimal amount) {
        applyWithdrawal(amount);
    }
}