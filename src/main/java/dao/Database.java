package dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {

    private static final String FILE_URL = "jdbc:h2:./data/bank";

    private static final String[] SCHEMA = {
            """
            CREATE TABLE IF NOT EXISTS customers (
                customer_id VARCHAR(20) PRIMARY KEY,
                customer_type VARCHAR(10) NOT NULL CHECK (customer_type IN ('INDIVIDUAL', 'COMPANY')),
                address VARCHAR(200) NOT NULL,
                contact_details VARCHAR(100) NOT NULL,
                first_name VARCHAR(100),
                surname VARCHAR(100),
                identification VARCHAR(50),
                employed BOOLEAN DEFAULT FALSE,
                employer_name VARCHAR(100),
                employer_address VARCHAR(200),
                company_name VARCHAR(150),
                registration_number VARCHAR(50)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS user_accounts (
                user_id VARCHAR(20) PRIMARY KEY,
                username VARCHAR(50) NOT NULL UNIQUE,
                password_hash VARCHAR(200) NOT NULL,
                user_role VARCHAR(20) NOT NULL CHECK (user_role IN ('CUSTOMER', 'BANK_STAFF')),
                failed_attempts INT NOT NULL DEFAULT 0,
                locked BOOLEAN NOT NULL DEFAULT FALSE,
                customer_id VARCHAR(20),
                FOREIGN KEY (customer_id) REFERENCES customers (customer_id)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS accounts (
                account_number VARCHAR(20) PRIMARY KEY,
                account_type VARCHAR(20) NOT NULL CHECK (account_type IN ('SAVINGS', 'INVESTMENT', 'CHEQUE')),
                balance DECIMAL(19, 2) NOT NULL,
                branch VARCHAR(100) NOT NULL,
                date_opened DATE NOT NULL,
                customer_id VARCHAR(20) NOT NULL,
                FOREIGN KEY (customer_id) REFERENCES customers (customer_id)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS transactions (
                transaction_id VARCHAR(20) PRIMARY KEY,
                account_number VARCHAR(20) NOT NULL,
                transaction_type VARCHAR(20) NOT NULL
                    CHECK (transaction_type IN ('ACCOUNT_OPENING', 'DEPOSIT', 'WITHDRAWAL', 'INTEREST_CREDIT')),
                amount DECIMAL(19, 2),
                date_time TIMESTAMP NOT NULL,
                resulting_balance DECIMAL(19, 2) NOT NULL,
                reference VARCHAR(200),
                interest_period VARCHAR(7),
                FOREIGN KEY (account_number) REFERENCES accounts (account_number)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS interest_payments (
                payment_id VARCHAR(20) PRIMARY KEY,
                account_number VARCHAR(20) NOT NULL,
                period VARCHAR(7) NOT NULL,
                status VARCHAR(20) NOT NULL
                    CHECK (status IN ('PENDING', 'VALIDATING', 'CALCULATING', 'POSTING',
                                      'COMPLETED', 'FAILED', 'SKIPPED')),
                calculated_amount DECIMAL(19, 2),
                transaction_id VARCHAR(20),
                FOREIGN KEY (account_number) REFERENCES accounts (account_number),
                FOREIGN KEY (transaction_id) REFERENCES transactions (transaction_id)
            )
            """
    };

    private final String url;

    private Database(String url) {
        this.url = url;
        createTables();
    }

    public static Database file() {
        return new Database(FILE_URL);
    }

    public static Database inMemory(String name) {
        return new Database("jdbc:h2:mem:" + name + ";DB_CLOSE_DELAY=-1");
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, "sa", "");
    }

    private void createTables() {
        try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
            for (String sql : SCHEMA) {
                statement.execute(sql);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not set up the database.", e);
        }
    }
}