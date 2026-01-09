package com.example.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Event emitted when authentication succeeds or fails
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AuthenticationEvent extends BaseEvent {
    
    private String authType;  // database, federation, ldap
    private boolean success;
    private String failureReason;
    
    @Override
    public String getEventType() {
        return success ? "AUTH_SUCCESS" : "AUTH_FAILURE";
    }
}
