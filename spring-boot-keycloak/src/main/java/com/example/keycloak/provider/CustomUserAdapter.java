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
    
    /**
     * Get realm roles từ database
     * Role được lấy từ field 'role' trong CustomUser và map vào Keycloak realm roles
     */
    @Override
    public Stream<RoleModel> getRealmRoleMappingsStream() {
        if (user.getRole() != null && !user.getRole().isEmpty()) {
            // Tìm role trong realm
            RoleModel role = realm.getRole(user.getRole());
            if (role == null) {
                // Tạo role nếu chưa tồn tại trong realm
                role = realm.addRole(user.getRole());
            }
            return Stream.of(role);
        }
        return Stream.empty();
    }
    
    /**
     * Check if user has a specific role
     */
    @Override
    public boolean hasRole(RoleModel role) {
        // Check role từ database trước
        if (user.getRole() != null && user.getRole().equals(role.getName())) {
            return true;
        }
        // Fallback to parent implementation
        return super.hasRole(role);
    }
    
    /**
     * Get all role mappings (realm + client)
     */
    @Override
    public Stream<RoleModel> getRoleMappingsStream() {
        return getRealmRoleMappingsStream();
    }
}

