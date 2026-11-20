package view;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class RegistrationView {

    private final ScrollPane root = new ScrollPane();
    private final ComboBox<String> typeBox = new ComboBox<>();
    private final TextField firstNameField = new TextField();
    private final TextField surnameField = new TextField();
    private final TextField identificationField = new TextField();
    private final TextField employerNameField = new TextField();
    private final TextField employerAddressField = new TextField();
    private final TextField companyNameField = new TextField();
    private final TextField registrationNumberField = new TextField();
    private final TextField addressField = new TextField();
    private final TextField contactField = new TextField();
    private final TextField usernameField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final PasswordField confirmPasswordField = new PasswordField();
    private final Button registerButton = new Button("Register");
    private final Button backButton = new Button("Back to login");
    private final Label messageLabel = new Label();

    public RegistrationView() {
        Label title = new Label("Register as a new customer");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");

        typeBox.getItems().addAll("Individual", "Company");
        typeBox.setValue("Individual");
        typeBox.setMaxWidth(Double.MAX_VALUE);

        VBox individualBox = new VBox(10,
                field("First name", firstNameField),
                field("Surname", surnameField),
                field("Identification (for example Omang number)", identificationField),
                field("Employer name (optional)", employerNameField),
                field("Employer address (optional)", employerAddressField));

        VBox companyBox = new VBox(10,
                field("Company name", companyNameField),
                field("Registration number", registrationNumberField));
        companyBox.setVisible(false);
        companyBox.setManaged(false);

        typeBox.setOnAction(event -> {
            boolean individual = "Individual".equals(typeBox.getValue());
            individualBox.setVisible(individual);
            individualBox.setManaged(individual);
            companyBox.setVisible(!individual);
            companyBox.setManaged(!individual);
        });

        registerButton.setMaxWidth(Double.MAX_VALUE);
        registerButton.setDefaultButton(true);
        backButton.setMaxWidth(Double.MAX_VALUE);
        messageLabel.setWrapText(true);

        VBox content = new VBox(10,
                title,
                field("Customer type", typeBox),
                individualBox,
                companyBox,
                field("Address", addressField),
                field("Contact details", contactField),
                field("Username", usernameField),
                field("Password (at least 6 characters)", passwordField),
                field("Confirm password", confirmPasswordField),
                registerButton, backButton, messageLabel);
        content.setPadding(new Insets(25));

        root.setContent(content);
        root.setFitToWidth(true);
    }

    private static VBox field(String labelText, Control control) {
        return new VBox(4, new Label(labelText), control);
    }

    public ScrollPane getRoot() {
        return root;
    }

    public String getCustomerType() {
        return typeBox.getValue();
    }

    public String getFirstName() {
        return firstNameField.getText().trim();
    }

    public String getSurname() {
        return surnameField.getText().trim();
    }

    public String getIdentification() {
        return identificationField.getText().trim();
    }

    public String getEmployerName() {
        return employerNameField.getText().trim();
    }

    public String getEmployerAddress() {
        return employerAddressField.getText().trim();
    }

    public String getCompanyName() {
        return companyNameField.getText().trim();
    }

    public String getRegistrationNumber() {
        return registrationNumberField.getText().trim();
    }

    public String getAddress() {
        return addressField.getText().trim();
    }

    public String getContactDetails() {
        return contactField.getText().trim();
    }

    public String getUsername() {
        return usernameField.getText().trim();
    }

    public String getPassword() {
        return passwordField.getText();
    }

    public String getConfirmPassword() {
        return confirmPasswordField.getText();
    }

    public Button getRegisterButton() {
        return registerButton;
    }

    public Button getBackButton() {
        return backButton;
    }

    public void showError(String message) {
        messageLabel.setStyle("-fx-text-fill: #c0392b;");
        messageLabel.setText(message);
    }

    public void showMessage(String message) {
        messageLabel.setStyle("-fx-text-fill: #1e8449;");
        messageLabel.setText(message);
    }

    public void clearForm() {
        firstNameField.clear();
        surnameField.clear();
        identificationField.clear();
        employerNameField.clear();
        employerAddressField.clear();
        companyNameField.clear();
        registrationNumberField.clear();
        addressField.clear();
        contactField.clear();
        usernameField.clear();
        passwordField.clear();
        confirmPasswordField.clear();
    }

    public void clearPasswords() {
        passwordField.clear();
        confirmPasswordField.clear();
    }
}