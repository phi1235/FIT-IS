package com.example.user.dto;

import lombok.Data;

@Data
public class UpdateMeRequest {
    private String firstName;
    private String lastName;
    private String email;
}
