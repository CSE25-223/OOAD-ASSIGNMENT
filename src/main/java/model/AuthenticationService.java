package model;

import dao.CustomerDAO;
import dao.UserAccountDAO;

import java.util.Optional;

public class AuthenticationService {

    private static final String INVALID_LOGIN = "Invalid username or password.";
    private static final long ID_FLOOR = 1000;

    private final UserAccountDAO userDAO;
    private final CustomerDAO customerDAO;

    public AuthenticationService(UserAccountDAO userDAO, CustomerDAO customerDAO) {
        if (userDAO == null || customerDAO == null) {
            throw new IllegalArgumentException("The data access objects are required.");
        }
        this.userDAO = userDAO;
        this.customerDAO = customerDAO;
    }

    public boolean hasUsers() {
        return !userDAO.findAll().isEmpty();
    }

    public String nextCustomerId() {
        long highest = ID_FLOOR;
        for (Customer customer : customerDAO.findAll()) {
            highest = Math.max(highest, numeric(customer.getCustomerId()));
        }
        return String.valueOf(highest + 1);
    }

    public UserAccount register(Customer customer, String username, String password) {
        UserAccount user = new UserAccount(nextUserId(), username, password, Role.CUSTOMER, customer);
        addUser(user);
        return user;
    }

    public void addUser(UserAccount user) {
        if (user == null) {
            throw new IllegalArgumentException("A user is required.");
        }
        if (userDAO.findByUsername(user.getUsername()).isPresent()) {
            throw new IllegalArgumentException("That username is already taken.");
        }

        Customer customer = user.getCustomer();
        boolean customerCreated = false;
        if (customer != null && customerDAO.findById(customer.getCustomerId()).isEmpty()) {
            customerDAO.create(customer);
            customerCreated = true;
        }

        try {
            userDAO.create(user);
        } catch (RuntimeException e) {
            if (customerCreated) {
                customerDAO.delete(customer.getCustomerId());
            }
            throw e;
        }
    }

    public UserAccount authenticate(String username, String password) {
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Enter your username and password.");
        }

        Optional<UserAccount> found = userDAO.findByUsername(username);
        if (found.isEmpty()) {
            throw new IllegalArgumentException(INVALID_LOGIN);
        }

        UserAccount user = found.get();
        if (user.isLocked()) {
            throw new IllegalStateException(
                    "This account is locked. Please contact bank staff to unlock it.");
        }

        if (!user.checkPassword(password)) {
            user.registerFailure();
            userDAO.update(user);
            if (user.isLocked()) {
                throw new IllegalStateException(
                        "Too many failed attempts. This account is now locked. "
                                + "Please contact bank staff to unlock it.");
            }
            throw new IllegalArgumentException(INVALID_LOGIN);
        }

        user.resetFailures();
        userDAO.update(user);
        return user;
    }

    public void unlock(UserAccount staff, String username) {
        if (staff == null || staff.getRole() != Role.BANK_STAFF) {
            throw new IllegalStateException("Only bank staff can unlock a user account.");
        }
        UserAccount user = userDAO.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
        user.unlock();
        userDAO.update(user);
    }

    private String nextUserId() {
        long highest = ID_FLOOR;
        for (UserAccount user : userDAO.findAll()) {
            highest = Math.max(highest, numeric(user.getUserId()));
        }
        return String.valueOf(highest + 1);
    }

    private static long numeric(String id) {
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}