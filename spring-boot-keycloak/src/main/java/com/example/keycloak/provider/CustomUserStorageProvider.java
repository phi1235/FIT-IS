package com.example.keycloak.provider;

import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserQueryProvider;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class CustomUserStorageProvider implements
        UserStorageProvider,
        UserLookupProvider,
        CredentialInputValidator,
        UserQueryProvider {

    private static final Logger log = LoggerFactory.getLogger(CustomUserStorageProvider.class);
    private static final Logger auditLog = LoggerFactory.getLogger("AUDIT");

    private final KeycloakSession session;
    private final ComponentModel model;
    private final CustomUserRepository userRepository;

    public CustomUserStorageProvider(KeycloakSession session, ComponentModel model,
            CustomUserRepository userRepository) {
        this.session = session;
        this.model = model;
        this.userRepository = userRepository;
    }

    @Override
    public void close() {
        userRepository.close();
    }

    // ========== UserLookupProvider Implementation ==========

    @Override
    public UserModel getUserById(String id, RealmModel realm) {
        String externalId = StorageId.externalId(id);
        CustomUser user = userRepository.findById(externalId);
        if (user != null) {
            return new CustomUserAdapter(session, realm, model, user);
        }
        return null;
    }

    @Override
    public UserModel getUserByUsername(String username, RealmModel realm) {
        CustomUser user = userRepository.findByUsername(username);
        if (user != null) {
            return new CustomUserAdapter(session, realm, model, user);
        }
        return null;
    }

    @Override
    public UserModel getUserByEmail(String email, RealmModel realm) {
        CustomUser user = userRepository.findByEmail(email);
        if (user != null) {
            return new CustomUserAdapter(session, realm, model, user);
        }
        return null;
    }

    // ========== CredentialInputValidator Implementation ==========

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return "password".equals(credentialType);
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        return supportsCredentialType(credentialType);
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput credentialInput) {
        log.info("=== isValid called for user: {}, credential type: {}",
                user != null ? user.getUsername() : "null",
                credentialInput != null ? credentialInput.getType() : "null");

        if (!supportsCredentialType(credentialInput.getType())) {
            log.info("Credential type not supported: {}", credentialInput.getType());
            return false;
        }

        String username = user.getUsername();
        String clientIp = getClientIpAddress();

        log.info("Starting password validation for user: {}", username);

        try {
            CustomUser customUser = userRepository.findByUsername(username);
            log.info("User found in database: {}", customUser != null ? customUser.getUsername() : "null");

            if (customUser == null) {
                auditLog.warn("AUTH_FAILED | user={} | reason=USER_NOT_FOUND | ip={}",
                        username, clientIp);
                return false;
            }

            if (!customUser.isEnabled()) {
                auditLog.warn("AUTH_FAILED | user={} | reason=ACCOUNT_DISABLED | ip={}",
                        username, clientIp);
                return false;
            }

            String inputPassword = credentialInput.getChallengeResponse();
            String storedHash = customUser.getPassword();

            log.info("Password validation for user: {}, hash length: {}",
                    username, storedHash != null ? storedHash.length() : 0);

            boolean isValid = verifyPassword(inputPassword, storedHash);

            if (isValid) {
                auditLog.info("AUTH_SUCCESS | user={} | ip={}", username, clientIp);
                return true;
            } else {
                auditLog.warn("AUTH_FAILED | user={} | reason=INVALID_PASSWORD | ip={}",
                        username, clientIp);
                return false;
            }

        } catch (Exception e) {
            log.error("Error during password validation for user {}: {}", username, e.getMessage(), e);
            auditLog.error("AUTH_ERROR | user={} | error={} | ip={}",
                    username, e.getMessage(), clientIp);
            return false;
        }
    }

    /**
     * Verify password using BCrypt
     */
    private boolean verifyPassword(String inputPassword, String storedHash) {
        if (inputPassword == null || storedHash == null) {
            log.warn("Password verification failed: null password or hash");
            return false;
        }

        String trimmedHash = storedHash.trim();
        log.info("verifyPassword - input length: {}, hash length: {}",
                inputPassword.length(), trimmedHash.length());

        try {
            return BCrypt.checkpw(inputPassword, trimmedHash);
        } catch (IllegalArgumentException e) {
            log.error("Invalid password hash format. Error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get client IP address for audit logging
     */
    private String getClientIpAddress() {
        try {
            // Try to get client IP from Keycloak context
            if (session != null && session.getContext() != null &&
                    session.getContext().getConnection() != null) {
                return session.getContext().getConnection().getRemoteAddr();
            }
        } catch (Exception e) {
            log.debug("Could not determine client IP: {}", e.getMessage());
        }
        return "unknown";
    }

    // ========== UserQueryProvider Implementation ==========

    @Override
    public int getUsersCount(RealmModel realm) {
        return userRepository.count();
    }

    @Override
    public Stream<UserModel> getUsersStream(RealmModel realm, Integer firstResult, Integer maxResults) {
        return userRepository.searchUsers("")
                .stream()
                .skip(firstResult == null ? 0 : firstResult)
                .limit(maxResults == null ? Integer.MAX_VALUE : maxResults)
                .map(user -> new CustomUserAdapter(session, realm, model, user));
    }

    @Override
    public Stream<UserModel> searchForUserStream(RealmModel realm, String search, Integer firstResult,
            Integer maxResults) {
        return userRepository.searchUsers(search)
                .stream()
                .skip(firstResult == null ? 0 : firstResult)
                .limit(maxResults == null ? Integer.MAX_VALUE : maxResults)
                .map(user -> new CustomUserAdapter(session, realm, model, user));
    }

    @Override
    public List<UserModel> getUsers(RealmModel realm, int firstResult, int maxResults) {
        return userRepository.searchUsers("")
                .stream()
                .skip(firstResult)
                .limit(maxResults)
                .map(user -> (UserModel) new CustomUserAdapter(session, realm, model, user))
                .toList();
    }

    @Override
    public List<UserModel> getUsers(RealmModel realm) {
        return getUsers(realm, 0, Integer.MAX_VALUE);
    }

    @Override
    public Stream<UserModel> searchForUserStream(RealmModel realm, Map<String, String> params, Integer firstResult,
            Integer maxResults) {
        String search = params.getOrDefault("username", "");
        return searchForUserStream(realm, search, firstResult, maxResults);
    }

    @Override
    public List<UserModel> searchForUser(Map<String, String> params, RealmModel realm, int firstResult,
            int maxResults) {
        String search = params.getOrDefault("username", "");
        return userRepository.searchUsers(search)
                .stream()
                .skip(firstResult)
                .limit(maxResults)
                .map(user -> (UserModel) new CustomUserAdapter(session, realm, model, user))
                .toList();
    }

    @Override
    public List<UserModel> searchForUser(Map<String, String> params, RealmModel realm) {
        return searchForUser(params, realm, 0, Integer.MAX_VALUE);
    }

    @Override
    public List<UserModel> searchForUser(String search, RealmModel realm, int firstResult, int maxResults) {
        return userRepository.searchUsers(search)
                .stream()
                .skip(firstResult)
                .limit(maxResults)
                .map(user -> (UserModel) new CustomUserAdapter(session, realm, model, user))
                .toList();
    }

    @Override
    public List<UserModel> searchForUser(String search, RealmModel realm) {
        return searchForUser(search, realm, 0, Integer.MAX_VALUE);
    }

    @Override
    public Stream<UserModel> getGroupMembersStream(RealmModel realm, GroupModel group, Integer firstResult,
            Integer maxResults) {
        return Stream.empty();
    }

    @Override
    public List<UserModel> getGroupMembers(RealmModel realm, GroupModel group, int firstResult, int maxResults) {
        return Collections.emptyList();
    }

    @Override
    public List<UserModel> getGroupMembers(RealmModel realm, GroupModel group) {
        return Collections.emptyList();
    }

    @Override
    public Stream<UserModel> searchForUserByUserAttributeStream(RealmModel realm, String attrName, String attrValue) {
        return Stream.empty();
    }

    @Override
    public List<UserModel> searchForUserByUserAttribute(String attrName, String attrValue, RealmModel realm) {
        return Collections.emptyList();
    }
}
