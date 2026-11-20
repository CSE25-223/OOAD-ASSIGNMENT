package view;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Function;

public class AccountView {

    public record AccountRow(String accountType, String accountNumber, String balance) { }

    public record HistoryRow(String transactionId, String dateTime, String type,
                             String amount, String balance) { }

    private final BorderPane root = new BorderPane();
    private final Label welcomeLabel = new Label("Welcome");
    private final Button logoutButton = new Button("Log out");

    private final TableView<AccountRow> accountsTable = new TableView<>();
    private final ComboBox<String> accountTypeBox = new ComboBox<>();
    private final TextField openingDepositField = new TextField();
    private final Button openAccountButton = new Button("Open account");
    private final TextField amountField = new TextField();
    private final Button depositButton = new Button("Deposit");
    private final Button withdrawButton = new Button("Withdraw");
    private final Button historyButton = new Button("View history");
    private final Label messageLabel = new Label();
    private final TableView<HistoryRow> historyTable = new TableView<>();

    public AccountView() {
        welcomeLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox topBar = new HBox(10, welcomeLabel, spacer, logoutButton);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(15, 20, 10, 20));

        accountsTable.getColumns().add(column("Account type", 160, AccountRow::accountType));
        accountsTable.getColumns().add(column("Account number", 180, AccountRow::accountNumber));
        accountsTable.getColumns().add(column("Balance", 160, AccountRow::balance));
        accountsTable.setPlaceholder(new Label("No accounts to show. Open one below."));
        accountsTable.setPrefHeight(150);

        accountTypeBox.getItems().addAll("Savings", "Investment", "Cheque");
        accountTypeBox.setValue("Savings");
        openingDepositField.setPromptText("Opening deposit");
        openingDepositField.setPrefWidth(200);
        HBox openAccountRow = new HBox(10, new Label("Open a new account:"), accountTypeBox,
                openingDepositField, openAccountButton);
        openAccountRow.setAlignment(Pos.CENTER_LEFT);

        amountField.setPromptText("Amount in BWP");
        amountField.setPrefWidth(260);
        HBox actions = new HBox(10, new Label("Amount:"), amountField,
                depositButton, withdrawButton, historyButton);
        actions.setAlignment(Pos.CENTER_LEFT);

        messageLabel.setWrapText(true);

        historyTable.getColumns().add(column("Transaction ID", 120, HistoryRow::transactionId));
        historyTable.getColumns().add(column("Date and time", 200, HistoryRow::dateTime));
        historyTable.getColumns().add(column("Type", 130, HistoryRow::type));
        historyTable.getColumns().add(column("Amount", 110, HistoryRow::amount));
        historyTable.getColumns().add(column("Balance", 110, HistoryRow::balance));
        historyTable.setPlaceholder(new Label("Select an account and click View history."));
        VBox.setVgrow(historyTable, Priority.ALWAYS);

        VBox center = new VBox(10,
                new Label("Your accounts"), accountsTable,
                openAccountRow, actions, messageLabel,
                new Label("Transaction history"), historyTable);
        center.setPadding(new Insets(0, 20, 20, 20));

        root.setTop(topBar);
        root.setCenter(center);
    }

    private static <T> TableColumn<T, String> column(String title, double width,
                                                     Function<T, String> textOf) {
        TableColumn<T, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new SimpleStringProperty(textOf.apply(cell.getValue())));
        column.setPrefWidth(width);
        return column;
    }

    public BorderPane getRoot() {
        return root;
    }

    public void setCustomerName(String name) {
        welcomeLabel.setText("Welcome, " + name);
    }

    public void setAccounts(List<AccountRow> rows) {
        accountsTable.setItems(FXCollections.observableArrayList(rows));
    }

    public void setHistory(List<HistoryRow> rows) {
        historyTable.setItems(FXCollections.observableArrayList(rows));
    }

    public void showError(String message) {
        messageLabel.setStyle("-fx-text-fill: #c0392b;");
        messageLabel.setText(message);
    }

    public void showMessage(String message) {
        messageLabel.setStyle("-fx-text-fill: #1e8449;");
        messageLabel.setText(message);
    }

    public void clearAmount() {
        amountField.clear();
    }

    public void clearOpeningDeposit() {
        openingDepositField.clear();
    }

    public String getSelectedAccountNumber() {
        AccountRow selected = accountsTable.getSelectionModel().getSelectedItem();
        return (selected == null) ? null : selected.accountNumber();
    }

    public String getAmountText() {
        return amountField.getText().trim();
    }

    public String getSelectedAccountType() {
        return accountTypeBox.getValue();
    }

    public String getOpeningDepositText() {
        return openingDepositField.getText().trim();
    }

    public Button getDepositButton() {
        return depositButton;
    }

    public Button getWithdrawButton() {
        return withdrawButton;
    }

    public Button getHistoryButton() {
        return historyButton;
    }

    public Button getOpenAccountButton() {
        return openAccountButton;
    }

    public Button getLogoutButton() {
        return logoutButton;
    }
}