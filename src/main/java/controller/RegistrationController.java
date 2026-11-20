package controller;

import model.AuthenticationService;
import model.CompanyCustomer;
import model.Customer;
import model.IndividualCustomer;
import view.RegistrationView;

public class RegistrationController {

    private final RegistrationView view;
    private final AuthenticationService authenticationService;

    public RegistrationController(RegistrationView view, AuthenticationService authenticationService,
                                  Runnable onBack) {
        this.view = view;
        this.authenticationService = authenticationService;

        view.getRegisterButton().setOnAction(event -> handleRegister());
        view.getBackButton().setOnAction(event -> onBack.run());
    }

    private void handleRegister() {
        try {
            String password = view.getPassword();
            if (!password.equals(view.getConfirmPassword())) {
                throw new IllegalArgumentException("The two passwords do not match.");
            }
            Customer customer = buildCustomer();
            authenticationService.register(customer, view.getUsername(), password);
            view.clearForm();
            view.showMessage("Registration successful. You can now go back and log in.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            view.clearPasswords();
            view.showError(e.getMessage());
        }
    }

    private Customer buildCustomer() {
        String customerId = authenticationService.nextCustomerId();

        if ("Company".equals(view.getCustomerType())) {
            return new CompanyCustomer(customerId, view.getCompanyName(), view.getRegistrationNumber(),
                    view.getAddress(), view.getContactDetails());
        }

        boolean hasEmployer = !view.getEmployerName().isEmpty() || !view.getEmployerAddress().isEmpty();
        if (hasEmployer) {
            return new IndividualCustomer(customerId, view.getFirstName(), view.getSurname(),
                    view.getIdentification(), view.getAddress(), view.getContactDetails(),
                    view.getEmployerName(), view.getEmployerAddress());
        }
        return new IndividualCustomer(customerId, view.getFirstName(), view.getSurname(),
                view.getIdentification(), view.getAddress(), view.getContactDetails());
    }
}