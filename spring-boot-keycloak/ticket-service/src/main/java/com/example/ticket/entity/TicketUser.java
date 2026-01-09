package com.example.ticket.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.UUID;

/**
 * Read-only mapping for user information from auth schema
 */
@Entity
@Table(name = "auth_user", schema = "auth")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketUser {

    @Id
    private UUID id;

    private String username;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    public String getFullName() {
        if (firstName == null && lastName == null) return username;
        return (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
    }
}
