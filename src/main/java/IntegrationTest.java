import controller.AccountController;
import controller.LoginController;
import controller.RegistrationController;
import dao.AccountDAO;
import dao.CustomerDAO;
import dao.Database;
import dao.UserAccountDAO;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import model.Account;
import model.AuthenticationService;
import model.BankingService;
import model.IndividualCustomer;
import model.Role;
import model.UserAccount;
import view.AccountView;
import view.LoginView;
import view.RegistrationView;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class IntegrationTest {

    private static final String BRANCH = "Gaborone Main";
    private static final String NO_SELECTION = "Select an account first.";

    private static int passed = 0;
    private static int failed = 0;

    private static Database database;
    private static CustomerDAO customerDAO;
    private static UserAccountDAO userDAO;
    private static AccountDAO accountDAO;
    private static AuthenticationService auth;
    private static BankingService bank;

    private static String savingsNumber;
    private static String investmentNumber;
    private static String chequeNumber;
    private static UserAccount neoyameUser;

    public static void main(String[] args) {
        try {
            Platform.startup(() -> {
                try {
                    runTests();
                } catch (Throwable e) {
                    failed++;
                    System.out.println("FAIL: The test stopped with an unexpected error: " + e);
                    e.printStackTrace(System.out);
                } finally {
                    System.out.println();
                    System.out.println("Passed: " + passed + "   Failed: " + failed);
                    Platform.exit();
                }
            });
        } catch (UnsupportedOperationException e) {
            System.out.println("This test opens real JavaFX screens, so it needs a computer with a display.");
        }
    }

    private static void runTests() {
        database = Database.inMemory("integrationtest");
        startApplication();
        seedData();
        loginTests();
        accountScreenTests();
        registrationTests();
        restartTests();
    }

    private static void startApplication() {
        customerDAO = new CustomerDAO(database);
        userDAO = new UserAccountDAO(database, customerDAO);
        accountDAO = new AccountDAO(database, customerDAO);
        auth = new AuthenticationService(userDAO, customerDAO);
        bank = new BankingService(accountDAO);
    }

    private static void seedData() {
        IndividualCustomer neoyame = new IndividualCustomer("1001", "Neoyame", "Dibotelo",
                "244357893", "Plot 142, Gaborone", "76983170", "Arilyx Ltd", "Plot 5, Gaborone");
        auth.addUser(new UserAccount("9001", "Neoyame", "passneo", Role.CUSTOMER, neoyame));
        savingsNumber = bank.openAccount(neoyame, "Savings", BRANCH, new BigDecimal("1000.00")).getAccountNumber();
        investmentNumber = bank.openAccount(neoyame, "Investment", BRANCH, new BigDecimal("500.00")).getAccountNumber();
        chequeNumber = bank.openAccount(neoyame, "Cheque", BRANCH, new BigDecimal("2000.00")).getAccountNumber();

        IndividualCustomer sebina = new IndividualCustomer("1002", "Neo", "Sebina",
                "421987654", "Plot 8, Francistown", "72345678");
        auth.addUser(new UserAccount("9004", "sebina", "sebina123", Role.CUSTOMER, sebina));
        bank.openAccount(sebina, "Savings", BRANCH, new BigDecimal("300.00"));

        auth.addUser(new UserAccount("9002", "teller", "staffpass", Role.BANK_STAFF, null));
    }

    private static void loginTests() {
        section("1. Login: LoginView, LoginController, AuthenticationService and the database");

        AtomicReference<UserAccount> result = new AtomicReference<>();
        LoginView screen = loginScreen(result);

        submitLogin(screen, "Neoyame", "wrong");
        check("A wrong password shows an error on the screen",
                hasLabel(screen.getRoot(), "Invalid username or password."));
        check("A wrong password does not open the account screen", result.get() == null);
        check("The failed attempt is saved in the database",
                userDAO.findByUsername("Neoyame").orElseThrow().getFailedAttempts() == 1);

        submitLogin(screen, "Neoyame", "passneo");
        neoyameUser = result.get();
        check("The correct password logs in and returns the customer",
                neoyameUser != null && "Neoyame Dibotelo".equals(neoyameUser.getCustomer().getDisplayName()));
        check("A successful login clears the saved failures",
                userDAO.findByUsername("Neoyame").orElseThrow().getFailedAttempts() == 0);

        AtomicReference<UserAccount> staffResult = new AtomicReference<>();
        LoginView staffScreen = loginScreen(staffResult);
        submitLogin(staffScreen, "teller", "staffpass");
        check("A bank staff login is told the staff screens are not available",
                hasLabel(staffScreen.getRoot(), "Bank staff screens are not available yet.")
                        && staffResult.get() == null);

        AtomicReference<UserAccount> lockResult = new AtomicReference<>();
        LoginView lockScreen = loginScreen(lockResult);
        submitLogin(lockScreen, "sebina", "bad");
        submitLogin(lockScreen, "sebina", "bad");
        submitLogin(lockScreen, "sebina", "bad");
        check("The third wrong password locks the login",
                hasLabel(lockScreen.getRoot(),
                        "Too many failed attempts. This account is now locked. Please contact bank staff to unlock it."));
        check("The lock is saved in the database", userDAO.findByUsername("sebina").orElseThrow().isLocked());
        submitLogin(lockScreen, "sebina", "sebina123");
        check("The correct password is refused while the login is locked",
                hasLabel(lockScreen.getRoot(), "This account is locked. Please contact bank staff to unlock it.")
                        && lockResult.get() == null);
    }

    private static void accountScreenTests() {
        section("2. Account screen: AccountView, AccountController, BankingService, model and database");

        bank.loadAccounts(neoyameUser.getCustomer());
        AccountView view = new AccountView();
        AtomicBoolean loggedOut = new AtomicBoolean(false);
        new AccountController(view, bank, neoyameUser, () -> loggedOut.set(true));

        check("The heading shows the customer's name", hasLabel(view.getRoot(), "Welcome, Neoyame Dibotelo"));
        check("The accounts table shows the three accounts from the database", accountRows(view).size() == 3);

        selectAccount(view, savingsNumber);
        setAmount(view, "250.00");
        view.getDepositButton().fire();
        check("The screen confirms the deposit",
                hasLabel(view.getRoot(), "Deposit successful. New balance: BWP 1250.00"));
        check("The accounts table shows the new balance", "BWP 1250.00".equals(balanceOf(view, savingsNumber)));
        check("The history table shows the deposit",
                historyRows(view).stream().anyMatch(
                        row -> "Deposit".equals(row.type()) && "BWP 250.00".equals(row.amount())));
        Account savedSavings = accountDAO.findByNumber(savingsNumber).orElseThrow();
        check("The database holds the new Savings balance", isEqual(savedSavings.getBalance(), "1250.00"));
        check("The database holds the deposit in the history", savedSavings.getTransactionHistory().size() == 2);

        selectAccount(view, savingsNumber);
        setAmount(view, "10.00");
        view.getWithdrawButton().fire();
        check("A Savings withdrawal is refused on the screen",
                hasLabel(view.getRoot(),
                        "Withdrawals are not allowed from a savings account. Your balance has not changed."));
        check("The Savings balance in the database is unchanged",
                isEqual(accountDAO.findByNumber(savingsNumber).orElseThrow().getBalance(), "1250.00"));

        selectAccount(view, chequeNumber);
        setAmount(view, "2500");
        view.getWithdrawButton().fire();
        check("A Cheque overdraft is refused on the screen",
                hasLabel(view.getRoot(),
                        "Insufficient funds. The withdrawal amount is more than the available balance."));
        check("The Cheque balance in the database is unchanged",
                isEqual(accountDAO.findByNumber(chequeNumber).orElseThrow().getBalance(), "2000.00"));

        selectAccount(view, chequeNumber);
        setAmount(view, "abc");
        view.getDepositButton().fire();
        check("An amount that is not a number is refused",
                hasLabel(view.getRoot(), "Enter a valid amount, for example 250.00."));

        accountsTable(view).getSelectionModel().clearSelection();
        setAmount(view, "10.00");
        view.getDepositButton().fire();
        check("A deposit with no account selected is refused", hasLabel(view.getRoot(), NO_SELECTION));

        selectAccount(view, investmentNumber);
        setAmount(view, "200.00");
        view.getWithdrawButton().fire();
        check("The screen confirms the Investment withdrawal",
                hasLabel(view.getRoot(), "Withdrawal successful. New balance: BWP 300.00"));
        Account savedInvestment = accountDAO.findByNumber(investmentNumber).orElseThrow();
        check("The database holds the new Investment balance", isEqual(savedInvestment.getBalance(), "300.00"));
        check("The database holds the withdrawal in the history", savedInvestment.getTransactionHistory().size() == 2);

        typeBox(view).setValue("Savings");
        setOpeningDeposit(view, "300.00");
        view.getOpenAccountButton().fire();
        check("The screen confirms the new Savings account",
                hasLabelMatching(view.getRoot(), "Savings account [0-9]{9} opened\\."));
        check("The accounts table shows four accounts", accountRows(view).size() == 4);
        check("The database holds four accounts for the customer",
                accountDAO.findByCustomer(customerDAO.findById("1001").orElseThrow()).size() == 4);

        typeBox(view).setValue("Investment");
        setOpeningDeposit(view, "400.00");
        view.getOpenAccountButton().fire();
        check("An Investment account below BWP 500.00 is refused on the screen",
                hasLabel(view.getRoot(),
                        "An investment account needs an opening deposit of at least BWP 500.00."));
        check("The refused account is not on the screen", accountRows(view).size() == 4);
        check("The refused account is not in the database",
                accountDAO.findByCustomer(customerDAO.findById("1001").orElseThrow()).size() == 4);

        view.getLogoutButton().fire();
        check("The Log out button returns to the login screen", loggedOut.get());
    }

    private static void registrationTests() {
        section("3. Registration: RegistrationView, RegistrationController and the database");

        RegistrationView view = new RegistrationView();
        AtomicBoolean backClicked = new AtomicBoolean(false);
        new RegistrationController(view, auth, () -> backClicked.set(true));

        fillRegistration(view, "keabetswe", "keapass1", "different1");
        view.getRegisterButton().fire();
        check("Passwords that do not match are refused",
                hasLabel(view.getRoot(), "The two passwords do not match."));
        check("Nothing is saved when the passwords do not match",
                userDAO.findByUsername("keabetswe").isEmpty());

        fillRegistration(view, "keabetswe", "keapass1", "keapass1");
        view.getRegisterButton().fire();
        check("The screen confirms the registration",
                hasLabel(view.getRoot(), "Registration successful. You can now go back and log in."));
        UserAccount registered = userDAO.findByUsername("keabetswe").orElse(null);
        check("The new login is saved in the database",
                registered != null && registered.getRole() == Role.CUSTOMER);
        check("The new customer is saved in the database",
                registered != null && "Keabetswe Moeng".equals(registered.getCustomer().getDisplayName()));

        int customersBefore = customerDAO.findAll().size();
        fillRegistration(view, "keabetswe", "keapass1", "keapass1");
        view.getRegisterButton().fire();
        check("A username that is already taken is refused",
                hasLabel(view.getRoot(), "That username is already taken."));
        check("No extra customer is saved for the refused registration",
                customerDAO.findAll().size() == customersBefore);

        view.getBackButton().fire();
        check("The Back button returns to the login screen", backClicked.get());

        AtomicReference<UserAccount> result = new AtomicReference<>();
        LoginView loginScreen = loginScreen(result);
        submitLogin(loginScreen, "keabetswe", "keapass1");
        check("The new customer can log in", result.get() != null);

        UserAccount user = result.get();
        bank.loadAccounts(user.getCustomer());
        AccountView accounts = new AccountView();
        new AccountController(accounts, bank, user, () -> { });
        check("A new customer starts with no accounts", accountRows(accounts).isEmpty());

        typeBox(accounts).setValue("Savings");
        setOpeningDeposit(accounts, "");
        accounts.getOpenAccountButton().fire();
        check("The new customer opens a Savings account with no deposit",
                hasLabelMatching(accounts.getRoot(), "Savings account [0-9]{9} opened\\."));
        List<Account> saved = accountDAO.findByCustomer(
                customerDAO.findById(user.getCustomer().getCustomerId()).orElseThrow());
        check("The account is saved with a balance of zero",
                saved.size() == 1 && isEqual(saved.get(0).getBalance(), "0.00"));

        typeBox(accounts).setValue("Cheque");
        setOpeningDeposit(accounts, "");
        accounts.getOpenAccountButton().fire();
        check("A Cheque account is refused for a customer who is not employed",
                hasLabel(accounts.getRoot(),
                        "A cheque account can only be opened for an employed customer "
                                + "who provides the employer's name and address."));
        check("The refused Cheque account is not on the screen", accountRows(accounts).size() == 1);
    }

    private static void restartTests() {
        section("4. Restart: every service and data access object is rebuilt on the same database");

        startApplication();

        AtomicReference<UserAccount> result = new AtomicReference<>();
        LoginView screen = loginScreen(result);
        submitLogin(screen, "Neoyame", "passneo");
        check("The login still works after the restart", result.get() != null);

        UserAccount user = result.get();
        bank.loadAccounts(user.getCustomer());
        AccountView view = new AccountView();
        new AccountController(view, bank, user, () -> { });
        check("The four accounts are remembered", accountRows(view).size() == 4);
        check("The Savings balance is remembered", "BWP 1250.00".equals(balanceOf(view, savingsNumber)));
        check("The Investment balance is remembered", "BWP 300.00".equals(balanceOf(view, investmentNumber)));
        check("The Cheque balance is remembered", "BWP 2000.00".equals(balanceOf(view, chequeNumber)));

        selectAccount(view, savingsNumber);
        view.getHistoryButton().fire();
        check("The Savings history shows the opening and the deposit",
                historyRows(view).size() == 2
                        && "Account Opening".equals(historyRows(view).get(0).type())
                        && "Deposit".equals(historyRows(view).get(1).type()));

        AtomicReference<UserAccount> lockResult = new AtomicReference<>();
        LoginView lockScreen = loginScreen(lockResult);
        submitLogin(lockScreen, "sebina", "sebina123");
        check("A locked login is still locked after the restart",
                hasLabel(lockScreen.getRoot(), "This account is locked. Please contact bank staff to unlock it.")
                        && lockResult.get() == null);

        auth.unlock(userDAO.findByUsername("teller").orElseThrow(), "sebina");
        submitLogin(lockScreen, "sebina", "sebina123");
        check("The login works again after bank staff unlock it", lockResult.get() != null);
    }

    private static LoginView loginScreen(AtomicReference<UserAccount> result) {
        LoginView view = new LoginView();
        new LoginController(view, auth, result::set, () -> { });
        return view;
    }

    private static void submitLogin(LoginView view, String username, String password) {
        plainFields(view.getRoot()).get(0).setText(username);
        passwordFields(view.getRoot()).get(0).setText(password);
        view.getLoginButton().fire();
    }

    private static void fillRegistration(RegistrationView view, String username, String password, String confirm) {
        List<TextField> fields = plainFields(view.getRoot());
        fields.get(0).setText("Keabetswe");
        fields.get(1).setText("Moeng");
        fields.get(2).setText("123123123");
        fields.get(7).setText("Plot 9, Gaborone");
        fields.get(8).setText("78901234");
        fields.get(9).setText(username);
        List<PasswordField> passwords = passwordFields(view.getRoot());
        passwords.get(0).setText(password);
        passwords.get(1).setText(confirm);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static TableView<AccountView.AccountRow> accountsTable(AccountView view) {
        List<TableView> tables = all(view.getRoot(), TableView.class);
        return (TableView<AccountView.AccountRow>) tables.get(0);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static TableView<AccountView.HistoryRow> historyTable(AccountView view) {
        List<TableView> tables = all(view.getRoot(), TableView.class);
        return (TableView<AccountView.HistoryRow>) tables.get(1);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static ComboBox<String> typeBox(AccountView view) {
        List<ComboBox> boxes = all(view.getRoot(), ComboBox.class);
        return (ComboBox<String>) boxes.get(0);
    }

    private static List<AccountView.AccountRow> accountRows(AccountView view) {
        return accountsTable(view).getItems();
    }

    private static List<AccountView.HistoryRow> historyRows(AccountView view) {
        return historyTable(view).getItems();
    }

    private static String balanceOf(AccountView view, String accountNumber) {
        for (AccountView.AccountRow row : accountRows(view)) {
            if (row.accountNumber().equals(accountNumber)) {
                return row.balance();
            }
        }
        return "";
    }

    private static void selectAccount(AccountView view, String accountNumber) {
        TableView<AccountView.AccountRow> table = accountsTable(view);
        for (int i = 0; i < table.getItems().size(); i++) {
            if (table.getItems().get(i).accountNumber().equals(accountNumber)) {
                table.getSelectionModel().clearSelection();
                table.getSelectionModel().select(i);
                return;
            }
        }
        throw new IllegalStateException("Account " + accountNumber + " is not on the screen.");
    }

    private static void setAmount(AccountView view, String text) {
        fieldWithPrompt(view.getRoot(), "Amount in BWP").setText(text);
    }

    private static void setOpeningDeposit(AccountView view, String text) {
        fieldWithPrompt(view.getRoot(), "Opening deposit").setText(text);
    }

    private static TextField fieldWithPrompt(Node root, String prompt) {
        for (TextField field : plainFields(root)) {
            if (prompt.equals(field.getPromptText())) {
                return field;
            }
        }
        throw new IllegalStateException("No text box with the prompt: " + prompt);
    }

    private static List<TextField> plainFields(Node root) {
        List<TextField> fields = new ArrayList<>();
        for (TextField field : all(root, TextField.class)) {
            if (!(field instanceof PasswordField)) {
                fields.add(field);
            }
        }
        return fields;
    }

    private static List<PasswordField> passwordFields(Node root) {
        return all(root, PasswordField.class);
    }

    private static boolean hasLabel(Node root, String text) {
        for (Label label : all(root, Label.class)) {
            if (text.equals(label.getText())) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasLabelMatching(Node root, String pattern) {
        for (Label label : all(root, Label.class)) {
            if (label.getText() != null && label.getText().matches(pattern)) {
                return true;
            }
        }
        return false;
    }

    private static <T extends Node> List<T> all(Node node, Class<T> type) {
        List<T> found = new ArrayList<>();
        collect(node, type, found);
        return found;
    }

    private static <T extends Node> void collect(Node node, Class<T> type, List<T> found) {
        if (type.isInstance(node)) {
            found.add(type.cast(node));
        }
        if (node instanceof ScrollPane scrollPane && scrollPane.getContent() != null) {
            collect(scrollPane.getContent(), type, found);
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                collect(child, type, found);
            }
        }
    }

    private static boolean isEqual(BigDecimal actual, String expected) {
        return actual.compareTo(new BigDecimal(expected)) == 0;
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("--- " + title + " ---");
    }

    private static void check(String name, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("PASS: " + name);
        } else {
            failed++;
            System.out.println("FAIL: " + name);
        }
    }
}