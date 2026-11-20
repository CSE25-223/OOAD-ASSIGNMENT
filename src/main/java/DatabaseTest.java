import dao.Database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws SQLException {
        Database database = Database.inMemory("schematest");

        try (Connection connection = database.getConnection(); Statement statement = connection.createStatement()) {
            for (String table : new String[]{"CUSTOMERS", "USER_ACCOUNTS", "ACCOUNTS",
                    "TRANSACTIONS", "INTEREST_PAYMENTS"}) {
                check("Table " + table + " exists",
                        connection.getMetaData().getTables(null, null, table, null).next());
            }

            statement.execute("INSERT INTO customers (customer_id, customer_type, address, contact_details, "
                    + "first_name, surname, identification, employed) "
                    + "VALUES ('1001', 'INDIVIDUAL', 'Plot 12, Gaborone', '71234567', 'Thato', 'Kgosi', '123456789', FALSE)");
            ResultSet customer = statement.executeQuery("SELECT first_name FROM customers WHERE customer_id = '1001'");
            check("A customer can be saved and read back", customer.next() && "Thato".equals(customer.getString(1)));

            statement.execute("INSERT INTO accounts (account_number, account_type, balance, branch, date_opened, customer_id) "
                    + "VALUES ('453678203', 'SAVINGS', 1000.00, 'Gaborone Main', CURRENT_DATE, '1001')");
            ResultSet account = statement.executeQuery("SELECT balance FROM accounts WHERE account_number = '453678203'");
            check("An account can be saved and read back", account.next() && account.getBigDecimal(1).signum() > 0);

            expectRejected(statement, "Account without a customer is rejected",
                    "INSERT INTO accounts (account_number, account_type, balance, branch, date_opened, customer_id) "
                            + "VALUES ('111111111', 'SAVINGS', 0.00, 'Gaborone Main', CURRENT_DATE, '9999')");
            expectRejected(statement, "Unknown account type is rejected",
                    "INSERT INTO accounts (account_number, account_type, balance, branch, date_opened, customer_id) "
                            + "VALUES ('222222222', 'GOLD', 0.00, 'Gaborone Main', CURRENT_DATE, '1001')");
            expectRejected(statement, "Transaction without an account is rejected",
                    "INSERT INTO transactions (transaction_id, account_number, transaction_type, amount, date_time, resulting_balance) "
                            + "VALUES ('100001', '000000000', 'DEPOSIT', 10.00, CURRENT_TIMESTAMP, 10.00)");

            statement.execute("INSERT INTO user_accounts (user_id, username, password_hash, user_role, customer_id) "
                    + "VALUES ('9001', 'thato', 'hash', 'CUSTOMER', '1001')");
            expectRejected(statement, "Duplicate username is rejected",
                    "INSERT INTO user_accounts (user_id, username, password_hash, user_role, customer_id) "
                            + "VALUES ('9002', 'thato', 'hash', 'CUSTOMER', '1001')");
        }

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

    private static void expectRejected(Statement statement, String name, String sql) {
        try {
            statement.execute(sql);
            failed++;
            System.out.println("FAIL: " + name + " (no error was thrown)");
        } catch (SQLException e) {
            passed++;
            System.out.println("PASS: " + name);
        }
    }
}
