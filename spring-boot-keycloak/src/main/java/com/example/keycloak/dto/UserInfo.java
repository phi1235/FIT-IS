package com.example.keycloak.dto;

import lombok.Data;

@Data
public class UserInfo {
    private String username;
    private String email;
    private String authorities;
}

