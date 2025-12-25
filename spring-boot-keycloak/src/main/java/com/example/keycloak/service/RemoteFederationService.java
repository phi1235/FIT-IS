package com.example.keycloak.service;

import com.example.keycloak.service.federation.FederationStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Remote Federation Service
 * 
 * Design Patterns Applied:
 * - Strategy Pattern: Delegate authentication to specific FederationStrategy
 * - Dependency Injection: Inject all available strategies via Spring
 * 
 */
@Slf4j
@Service
public class RemoteFederationService {

    private final List<FederationStrategy> strategies;
    private final FederationStrategy defaultStrategy;

    /**
     * Constructor Injection - Spring auto-injects all FederationStrategy beans
     */
    public RemoteFederationService(List<FederationStrategy> strategies) {
        this.strategies = strategies;
        // Default to API strategy if available, otherwise first strategy
        this.defaultStrategy = strategies.stream()
                .filter(s -> s.supports("api"))
                .findFirst()
                .orElse(strategies.isEmpty() ? null : strategies.get(0));

        log.info("Loaded {} federation strategies: {}",
                strategies.size(),
                strategies.stream().map(FederationStrategy::getType).toList());
    }

    /**
     * Validate với LDAP
     */
    public boolean validateWithLDAP(String username, String password) {
        return validateWith("ldap", username, password);
    }

    /**
     * Validate với Active Directory
     */
    public boolean validateWithActiveDirectory(String username, String password) {
        return validateWith("ad", username, password);
    }

    /**
     * Validate với External API
     */
    public boolean validateWithExternalAPI(String username, String password) {
        return validateWith("api", username, password);
    }

    /**
     * Validate credentials với specified strategy type
     */
    public boolean validateWith(String type, String username, String password) {
        FederationStrategy strategy = findStrategy(type);
        if (strategy == null) {
            log.error("No federation strategy found for type: {}", type);
            return false;
        }
        return strategy.validate(username, password);
    }

    /**
     * Validate credentials với default strategy
     */
    public boolean validateCredentials(String username, String password) {
        if (username == null || password == null ||
                username.isBlank() || password.isBlank()) {
            log.warn("Empty credentials provided");
            return false;
        }

        if (defaultStrategy == null) {
            log.error("No default federation strategy configured");
            return false;
        }

        return defaultStrategy.validate(username, password);
    }

    /**
     * Find strategy by type
     */
    private FederationStrategy findStrategy(String type) {
        return strategies.stream()
                .filter(s -> s.supports(type))
                .findFirst()
                .orElse(null);
    }

    /**
     * Get user info từ Remote Federation
     */
    public Object getUserInfo(String username) {
        log.info("Getting user info for {} from Remote Federation", username);
        return null;
    }
}
