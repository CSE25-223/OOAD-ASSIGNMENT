package dao;

import model.Customer;
import model.Role;
import model.UserAccount;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserAccountDAO {

    private static final String INSERT = "INSERT INTO user_accounts (username, password_hash, user_role, "
            + "failed_attempts, locked, customer_id, user_id) VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String UPDATE = "UPDATE user_accounts SET username = ?, password_hash = ?, "
            + "user_role = ?, failed_attempts = ?, locked = ?, customer_id = ? WHERE user_id = ?";

    private final Database database;
    private final CustomerDAO customerDAO;

    public UserAccountDAO(Database database, CustomerDAO customerDAO) {
        this.database = database;
        this.customerDAO = customerDAO;
    }

    public void create(UserAccount user) {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT)) {
            bind(statement, user);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not save the user account.", e);
        }
    }

    public Optional<UserAccount> findById(String userId) {
        return findOne("SELECT * FROM user_accounts WHERE user_id = ?", userId);
    }

    public Optional<UserAccount> findByUsername(String username) {
        if (username == null) {
            return Optional.empty();
        }
        return findOne("SELECT * FROM user_accounts WHERE LOWER(username) = ?",
                username.trim().toLowerCase());
    }

    public List<UserAccount> findAll() {
        List<UserAccount> users = new ArrayList<>();
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT * FROM user_accounts ORDER BY user_id");
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                users.add(map(rows));
            }
            return users;
        } catch (SQLException e) {
            throw new IllegalStateException("Could not read the user accounts.", e);
        }
    }

    public boolean update(UserAccount user) {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE)) {
            bind(statement, user);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Could not update the user account.", e);
        }
    }

    public boolean delete(String userId) {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM user_accounts WHERE user_id = ?")) {
            statement.setString(1, userId);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Could not delete the user account.", e);
        }
    }

    private Optional<UserAccount> findOne(String sql, String value) {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? Optional.of(map(rows)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not read the user account.", e);
        }
    }

    private void bind(PreparedStatement statement, UserAccount user) throws SQLException {
        statement.setString(1, user.getUsername());
        statement.setString(2, user.getPasswordHash());
        statement.setString(3, user.getRole().name());
        statement.setInt(4, user.getFailedAttempts());
        statement.setBoolean(5, user.isLocked());
        if (user.getCustomer() == null) {
            statement.setNull(6, Types.VARCHAR);
        } else {
            statement.setString(6, user.getCustomer().getCustomerId());
        }
        statement.setString(7, user.getUserId());
    }

    private UserAccount map(ResultSet rows) throws SQLException {
        String customerId = rows.getString("customer_id");
        Customer customer = null;
        if (customerId != null) {
            customer = customerDAO.findById(customerId).orElseThrow(
                    () -> new IllegalStateException("The customer for this login is missing."));
        }
        return UserAccount.restore(rows.getString("user_id"), rows.getString("username"),
                rows.getString("password_hash"), Role.valueOf(rows.getString("user_role")),
                customer, rows.getInt("failed_attempts"), rows.getBoolean("locked"));
    }
}