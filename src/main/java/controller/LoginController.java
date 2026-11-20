package controller;

import model.AuthenticationService;
import model.Role;
import model.UserAccount;
import view.LoginView;

import java.util.function.Consumer;

public class LoginController {

    private final LoginView view;
    private final AuthenticationService authenticationService;
    private final Consumer<UserAccount> onLoginSuccess;

    public LoginController(LoginView view, AuthenticationService authenticationService,
                           Consumer<UserAccount> onLoginSuccess, Runnable onRegister) {
        this.view = view;
        this.authenticationService = authenticationService;
        this.onLoginSuccess = onLoginSuccess;

        view.getLoginButton().setOnAction(event -> handleLogin());
        view.getRegisterButton().setOnAction(event -> onRegister.run());
    }

    private void handleLogin() {
        try {
            UserAccount user = authenticationService.authenticate(view.getUsername(), view.getPassword());
            view.clearPassword();
            if (user.getRole() != Role.CUSTOMER) {
                view.showError("Bank staff screens are not available yet.");
                return;
            }
            onLoginSuccess.accept(user);
        } catch (IllegalArgumentException | IllegalStateException e) {
            view.clearPassword();
            view.showError(e.getMessage());
        }
    }
}