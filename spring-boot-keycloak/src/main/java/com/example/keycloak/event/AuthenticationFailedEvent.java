package com.example.keycloak.event;

import lombok.Getter;

/**
 * Event khi authentication thất bại
 */
@Getter
public class AuthenticationFailedEvent extends BaseEvent {
    
    private final String username;
    private final String authType;
    private final String failureReason;
    
    public AuthenticationFailedEvent(Object source, String username, String authType,
                                       String failureReason, String ipAddress) {
        super(source, "AUTHENTICATION_FAILED", username, ipAddress);
        this.username = username;
        this.authType = authType;
        this.failureReason = failureReason;
    }
}
