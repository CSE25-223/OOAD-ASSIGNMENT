package dao;

import model.Account;
import model.ChequeAccount;
import model.Customer;
import model.InvestmentAccount;
import model.SavingsAccount;
import model.Transaction;
import model.TransactionType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class AccountDAO {

    private static final String INSERT_ACCOUNT = "INSERT INTO accounts (account_number, account_type, balance, "
            + "branch, date_opened, customer_id) VALUES (?, ?, ?, ?, ?, ?)";

    private static final String INSERT_TRANSACTION = "INSERT INTO transactions (transaction_id, account_number, "
            + "transaction_type, amount, date_time, resulting_balance, reference, interest_period) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String BY_CUSTOMER = "SELECT * FROM accounts WHERE customer_id = ? "
            + "ORDER BY date_opened, (SELECT MIN(t.date_time) FROM transactions t "
            + "WHERE t.account_number = accounts.account_number)";

    @FunctionalInterface
    private interface Work<T> {
        T run(Connection connection) throws SQLException;
    }

    private final Database database;
    private final CustomerDAO customerDAO;

    public AccountDAO(Database database, CustomerDAO customerDAO) {
        this.database = database;
        this.customerDAO = customerDAO;
    }

    public void create(Account account) {
        inTransaction("Could not save the account.", connection -> {
            try (PreparedStatement statement = connection.prepareStatement(INSERT_ACCOUNT)) {
                statement.setString(1, account.getAccountNumber());
                statement.setString(2, account.getAccountType().toUpperCase());
                statement.setBigDecimal(3, account.getBalance());
                statement.setString(4, account.getBranch());
                statement.setObject(5, account.getDateOpened());
                statement.setString(6, account.getOwner().getCustomerId());
                statement.executeUpdate();
            }
            insertNewTransactions(connection, account);
            return null;
        });
    }

    public boolean save(Account account) {
        return inTransaction("Could not save the account.", connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE accounts SET balance = ? WHERE account_number = ?")) {
                statement.setBigDecimal(1, account.getBalance());
                statement.setString(2, account.getAccountNumber());
                if (statement.executeUpdate() == 0) {
                    return false;
                }
            }
            insertNewTransactions(connection, account);
            return true;
        });
    }

    public Optional<Account> findByNumber(String accountNumber) {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT * FROM accounts WHERE account_number = ?")) {
            statement.setString(1, accountNumber);
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) {
                    return Optional.empty();
                }
                Customer owner = loadOwner(rows.getString("customer_id"));
                return Optional.of(map(connection, rows, owner));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not read the account.", e);
        }
    }

    public List<Account> findByCustomer(Customer customer) {
        List<Account> accounts = new ArrayList<>();
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(BY_CUSTOMER)) {
            statement.setString(1, customer.getCustomerId());
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    accounts.add(map(connection, rows, customer));
                }
            }
            return accounts;
        } catch (SQLException e) {
            throw new IllegalStateException("Could not read the accounts.", e);
        }
    }

    public List<Account> findAll() {
        List<Account> accounts = new ArrayList<>();
        Map<String, Customer> owners = new HashMap<>();
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT * FROM accounts ORDER BY customer_id, account_number");
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                String customerId = rows.getString("customer_id");
                Customer owner = owners.get(customerId);
                if (owner == null) {
                    owner = loadOwner(customerId);
                    owners.put(customerId, owner);
                }
                accounts.add(map(connection, rows, owner));
            }
            return accounts;
        } catch (SQLException e) {
            throw new IllegalStateException("Could not read the accounts.", e);
        }
    }

    public boolean delete(String accountNumber) {
        return inTransaction("Could not delete the account.", connection -> {
            execute(connection, "DELETE FROM interest_payments WHERE account_number = ?", accountNumber);
            execute(connection, "DELETE FROM transactions WHERE account_number = ?", accountNumber);
            return execute(connection, "DELETE FROM accounts WHERE account_number = ?", accountNumber) > 0;
        });
    }

    private <T> T inTransaction(String failureMessage, Work<T> work) {
        try (Connection connection = database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                T result = work.run(connection);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException(failureMessage, e);
        }
    }

    private int execute(Connection connection, String sql, String value) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);
            return statement.executeUpdate();
        }
    }

    private void insertNewTransactions(Connection connection, Account account) throws SQLException {
        Set<String> existing = new HashSet<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT transaction_id FROM transactions WHERE account_number = ?")) {
            statement.setString(1, account.getAccountNumber());
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    existing.add(rows.getString(1));
                }
            }
        }

        try (PreparedStatement statement = connection.prepareStatement(INSERT_TRANSACTION)) {
            for (Transaction transaction : account.getTransactionHistory()) {
                if (existing.contains(transaction.getTransactionId())) {
                    continue;
                }
                statement.setString(1, transaction.getTransactionId());
                statement.setString(2, account.getAccountNumber());
                statement.setString(3, transaction.getType().name());
                if (transaction.getAmount() == null) {
                    statement.setNull(4, Types.DECIMAL);
                } else {
                    statement.setBigDecimal(4, transaction.getAmount());
                }
                statement.setObject(5, transaction.getDateTime());
                statement.setBigDecimal(6, transaction.getResultingBalance());
                statement.setString(7, transaction.getReference());
                statement.setString(8, transaction.getInterestPeriod() == null
                        ? null : transaction.getInterestPeriod().toString());
                statement.executeUpdate();
            }
        }
    }

    private Customer loadOwner(String customerId) {
        return customerDAO.findById(customerId).orElseThrow(
                () -> new IllegalStateException("The owner of this account is missing."));
    }

    private Account map(Connection connection, ResultSet rows, Customer owner) throws SQLException {
        String accountNumber = rows.getString("account_number");
        String branch = rows.getString("branch");
        LocalDate dateOpened = rows.getObject("date_opened", LocalDate.class);
        java.math.BigDecimal balance = rows.getBigDecimal("balance");
        List<Transaction> history = loadHistory(connection, accountNumber);

        return switch (rows.getString("account_type")) {
            case "SAVINGS" -> SavingsAccount.restore(accountNumber, owner, branch, dateOpened, balance, history);
            case "INVESTMENT" -> InvestmentAccount.restore(accountNumber, owner, branch, dateOpened, balance, history);
            case "CHEQUE" -> ChequeAccount.restore(accountNumber, owner, branch, dateOpened, balance, history);
            default -> throw new IllegalStateException("Unknown account type in the database.");
        };
    }

    private List<Transaction> loadHistory(Connection connection, String accountNumber) throws SQLException {
        List<Transaction> history = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM transactions WHERE account_number = ? ORDER BY date_time, transaction_id")) {
            statement.setString(1, accountNumber);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    String period = rows.getString("interest_period");
                    history.add(new Transaction(rows.getString("transaction_id"), accountNumber,
                            TransactionType.valueOf(rows.getString("transaction_type")),
                            rows.getBigDecimal("amount"),
                            rows.getObject("date_time", LocalDateTime.class),
                            rows.getBigDecimal("resulting_balance"),
                            rows.getString("reference"),
                            period == null ? null : YearMonth.parse(period)));
                }
            }
        }
        return history;
    }
}