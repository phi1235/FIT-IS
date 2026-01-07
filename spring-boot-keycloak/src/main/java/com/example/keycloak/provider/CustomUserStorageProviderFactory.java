package com.example.keycloak.provider;

import org.keycloak.Config;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.storage.UserStorageProviderFactory;

import java.util.List;

/**
 * Factory để tạo CustomUserStorageProvider
 */
public class CustomUserStorageProviderFactory implements UserStorageProviderFactory<CustomUserStorageProvider> {
    
    private static final String PROVIDER_ID = "custom-user-provider";
    
    @Override
    public CustomUserStorageProvider create(KeycloakSession session, ComponentModel model) {
        String dbUrl = model.getConfig().getFirst("dbUrl");
        String dbUser = model.getConfig().getFirst("dbUsername");
        String dbPassword = model.getConfig().getFirst("dbPassword");
        
        try {
            // Ensure PostgreSQL driver is registered even when loaded as a custom provider
            Class.forName("org.postgresql.Driver");

            java.sql.Connection connection = java.sql.DriverManager.getConnection(dbUrl, dbUser, dbPassword);
            CustomUserRepository userRepository = new CustomUserRepository(connection);
            return new CustomUserStorageProvider(session, model, userRepository);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("PostgreSQL driver not found.", e);
        } catch (java.sql.SQLException e) {
            throw new RuntimeException("Failed to connect to database: " + dbUrl, e);
        }
    }
    
    @Override
    public String getId() {
        return PROVIDER_ID;
    }
    
    @Override
    public String getHelpText() {
        return "Custom User Storage Provider - Sử dụng custom database để quản lý users";
    }
    
    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return List.of(
            new ProviderConfigProperty("dbUrl", "Database URL", 
                "JDBC URL của database", 
                ProviderConfigProperty.STRING_TYPE, 
                "jdbc:postgresql://postgres:5432/fis_bank"),
            new ProviderConfigProperty("dbUsername", "Database Username", 
                "Username để kết nối database", 
                ProviderConfigProperty.STRING_TYPE, 
                "sa"),
            new ProviderConfigProperty("dbPassword", "Database Password", 
                "Password để kết nối database", 
                ProviderConfigProperty.STRING_TYPE, 
                "")
        );
    }
    
    @Override
    public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel config) 
            throws ComponentValidationException {
        // Validate configuration
        String dbUrl = config.getConfig().getFirst("dbUrl");
        if (dbUrl == null || dbUrl.isEmpty()) {
            throw new ComponentValidationException("Database URL is required");
        }
    }
    
    @Override
    public void init(Config.Scope config) {
    }
    
    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }
    
    @Override
    public void close() {
    }
}

