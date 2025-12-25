package com.example.keycloak.service.federation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.naming.Context;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.Hashtable;

/**
 * Active Directory Federation Strategy
 * Strategy Pattern Implementation cho AD Authentication
 */
@Component
public class ActiveDirectoryFederationStrategy extends BaseFederationStrategy {

    private static final Logger log = LoggerFactory.getLogger(ActiveDirectoryFederationStrategy.class);

    private String adUrl = "ldap://localhost:389";
    private String adDomain = "example.com";

    @Override
    public boolean validate(String username, String password) {
        log.info("Validating user {} with Active Directory", username);

        if (isAccountLocked(username)) {
            auditLog.warn("AD_AUTH_BLOCKED | user={} | reason=ACCOUNT_LOCKED", username);
            return false;
        }

        try {
            Hashtable<String, String> env = new Hashtable<>();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            env.put(Context.PROVIDER_URL, adUrl);
            env.put(Context.SECURITY_AUTHENTICATION, "simple");

            String userPrincipal = username + "@" + adDomain;
            env.put(Context.SECURITY_PRINCIPAL, userPrincipal);
            env.put(Context.SECURITY_CREDENTIALS, password);

            DirContext ctx = new InitialDirContext(env);
            ctx.close();

            resetFailedAttempts(username);
            auditLog.info("AD_AUTH_SUCCESS | user={}", username);
            return true;

        } catch (javax.naming.AuthenticationException e) {
            recordFailedAttempt(username);
            auditLog.warn("AD_AUTH_FAILED | user={} | reason=INVALID_CREDENTIALS", username);
            return false;
        } catch (Exception e) {
            log.error("AD error for user {}: {}", username, e.getMessage());
            return false;
        }
    }

    @Override
    public String getType() {
        return "AD";
    }

    @Override
    public boolean supports(String type) {
        return "ad".equalsIgnoreCase(type) || "active_directory".equalsIgnoreCase(type);
    }
}
