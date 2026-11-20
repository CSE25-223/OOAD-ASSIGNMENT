package model;

import dao.AccountDAO;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

public class BankingService {

    private final AccountDAO accountDAO;
    private final AtomicLong paymentCounter = new AtomicLong(100000);

    public BankingService(AccountDAO accountDAO) {
        if (accountDAO == null) {
            throw new IllegalArgumentException("An account data access object is required.");
        }
        this.accountDAO = accountDAO;
    }

    public Account openAccount(Customer owner, String type, String branch) {
        return openAccount(owner, type, branch, BigDecimal.ZERO);
    }

    public Account openAccount(Customer owner, String type, String branch, BigDecimal initialDeposit) {
        if (owner == null) {
            throw new IllegalArgumentException("An account must belong to a customer.");
        }
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("Account type is required.");
        }

        Account account = switch (type.trim().toLowerCase()) {
            case "savings" -> new SavingsAccount(nextAccountNumber(), owner, branch, initialDeposit);
            case "investment" -> new InvestmentAccount(nextAccountNumber(), owner, branch, initialDeposit);
            case "cheque" -> openCheque(owner, branch, initialDeposit);
            default -> throw new IllegalArgumentException(
                    "Unknown account type. Choose Savings, Investment or Cheque.");
        };

        try {
            accountDAO.create(account);
        } catch (RuntimeException e) {
            owner.removeAccount(account);
            throw e;
        }
        return account;
    }

    private ChequeAccount openCheque(Customer owner, String branch, BigDecimal initialDeposit) {
        if (!(owner instanceof IndividualCustomer individual)) {
            throw new IllegalArgumentException(
                    "A cheque account can only be opened for an employed individual customer.");
        }
        return new ChequeAccount(nextAccountNumber(), individual, branch, initialDeposit);
    }

    public void loadAccounts(Customer customer) {
        if (customer == null) {
            throw new IllegalArgumentException("A customer is required.");
        }
        accountDAO.findByCustomer(customer);
    }

    public void deposit(Account account, BigDecimal amount) {
        Account target = requireAccount(account);
        applyAndSave(target, () -> target.deposit(amount));
    }

    public void withdraw(Account account, BigDecimal amount) {
        Account target = requireAccount(account);
        applyAndSave(target, () -> target.withdraw(amount));
    }

    public void processMonthlyInterest(YearMonth period) {
        if (period == null) {
            throw new IllegalArgumentException("A month is required for interest processing.");
        }
        for (Account account : accountDAO.findAll()) {
            String paymentId = String.valueOf(paymentCounter.incrementAndGet());
            PaymentStatus status = new PayInterest(paymentId, account, period).process();
            if (status == PaymentStatus.COMPLETED) {
                accountDAO.save(account);
            }
        }
    }

    public Account findAccount(String accountNumber) {
        return accountDAO.findByNumber(accountNumber).orElse(null);
    }

    public List<Account> getAccounts() {
        return accountDAO.findAll();
    }

    private void applyAndSave(Account account, Runnable operation) {
        operation.run();
        boolean saved;
        try {
            saved = accountDAO.save(account);
        } catch (RuntimeException e) {
            reload(account.getOwner());
            throw e;
        }
        if (!saved) {
            throw new IllegalStateException("The account could not be found in the database.");
        }
    }

    private void reload(Customer owner) {
        try {
            accountDAO.findByCustomer(owner);
        } catch (RuntimeException ignored) {
        }
    }

    private String nextAccountNumber() {
        String number;
        do {
            number = String.valueOf(ThreadLocalRandom.current().nextInt(100_000_000, 1_000_000_000));
        } while (accountDAO.findByNumber(number).isPresent());
        return number;
    }

    private static Account requireAccount(Account account) {
        if (account == null) {
            throw new IllegalArgumentException("Account not found.");
        }
        return account;
    }
}