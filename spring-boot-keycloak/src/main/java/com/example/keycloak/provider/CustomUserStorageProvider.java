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

    private static final int BCRYPT_WORK_FACTOR = 12;

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
            int passwordVersion = customUser.getPasswordVersion();

            log.info("Password validation for user: {}, version: {}, hash length: {}",
                    username, passwordVersion, storedHash != null ? storedHash.length() : 0);

            boolean isValid = verifyPasswordWithVersion(inputPassword, storedHash, passwordVersion);

            if (isValid) {
                auditLog.info("AUTH_SUCCESS | user={} | version={} | ip={}", username, passwordVersion, clientIp);
                return true;
            } else {
                auditLog.warn("AUTH_FAILED | user={} | reason=INVALID_PASSWORD | version={} | ip={}",
                        username, passwordVersion, clientIp);
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
     * Verify password based on version
     * Version 1: BCrypt(plain_password) - OLD format
     * Version 2: BCrypt(SHA256(password)) - NEW format (client sends SHA256)
     */
    private boolean verifyPasswordWithVersion(String inputPassword, String storedHash, int version) {
        if (inputPassword == null || storedHash == null) {
            log.warn("Password verification failed: null password or hash");
            return false;
        }

        String trimmedHash = storedHash.trim();
        log.info("verifyPasswordWithVersion - version: {}, input length: {}, hash length: {}",
                version, inputPassword.length(), trimmedHash.length());

        try {
            if (version == 2) {
                // New format: input is already SHA256 hashed from client
                // Verify: BCrypt.check(SHA256_input, stored_BCrypt_hash)
                if (inputPassword.length() != 64) {
                    log.warn("Version 2 expects SHA256 input (64 chars), got: {}", inputPassword.length());
                    return false;
                }
                return BCrypt.checkpw(inputPassword, trimmedHash);
            } else {
                // Old format (version 1): input is plain password
                // Verify: BCrypt.check(plain_input, stored_BCrypt_hash)
                return BCrypt.checkpw(inputPassword, trimmedHash);
            }
        } catch (IllegalArgumentException e) {
            log.error("Invalid password hash format. Error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Legacy verifyPassword for backward compatibility
     */
    private boolean verifyPassword(String plainPassword, String hashedPassword) {
        return verifyPasswordWithVersion(plainPassword, hashedPassword, 1);
    }

    /**
     * Hash password for new format (expects SHA256 input from client)
     */
    public static String hashPassword(String sha256Password) {
        return BCrypt.hashpw(sha256Password, BCrypt.gensalt(BCRYPT_WORK_FACTOR));
    }

    /**
     * Hash plain password (for migration or direct use)
     */
    public static String hashPlainPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(BCRYPT_WORK_FACTOR));
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
