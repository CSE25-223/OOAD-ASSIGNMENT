package model;

/**
 * A customer who is a person.
 * Stores personal identification and optional employment details.
 */
public class IndividualCustomer extends Customer {

    private final String firstName;
    private final String surname;
    private final String identification;
    private boolean employed;
    private String employerName;
    private String employerAddress;

    /** Creates an individual customer who is not employed. */
    public IndividualCustomer(String customerId, String firstName, String surname,
                              String identification, String address, String contactDetails) {
        super(customerId, address, contactDetails);
        this.firstName = requireText(firstName, "First name");
        this.surname = requireText(surname, "Surname");
        this.identification = requireText(identification, "Identification");
        this.employed = false;
    }

    /** Creates an individual customer who is employed. */
    public IndividualCustomer(String customerId, String firstName, String surname,
                              String identification, String address, String contactDetails,
                              String employerName, String employerAddress) {
        this(customerId, firstName, surname, identification, address, contactDetails);
        setEmployment(employerName, employerAddress);
    }

    public String getFirstName() {
        return firstName;
    }

    public String getSurname() {
        return surname;
    }

    public String getIdentification() {
        return identification;
    }

    public boolean isEmployed() {
        return employed;
    }

    public String getEmployerName() {
        return employerName;
    }

    public String getEmployerAddress() {
        return employerAddress;
    }

    /** Records where the customer works. Both the name and address are required. */
    public void setEmployment(String employerName, String employerAddress) {
        this.employerName = requireText(employerName, "Employer name");
        this.employerAddress = requireText(employerAddress, "Employer address");
        this.employed = true;
    }

    /** Records that the customer is no longer employed. */
    public void clearEmployment() {
        this.employerName = null;
        this.employerAddress = null;
        this.employed = false;
    }

    @Override
    public String getDisplayName() {
        return firstName + " " + surname;
    }
}