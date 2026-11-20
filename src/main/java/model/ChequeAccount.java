package model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ChequeAccount extends Account {

    public ChequeAccount(String accountNumber, IndividualCustomer owner, String branch) {
        super(accountNumber, requireEmployed(owner), branch, BigDecimal.ZERO);
    }

    public ChequeAccount(String accountNumber, IndividualCustomer owner, String branch,
                         BigDecimal openingDeposit) {
        super(accountNumber, requireEmployed(owner), branch, openingDeposit);
    }

    private ChequeAccount(String accountNumber, Customer owner, String branch, LocalDate dateOpened,
                          BigDecimal balance, List<Transaction> history) {
        super(accountNumber, owner, branch, dateOpened, balance, history);
    }

    public static ChequeAccount restore(String accountNumber, Customer owner, String branch,
                                        LocalDate dateOpened, BigDecimal balance,
                                        List<Transaction> history) {
        return new ChequeAccount(accountNumber, owner, branch, dateOpened, balance, history);
    }

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