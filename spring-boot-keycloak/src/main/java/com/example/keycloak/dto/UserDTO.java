package com.example.keycloak.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO để trả về thông tin user cho admin
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private String id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private boolean enabled;
}

