package com.example.keycloak.provider;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.storage.UserStorageProviderFactory;

import java.util.List;

public class RemoteUserStorageProviderFactory implements UserStorageProviderFactory<RemoteUserStorageProvider> {

    public static final String PROVIDER_ID = "remote-content-provider";

    @Override
    public RemoteUserStorageProvider create(KeycloakSession session, ComponentModel model) {
        String apiUrl = model.getConfig().getFirst("apiUrl");
        return new RemoteUserStorageProvider(session, model, apiUrl);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getHelpText() {
        return "Remote User Storage Provider - Fetches users from an external REST API";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return List.of(
                new ProviderConfigProperty("apiUrl", "API URL",
                        "Base URL of the remote API. If Keycloak is in Docker and Backend on Host",
                        ProviderConfigProperty.STRING_TYPE,
                        "http://host.docker.internal:8082/api"));
    }
}
