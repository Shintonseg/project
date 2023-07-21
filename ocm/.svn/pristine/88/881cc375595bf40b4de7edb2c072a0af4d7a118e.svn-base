package com.tible.ocm.models;

/**
 * The enum Company type.
 *
 * @author tible
 */
public enum CompanyType {
    /**
     * Customer company type.
     */
    CUSTOMER, /**
     * Distribution center company type.
     */
    DISTRIBUTION_CENTER, /**
     * RVM supplier type.
     */
    RVM_OWNER;

    public static CompanyType getTypeFromString(String string) {
        if (string != null) {
            try {
                return CompanyType.valueOf(string.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                //do nothing
            }
        }
        return null;
    }

    public static String getStringFromType(CompanyType companyType) {
        if (companyType != null) {
            return companyType.name();
        }
        return null;
    }
}
