package model;

import java.math.BigDecimal;

/**
 * Implemented by account types that earn monthly interest.
 */
public interface InterestBearing {

    /** Returns the interest earned for one month on the current balance. */
    BigDecimal calculateMonthlyInterest();
}