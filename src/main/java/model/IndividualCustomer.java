package model;

public class IndividualCustomer extends Customer {

    private final String firstName;
    private final String surname;
    private final String identification;
    private boolean employed;
    private String employerName;
    private String employerAddress;

    public IndividualCustomer(String customerId, String firstName, String surname,
                              String identification, String address, String contactDetails) {
        super(customerId, address, contactDetails);
        this.firstName = requireText(firstName, "First name");
        this.surname = requireText(surname, "Surname");
        this.identification = requireText(identification, "Identification");
        this.employed = false;
    }

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

    public void setEmployment(String employerName, String employerAddress) {
        this.employerName = requireText(employerName, "Employer name");
        this.employerAddress = requireText(employerAddress, "Employer address");
        this.employed = true;
    }

    @Override
    public String getDisplayName() {
        return firstName + " " + surname;
    }
}