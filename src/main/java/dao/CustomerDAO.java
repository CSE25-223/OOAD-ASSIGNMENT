package dao;

import model.CompanyCustomer;
import model.Customer;
import model.IndividualCustomer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CustomerDAO {

    private static final String INSERT = "INSERT INTO customers (customer_type, address, contact_details, "
            + "first_name, surname, identification, employed, employer_name, employer_address, "
            + "company_name, registration_number, customer_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String UPDATE = "UPDATE customers SET customer_type = ?, address = ?, "
            + "contact_details = ?, first_name = ?, surname = ?, identification = ?, employed = ?, "
            + "employer_name = ?, employer_address = ?, company_name = ?, registration_number = ? "
            + "WHERE customer_id = ?";

    private final Database database;

    public CustomerDAO(Database database) {
        this.database = database;
    }

    public void create(Customer customer) {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT)) {
            bind(statement, customer);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not save the customer.", e);
        }
    }

    public Optional<Customer> findById(String customerId) {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT * FROM customers WHERE customer_id = ?")) {
            statement.setString(1, customerId);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? Optional.of(map(rows)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not read the customer.", e);
        }
    }

    public List<Customer> findAll() {
        List<Customer> customers = new ArrayList<>();
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT * FROM customers ORDER BY customer_id");
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                customers.add(map(rows));
            }
            return customers;
        } catch (SQLException e) {
            throw new IllegalStateException("Could not read the customers.", e);
        }
    }

    public boolean update(Customer customer) {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE)) {
            bind(statement, customer);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Could not update the customer.", e);
        }
    }

    public boolean delete(String customerId) {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM customers WHERE customer_id = ?")) {
            statement.setString(1, customerId);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Could not delete the customer. Customers who hold accounts cannot be deleted.", e);
        }
    }

    private void bind(PreparedStatement statement, Customer customer) throws SQLException {
        statement.setString(2, customer.getAddress());
        statement.setString(3, customer.getContactDetails());
        statement.setString(12, customer.getCustomerId());

        if (customer instanceof IndividualCustomer individual) {
            statement.setString(1, "INDIVIDUAL");
            statement.setString(4, individual.getFirstName());
            statement.setString(5, individual.getSurname());
            statement.setString(6, individual.getIdentification());
            statement.setBoolean(7, individual.isEmployed());
            statement.setString(8, individual.getEmployerName());
            statement.setString(9, individual.getEmployerAddress());
            statement.setString(10, null);
            statement.setString(11, null);
        } else if (customer instanceof CompanyCustomer company) {
            statement.setString(1, "COMPANY");
            statement.setString(4, null);
            statement.setString(5, null);
            statement.setString(6, null);
            statement.setBoolean(7, false);
            statement.setString(8, null);
            statement.setString(9, null);
            statement.setString(10, company.getCompanyName());
            statement.setString(11, company.getRegistrationNumber());
        } else {
            throw new IllegalArgumentException("Unknown customer type.");
        }
    }

    private Customer map(ResultSet rows) throws SQLException {
        String customerId = rows.getString("customer_id");
        String address = rows.getString("address");
        String contactDetails = rows.getString("contact_details");

        if ("COMPANY".equals(rows.getString("customer_type"))) {
            return new CompanyCustomer(customerId, rows.getString("company_name"),
                    rows.getString("registration_number"), address, contactDetails);
        }

        String firstName = rows.getString("first_name");
        String surname = rows.getString("surname");
        String identification = rows.getString("identification");
        if (rows.getBoolean("employed")) {
            return new IndividualCustomer(customerId, firstName, surname, identification,
                    address, contactDetails,
                    rows.getString("employer_name"), rows.getString("employer_address"));
        }
        return new IndividualCustomer(customerId, firstName, surname, identification,
                address, contactDetails);
    }
}