package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Customer {

    private final String customerId;
    private final String address;
    private final String contactDetails;
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

    public String getContactDetails() {
        return contactDetails;
    }

    public abstract String getDisplayName();

    public List<Account> getAccounts() {
        return Collections.unmodifiableList(accounts);
    }

    void addAccount(Account account) {
        accounts.removeIf(existing -> existing.getAccountNumber().equals(account.getAccountNumber()));
        accounts.add(account);
    }

    void removeAccount(Account account) {
        accounts.remove(account);
    }

    protected static String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value.trim();
    }
}