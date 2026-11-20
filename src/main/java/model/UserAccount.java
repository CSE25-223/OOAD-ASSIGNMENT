package model;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class UserAccount {

    private static final int MAX_FAILED_ATTEMPTS = 3;
    private static final int ITERATIONS = 120_000;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int SALT_BYTES = 16;

    private final String userId;
    private final String username;
    private final String passwordHash;
    private final Role role;
    private final Customer customer;
    private int failedAttempts;
    private boolean locked;

    public UserAccount(String userId, String username, String rawPassword, Role role, Customer customer) {
        this.userId = Customer.requireText(userId, "User ID");
        this.username = Customer.requireText(username, "Username");
        if (rawPassword == null || rawPassword.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters long.");
        }
        checkRoleAndCustomer(role, customer);
        this.passwordHash = hashPassword(rawPassword);
        this.role = role;
        this.customer = customer;
        this.failedAttempts = 0;
        this.locked = false;
    }

    private UserAccount(String userId, String username, String passwordHash, Role role,
                        Customer customer, int failedAttempts, boolean locked) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.customer = customer;
        this.failedAttempts = failedAttempts;
        this.locked = locked;
    }

    public static UserAccount restore(String userId, String username, String passwordHash, Role role,
                                      Customer customer, int failedAttempts, boolean locked) {
        if (passwordHash == null || !passwordHash.contains(":")) {
            throw new IllegalArgumentException("The stored password hash is not valid.");
        }
        if (failedAttempts < 0) {
            throw new IllegalArgumentException("Failed attempts cannot be negative.");
        }
        checkRoleAndCustomer(role, customer);
        return new UserAccount(Customer.requireText(userId, "User ID"),
                Customer.requireText(username, "Username"), passwordHash, role, customer,
                failedAttempts, locked);
    }

    private static void checkRoleAndCustomer(Role role, Customer customer) {
        if (role == null) {
            throw new IllegalArgumentException("A user account needs a role.");
        }
        if (role == Role.CUSTOMER && customer == null) {
            throw new IllegalArgumentException("A customer login must be linked to a customer.");
        }
        if (role == Role.BANK_STAFF && customer != null) {
            throw new IllegalArgumentException("A bank staff login cannot be linked to a customer.");
        }
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public Customer getCustomer() {
        return customer;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public boolean isLocked() {
        return locked;
    }

    public boolean checkPassword(String rawPassword) {
        if (rawPassword == null) {
            return false;
        }
        String[] parts = passwordHash.split(":");
        byte[] salt = Base64.getDecoder().decode(parts[0]);
        byte[] expected = Base64.getDecoder().decode(parts[1]);
        byte[] actual = derive(rawPassword, salt);
        return MessageDigest.isEqual(expected, actual);
    }

    public void registerFailure() {
        failedAttempts++;
        if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
            locked = true;
        }
    }

    public void resetFailures() {
        failedAttempts = 0;
    }

    public void unlock() {
        locked = false;
        failedAttempts = 0;
    }

    private static String hashPassword(String rawPassword) {
        byte[] salt = new byte[SALT_BYTES];
        new SecureRandom().nextBytes(salt);
        byte[] hash = derive(rawPassword, salt);
        return Base64.getEncoder().encodeToString(salt) + ":" + Base64.getEncoder().encodeToString(hash);
    }

    private static byte[] derive(String rawPassword, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(rawPassword.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Password hashing is not available.");
        }
    }
}