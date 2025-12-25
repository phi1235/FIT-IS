package com.example.keycloak.service.federation;

/**
 * Federation Strategy Interface
 * Strategy Pattern: Cho phép thay đổi cách xác thực (LDAP, AD, API) một cách
 * linh hoạt
 * 
 * - Mỗi strategy phải handle account lockout
 */
public interface FederationStrategy {

    /**
     * Validate user credentials
     * 
     * @param username the username to validate
     * @param password the password to validate
     * @return true if credentials are valid
     */
    boolean validate(String username, String password);
    String getType();
    boolean supports(String type);
}
