package com.example.keycloak.provider;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.adapter.AbstractUserAdapterFederatedStorage;

import java.util.stream.Stream;

/**
 * Adapter để map CustomUser với Keycloak UserModel
 * Hỗ trợ phân quyền động từ database
 */
public class CustomUserAdapter extends AbstractUserAdapterFederatedStorage {

    private final CustomUser user;
    private final ComponentModel componentModel;
    private final RealmModel realm;
    private final KeycloakSession session;

    public CustomUserAdapter(KeycloakSession session, RealmModel realm, ComponentModel model, CustomUser user) {
        super(session, realm, model);
        this.user = user;
        this.componentModel = model;
        this.realm = realm;
        this.session = session;
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public String getEmail() {
        return user.getEmail();
    }

    @Override
    public String getFirstName() {
        return user.getFirstName();
    }

    @Override
    public String getLastName() {
        return user.getLastName();
    }

    @Override
    public boolean isEnabled() {
        return user.isEnabled();
    }

    @Override
    public String getId() {
        return StorageId.keycloakId(componentModel, user.getId());
    }

    @Override
    public void setEmail(String email) {
        user.setEmail(email);
    }

    @Override
    public void setFirstName(String firstName) {
        user.setFirstName(firstName);
    }

    @Override
    public void setLastName(String lastName) {
        user.setLastName(lastName);
    }

    @Override
    public void setEnabled(boolean enabled) {
        user.setEnabled(enabled);
    }

    @Override
    public void setUsername(String username) {
        user.setUsername(username);
    }

    @Override
    public Stream<RoleModel> getRealmRoleMappingsStream() {
        if (user.getRole() != null && !user.getRole().isEmpty()) {
            RoleModel role = realm.getRole(user.getRole());
            if (role == null) {
                role = realm.addRole(user.getRole());
            }
            return Stream.of(role);
        }
        return Stream.empty();
    }

    @Override
    public boolean hasRole(RoleModel role) {
        if (user.getRole() != null && user.getRole().equals(role.getName())) {
            return true;
        }
        return super.hasRole(role);
    }

    @Override
    public Stream<RoleModel> getRoleMappingsStream() {
        return getRealmRoleMappingsStream();
    }
}
