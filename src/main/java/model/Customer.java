package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Abstract superclass for all bank customers.
 * Stores the information shared by individual and company customers.
 */
public abstract class Customer {

    private final String customerId;
    private String address;
    private String contactDetails;
    private final List<Account> accounts = new ArrayList<>();

    protected Customer(String customerId, String address, String contactDetails) {
        this.customerId = requireText(customerId, "Customer ID");
        this.address = requireText(address, "Address");
        this.contactDetails = requireText(contactDetails, "Contact details");
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = requireText(address, "Address");
    }

    public String getContactDetails() {
        return contactDetails;
    }

    public void setContactDetails(String contactDetails) {
        this.contactDetails = requireText(contactDetails, "Contact details");
    }

    /** Each customer type decides how its name is displayed. */
    public abstract String getDisplayName();

    /** Returns a read-only view of the accounts this customer holds. */
    public List<Account> getAccounts() {
        return Collections.unmodifiableList(accounts);
    }

    /** Called by Account when it is created, so an account is always linked to its owner. */
    void addAccount(Account account) {
        accounts.add(account);
    }

    /** Rejects missing or blank values and returns the trimmed text. */
    protected static String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value.trim();
    }
}