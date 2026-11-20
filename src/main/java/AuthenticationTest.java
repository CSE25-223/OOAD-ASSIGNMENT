import dao.CustomerDAO;
import dao.Database;
import dao.UserAccountDAO;
import model.AuthenticationService;
import model.IndividualCustomer;
import model.Role;
import model.UserAccount;

public class AuthenticationTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        Database database = Database.inMemory("authtest");
        CustomerDAO customerDAO = new CustomerDAO(database);
        UserAccountDAO userDAO = new UserAccountDAO(database, customerDAO);
        AuthenticationService auth = new AuthenticationService(userDAO, customerDAO);

        check("An empty database has no users", !auth.hasUsers());

        IndividualCustomer customer = new IndividualCustomer("1001", "Thato", "Kgosi",
                "123456789", "Plot 12, Gaborone", "71234567");
        auth.addUser(new UserAccount("9001", "thato", "secret123", Role.CUSTOMER, customer));
        UserAccount teller = new UserAccount("9002", "teller", "staffpass", Role.BANK_STAFF, null);
        auth.addUser(teller);

        check("Users exist after adding", auth.hasUsers());
        check("The customer was saved with the login", customerDAO.findById("1001").isPresent());
        check("Correct login returns the user", "9001".equals(auth.authenticate("thato", "secret123").getUserId()));
        check("Username is not case-sensitive", "9001".equals(auth.authenticate("THATO", "secret123").getUserId()));
        expectRejected("Wrong password is rejected", () -> auth.authenticate("thato", "wrong"));
        check("Wrong password counts as one failure", stored(userDAO, "thato").getFailedAttempts() == 1);
        auth.authenticate("thato", "secret123");
        check("Successful login resets the failures", stored(userDAO, "thato").getFailedAttempts() == 0);

        expectRejected("Unknown username is rejected", () -> auth.authenticate("nobody", "secret123"));
        expectRejected("Blank username is rejected", () -> auth.authenticate("  ", "secret123"));
        expectRejected("Blank password is rejected", () -> auth.authenticate("thato", ""));
        expectRejected("Duplicate username is rejected when adding a user",
                () -> auth.addUser(new UserAccount("9003", "THATO", "secret123", Role.BANK_STAFF, null)));

        String neoId = auth.nextCustomerId();
        IndividualCustomer neo = new IndividualCustomer(neoId, "Neo", "Sebina",
                "987654321", "Plot 8, Francistown", "72345678");
        UserAccount registered = auth.register(neo, "neo", "neopass1");
        check("Registered user can log in",
                registered.getUserId().equals(auth.authenticate("neo", "neopass1").getUserId()));
        check("Registered user is a customer", registered.getRole() == Role.CUSTOMER);
        check("Registered customer was saved", customerDAO.findById(neoId).isPresent());
        check("The next customer ID is different", !neoId.equals(auth.nextCustomerId()));

        String extraId = auth.nextCustomerId();
        IndividualCustomer extra = new IndividualCustomer(extraId, "Extra", "Person",
                "111111111", "Plot 1, Gaborone", "70000000");
        expectRejected("Duplicate username is rejected", () -> auth.register(extra, "NEO", "otherpass1"));
        check("Rejected registration did not save the customer", customerDAO.findById(extraId).isEmpty());
        expectRejected("Short password is rejected", () -> auth.register(extra, "extra", "123"));
        check("Rejected password did not save the customer", customerDAO.findById(extraId).isEmpty());

        expectRejected("Failure 1", () -> auth.authenticate("thato", "bad"));
        expectRejected("Failure 2", () -> auth.authenticate("thato", "bad"));
        expectRejected("Failure 3 locks the account", () -> auth.authenticate("thato", "bad"));
        check("Account is locked after three failures", stored(userDAO, "thato").isLocked());
        expectRejected("Correct password is refused while locked",
                () -> auth.authenticate("thato", "secret123"));

        CustomerDAO customerDAO2 = new CustomerDAO(database);
        AuthenticationService restarted = new AuthenticationService(
                new UserAccountDAO(database, customerDAO2), customerDAO2);
        expectRejected("The lock is remembered by a new service",
                () -> restarted.authenticate("thato", "secret123"));

        expectRejected("Customer cannot unlock an account", () -> auth.unlock(stored(userDAO, "thato"), "thato"));
        auth.unlock(teller, "thato");
        check("Staff can unlock the account", !stored(userDAO, "thato").isLocked());
        check("Login works again after unlock", "9001".equals(auth.authenticate("thato", "secret123").getUserId()));

        System.out.println();
        System.out.println("Passed: " + passed + "   Failed: " + failed);
    }

    private static UserAccount stored(UserAccountDAO dao, String username) {
        return dao.findByUsername(username).orElseThrow();
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

    private static void expectRejected(String name, Runnable action) {
        try {
            action.run();
            failed++;
            System.out.println("FAIL: " + name + " (no error was thrown)");
        } catch (RuntimeException e) {
            passed++;
            System.out.println("PASS: " + name + "  ->  " + e.getMessage());
        }
    }
}