package com.example.keycloak.service.federation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.naming.Context;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.Hashtable;

/**
 * LDAP Federation Strategy
 * Strategy Pattern Implementation cho LDAP Authentication
 * 
 * - Actual LDAP bind authentication
 * - Audit logging
 * - Secure connection handling
 */
@Component
public class LdapFederationStrategy extends BaseFederationStrategy {

    private static final Logger log = LoggerFactory.getLogger(LdapFederationStrategy.class);

    // Configuration - should come from application.properties
    private String ldapUrl = "ldap://localhost:389";
    private String ldapBaseDn = "dc=example,dc=com";
    private String ldapUserDnPattern = "uid={0},ou=users";

    @Override
    public boolean validate(String username, String password) {
        log.info("Validating user {} with LDAP", username);

        // Check account lockout
        if (isAccountLocked(username)) {
            auditLog.warn("LDAP_AUTH_BLOCKED | user={} | reason=ACCOUNT_LOCKED", username);
            return false;
        }

        try {
            // Setup LDAP environment
            Hashtable<String, String> env = new Hashtable<>();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            env.put(Context.PROVIDER_URL, ldapUrl);
            env.put(Context.SECURITY_AUTHENTICATION, "simple");

            // Construct user DN
            String userDn = ldapUserDnPattern.replace("{0}", username) + "," + ldapBaseDn;
            env.put(Context.SECURITY_PRINCIPAL, userDn);
            env.put(Context.SECURITY_CREDENTIALS, password);

            // Attempt LDAP bind (authentication)
            DirContext ctx = new InitialDirContext(env);
            ctx.close();

            // Success
            resetFailedAttempts(username);
            auditLog.info("LDAP_AUTH_SUCCESS | user={}", username);
            return true;

        } catch (javax.naming.AuthenticationException e) {
            recordFailedAttempt(username);
            auditLog.warn("LDAP_AUTH_FAILED | user={} | reason=INVALID_CREDENTIALS", username);
            return false;
        } catch (Exception e) {
            log.error("LDAP connection error for user {}: {}", username, e.getMessage());
            auditLog.error("LDAP_AUTH_ERROR | user={} | error={}", username, e.getMessage());
            return false;
        }
    }

    @Override
    public String getType() {
        return "LDAP";
    }

    @Override
    public boolean supports(String type) {
        return "ldap".equalsIgnoreCase(type);
    }
}
