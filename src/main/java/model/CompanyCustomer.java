package model;

/**
 * A customer that is a company or organisation.
 */
public class CompanyCustomer extends Customer {

    private final String companyName;
    private final String registrationNumber;

    public CompanyCustomer(String customerId, String companyName, String registrationNumber,
                           String address, String contactDetails) {
        super(customerId, address, contactDetails);
        this.companyName = requireText(companyName, "Company name");
        this.registrationNumber = requireText(registrationNumber, "Registration number");
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    @Override
    public String getDisplayName() {
        return companyName;
    }
}