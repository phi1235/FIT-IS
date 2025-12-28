package com.example.keycloak.provider;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity đại diện cho User trong custom database
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomUser {
    private String id;
    private String username;
    private String email;
    private String password;

    @JsonAlias("first_name")
    private String firstName;

    @JsonAlias("last_name")
    private String lastName;

    private boolean enabled;
    private String role; // Role: 'admin' or 'user'

    // Password hash version: 1 = BCrypt(plain), 2 = BCrypt(SHA256(plain))
    @JsonAlias("password_version")
    private int passwordVersion = 1;

    // Constructor without passwordVersion for backward compatibility
    public CustomUser(String id, String username, String email, String password,
            String firstName, String lastName, boolean enabled, String role) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.enabled = enabled;
        this.role = role;
        this.passwordVersion = 1;
    }
}
