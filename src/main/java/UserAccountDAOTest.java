import dao.CustomerDAO;
import dao.Database;
import dao.UserAccountDAO;
import model.IndividualCustomer;
import model.Role;
import model.UserAccount;

public class UserAccountDAOTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        Database database = Database.inMemory("usertest");
        CustomerDAO customerDAO = new CustomerDAO(database);
        UserAccountDAO dao = new UserAccountDAO(database, customerDAO);

        IndividualCustomer customer = new IndividualCustomer("1001", "Thato", "Kgosi",
                "123456789", "Plot 12, Gaborone", "71234567");
        customerDAO.create(customer);

        UserAccount thato = new UserAccount("9001", "thato", "secret123", Role.CUSTOMER, customer);
        UserAccount teller = new UserAccount("9002", "teller", "staffpass", Role.BANK_STAFF, null);
        dao.create(thato);
        dao.create(teller);
        check("Two users were saved", dao.findAll().size() == 2);

        UserAccount loaded = dao.findByUsername("thato").orElse(null);
        check("User is found by username", loaded != null);
        if (loaded != null) {
            check("Password still works after loading", loaded.checkPassword("secret123"));
            check("Wrong password is still rejected", !loaded.checkPassword("wrong"));
            check("Role is read back", loaded.getRole() == Role.CUSTOMER);
            check("Customer link is read back",
                    loaded.getCustomer() != null && "1001".equals(loaded.getCustomer().getCustomerId()));
            check("New user is not locked", !loaded.isLocked() && loaded.getFailedAttempts() == 0);
        }

        check("Username search is not case-sensitive", dao.findByUsername("THATO").isPresent());
        check("User is found by ID", dao.findById("9002").isPresent());
        check("Unknown username is not found", dao.findByUsername("nobody").isEmpty());
        check("Staff login has no customer",
                dao.findById("9002").map(u -> u.getCustomer() == null).orElse(false));

        UserAccount toLock = dao.findByUsername("thato").orElseThrow();
        toLock.registerFailure();
        toLock.registerFailure();
        toLock.registerFailure();
        check("Update reports success", dao.update(toLock));
        UserAccount locked = dao.findByUsername("thato").orElseThrow();
        check("Lock and failed attempts are saved", locked.isLocked() && locked.getFailedAttempts() == 3);

        locked.unlock();
        dao.update(locked);
        UserAccount unlocked = dao.findByUsername("thato").orElseThrow();
        check("Unlock is saved", !unlocked.isLocked() && unlocked.getFailedAttempts() == 0);

        expectRejected("Duplicate user ID is rejected", () -> dao.create(thato));
        IndividualCustomer unsaved = new IndividualCustomer("5555", "Neo", "Sebina",
                "987654321", "Plot 8, Francistown", "72345678");
        expectRejected("Login for a customer that is not saved is rejected",
                () -> dao.create(new UserAccount("9003", "neo", "neopass1", Role.CUSTOMER, unsaved)));
        check("Updating an unknown user changes nothing",
                !dao.update(new UserAccount("7777", "ghost", "ghostpass", Role.BANK_STAFF, null)));

        check("Delete reports success", dao.delete("9002"));
        check("Deleted user is gone", dao.findById("9002").isEmpty());
        check("Deleting an unknown user reports failure", !dao.delete("9002"));
        check("The customer is not deleted with the login", customerDAO.findById("1001").isPresent());

        System.out.println();
        System.out.println("Passed: " + passed + "   Failed: " + failed);
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