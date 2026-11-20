package view;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class LoginView {

    private final VBox root = new VBox(10);
    private final TextField usernameField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final Button loginButton = new Button("Log in");
    private final Button registerButton = new Button("Register as a new customer");
    private final Label messageLabel = new Label();

    public LoginView() {
        Label title = new Label("Banking System");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        Label subtitle = new Label("Please log in to continue");

        usernameField.setPromptText("Enter your username");
        passwordField.setPromptText("Enter your password");

        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setDefaultButton(true);
        registerButton.setMaxWidth(Double.MAX_VALUE);

        messageLabel.setWrapText(true);

        root.setPadding(new Insets(30));
        root.getChildren().addAll(
                title, subtitle,
                new Label("Username"), usernameField,
                new Label("Password"), passwordField,
                loginButton, registerButton, messageLabel);
    }

    public VBox getRoot() {
        return root;
    }

    public String getUsername() {
        return usernameField.getText().trim();
    }

    public String getPassword() {
        return passwordField.getText();
    }

    public Button getLoginButton() {
        return loginButton;
    }

    public Button getRegisterButton() {
        return registerButton;
    }

    public void showError(String message) {
        messageLabel.setStyle("-fx-text-fill: #c0392b;");
        messageLabel.setText(message);
    }

    public void showMessage(String message) {
        messageLabel.setStyle("-fx-text-fill: #1e8449;");
        messageLabel.setText(message);
    }

    public void clearPassword() {
        passwordField.clear();
    }
}