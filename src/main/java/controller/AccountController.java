package controller;

import model.Account;
import model.BankingService;
import model.Transaction;
import model.UserAccount;
import view.AccountView;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class AccountController {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String BRANCH = "Gaborone Main";

    private final AccountView view;
    private final BankingService bankingService;
    private final UserAccount user;

    public AccountController(AccountView view, BankingService bankingService, UserAccount user,
                             Runnable onLogout) {
        this.view = view;
        this.bankingService = bankingService;
        this.user = user;

        view.setCustomerName(user.getCustomer().getDisplayName());
        refreshAccounts();

        view.getOpenAccountButton().setOnAction(event -> handleOpenAccount());
        view.getDepositButton().setOnAction(event ->
                handleTransaction(bankingService::deposit, "Deposit successful."));
        view.getWithdrawButton().setOnAction(event ->
                handleTransaction(bankingService::withdraw, "Withdrawal successful."));
        view.getHistoryButton().setOnAction(event -> handleHistory());
        view.getLogoutButton().setOnAction(event -> onLogout.run());
    }

    private void handleOpenAccount() {
        try {
            BigDecimal openingDeposit = readMoney(view.getOpeningDepositText(), true);
            Account account = bankingService.openAccount(user.getCustomer(),
                    view.getSelectedAccountType(), BRANCH, openingDeposit);
            view.clearOpeningDeposit();
            refreshAccounts();
            showHistory(account);
            view.showMessage(account.getAccountType() + " account " + account.getAccountNumber()
                    + " opened.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            view.showError(e.getMessage());
        }
    }

    private void handleTransaction(BiConsumer<Account, BigDecimal> operation, String successMessage) {
        try {
            Account account = selectedAccount();
            operation.accept(account, readMoney(view.getAmountText(), false));
            view.showMessage(successMessage + " New balance: " + money(account.getBalance()));
            view.clearAmount();
            refreshAccounts();
            showHistory(account);
        } catch (IllegalArgumentException | IllegalStateException e) {
            view.showError(e.getMessage());
        }
    }

    private void handleHistory() {
        try {
            Account account = selectedAccount();
            showHistory(account);
            view.showMessage("Showing history for " + account.getAccountNumber() + ".");
        } catch (IllegalArgumentException | IllegalStateException e) {
            view.showError(e.getMessage());
        }
    }

    private Account selectedAccount() {
        String accountNumber = view.getSelectedAccountNumber();
        if (accountNumber == null) {
            throw new IllegalArgumentException("Select an account first.");
        }
        for (Account account : user.getCustomer().getAccounts()) {
            if (account.getAccountNumber().equals(accountNumber)) {
                return account;
            }
        }
        throw new IllegalArgumentException("That account does not belong to you.");
    }

    private BigDecimal readMoney(String text, boolean blankMeansZero) {
        if (text.isEmpty()) {
            if (blankMeansZero) {
                return BigDecimal.ZERO;
            }
            throw new IllegalArgumentException("Enter an amount, for example 250.00.");
        }
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Enter a valid amount, for example 250.00.");
        }
    }

    private void refreshAccounts() {
        List<AccountView.AccountRow> rows = new ArrayList<>();
        for (Account account : user.getCustomer().getAccounts()) {
            rows.add(new AccountView.AccountRow(account.getAccountType(),
                    account.getAccountNumber(), money(account.getBalance())));
        }
        view.setAccounts(rows);
    }

    private void showHistory(Account account) {
        List<AccountView.HistoryRow> rows = new ArrayList<>();
        for (Transaction t : account.getTransactionHistory()) {
            String amount = (t.getAmount() == null) ? "-" : money(t.getAmount());
            rows.add(new AccountView.HistoryRow(t.getTransactionId(),
                    t.getDateTime().format(DATE_FORMAT), t.getType().toString(),
                    amount, money(t.getResultingBalance())));
        }
        view.setHistory(rows);
    }

    private static String money(BigDecimal value) {
        return "BWP " + value.toPlainString();
    }
}