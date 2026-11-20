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
import model.Account;
import model.AuthenticationService;
import model.BankingService;
import model.CompanyCustomer;
import model.Customer;
import model.IndividualCustomer;
import model.Role;
import model.UserAccount;
import view.AccountView;
import view.LoginView;
import view.RegistrationView;

import java.math.BigDecimal;
import java.time.YearMonth;

public class MainApp extends Application {

    private static final String BRANCH = "Gaborone Main";

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

    private void addLogin(String userId, String username, String password, Customer customer) {
        authenticationService.addUser(new UserAccount(userId, username, password, Role.CUSTOMER, customer));
    }

    private Account open(Customer owner, String type, String openingDeposit) {
        return bankingService.openAccount(owner, type, BRANCH, new BigDecimal(openingDeposit));
    }

    private void addSampleData() {
        IndividualCustomer Neoyame = new IndividualCustomer("1001", "Neoyame", "Dibotelo",
                "244357893", "Plot 142, Gaborone", "76983170", "Arilyx Ltd", "Plot 5, Gaborone");
        addLogin("9001", "Neoyame", "passneo", Neoyame);
        open(Neoyame, "Savings", "1000.00");
        open(Neoyame, "Investment", "500.00");
        open(Neoyame, "Cheque", "2000.00");

        IndividualCustomer thato = new IndividualCustomer("1002", "Thato", "Kgosi",
                "312456789", "Plot 12, Gaborone", "71234567", "Kgale Hardware", "Plot 30, Gaborone");
        addLogin("9003", "thato", "thato123", thato);
        Account thatoSavings = open(thato, "Savings", "1500.00");
        Account thatoCheque = open(thato, "Cheque", "3000.00");
        bankingService.deposit(thatoSavings, new BigDecimal("250.00"));
        bankingService.withdraw(thatoCheque, new BigDecimal("450.00"));

        IndividualCustomer sebina = new IndividualCustomer("1003", "Neo", "Sebina",
                "421987654", "Plot 8, Francistown", "72345678");
        addLogin("9004", "sebina", "sebina123", sebina);
        open(sebina, "Savings", "300.00");

        IndividualCustomer lesego = new IndividualCustomer("1004", "Lesego", "Molefe",
                "533210987", "Plot 77, Mogoditshane", "73456789", "Okavango Logistics", "Plot 4, Gaborone");
        addLogin("9005", "lesego", "lesego123", lesego);
        open(lesego, "Savings", "800.00");
        Account lesegoInvestment = open(lesego, "Investment", "1200.00");
        Account lesegoCheque = open(lesego, "Cheque", "2500.00");
        bankingService.deposit(lesegoCheque, new BigDecimal("500.00"));
        bankingService.withdraw(lesegoInvestment, new BigDecimal("200.00"));

        IndividualCustomer kabelo = new IndividualCustomer("1005", "Kabelo", "Mothibi",
                "644321098", "Plot 21, Maun", "74567890");
        addLogin("9006", "kabelo", "kabelo123", kabelo);
        open(kabelo, "Investment", "1500.00");
        open(kabelo, "Savings", "200.00");

        IndividualCustomer boitumelo = new IndividualCustomer("1006", "Boitumelo", "Seretse",
                "755432109", "Plot 3, Palapye", "75678901", "Serowe Mining Services", "Plot 9, Palapye");
        addLogin("9007", "boitumelo", "boitumelo123", boitumelo);
        Account boitumeloCheque = open(boitumelo, "Cheque", "5000.00");
        open(boitumelo, "Savings", "750.00");
        bankingService.withdraw(boitumeloCheque, new BigDecimal("1200.00"));

        CompanyCustomer delta = new CompanyCustomer("1007", "Delta Traders",
                "BW00012345", "Main Mall, Gaborone", "3901234");
        addLogin("9008", "delta", "delta123", delta);
        open(delta, "Savings", "5000.00");
        Account deltaInvestment = open(delta, "Investment", "2500.00");
        bankingService.deposit(deltaInvestment, new BigDecimal("500.00"));

        CompanyCustomer kalahari = new CompanyCustomer("1008", "Kalahari Foods",
                "BW00067890", "Industrial Site, Lobatse", "5301122");
        addLogin("9009", "kalahari", "kalahari123", kalahari);
        Account kalahariSavings = open(kalahari, "Savings", "8000.00");
        bankingService.deposit(kalahariSavings, new BigDecimal("1000.00"));

        IndividualCustomer tebogo = new IndividualCustomer("1009", "Tebogo", "Ramotswe",
                "866543210", "Plot 15, Kanye", "76789012");
        addLogin("9010", "tebogo", "tebogo123", tebogo);
        open(tebogo, "Savings", "450.00");

        IndividualCustomer masego = new IndividualCustomer("1010", "Masego", "Phiri",
                "977654321", "Plot 52, Tlokweng", "77890123", "Limpopo Bank Services", "Plot 2, Gaborone");
        addLogin("9011", "masego", "masego123", masego);
        open(masego, "Investment", "800.00");
        open(masego, "Cheque", "1200.00");
        open(masego, "Savings", "600.00");

        authenticationService.addUser(new UserAccount("9002", "teller", "staffpass", Role.BANK_STAFF, null));

        bankingService.processMonthlyInterest(YearMonth.of(2026, 10));
    }

    public static void main(String[] args) {
        launch(args);
    }
}