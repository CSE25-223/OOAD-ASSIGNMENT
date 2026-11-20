import controller.AccountController;
import controller.LoginController;
import controller.RegistrationController;
import dao.AccountDAO;
import dao.CustomerDAO;
import dao.Database;
import dao.UserAccountDAO;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import model.AuthenticationService;
import model.BankingService;
import model.IndividualCustomer;
import model.Role;
import model.UserAccount;
import view.AccountView;
import view.LoginView;
import view.RegistrationView;

import java.math.BigDecimal;

public class MainApp extends Application {

    private AuthenticationService authenticationService;
    private BankingService bankingService;
    private Stage stage;

    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;

        Database database = Database.file();
        CustomerDAO customerDAO = new CustomerDAO(database);
        UserAccountDAO userDAO = new UserAccountDAO(database, customerDAO);
        AccountDAO accountDAO = new AccountDAO(database, customerDAO);
        authenticationService = new AuthenticationService(userDAO, customerDAO);
        bankingService = new BankingService(accountDAO);

        if (!authenticationService.hasUsers()) {
            addSampleData();
        }

        stage.setTitle("Banking System");
        showLogin();
        stage.show();
    }

    private void showLogin() {
        LoginView view = new LoginView();
        new LoginController(view, authenticationService, this::showAccounts, this::showRegistration);
        stage.setScene(new Scene(view.getRoot(), 380, 420));
    }

    private void showRegistration() {
        RegistrationView view = new RegistrationView();
        new RegistrationController(view, authenticationService, this::showLogin);
        stage.setScene(new Scene(view.getRoot(), 440, 720));
    }

    private void showAccounts(UserAccount user) {
        bankingService.loadAccounts(user.getCustomer());
        AccountView view = new AccountView();
        new AccountController(view, bankingService, user, this::showLogin);
        stage.setScene(new Scene(view.getRoot(), 780, 680));
    }

    private void addSampleData() {
        IndividualCustomer Neoyame = new IndividualCustomer("1001", "Neoyame", "Dibotelo",
                "244357893", "Plot 142, Gaborone", "76983170", "Arilyx Ltd", "Plot 5, Gaborone");

        authenticationService.addUser(new UserAccount("9001", "Neoyame", "passneo", Role.CUSTOMER, Neoyame));
        bankingService.openAccount(Neoyame, "Savings", "Gaborone Main", new BigDecimal("1000.00"));
        bankingService.openAccount(Neoyame, "Investment", "Gaborone Main", new BigDecimal("500.00"));
        bankingService.openAccount(Neoyame, "Cheque", "Gaborone Main", new BigDecimal("2000.00"));

        authenticationService.addUser(new UserAccount("9002", "teller", "staffpass", Role.BANK_STAFF, null));
    }

    public static void main(String[] args) {
        launch(args);
    }
}