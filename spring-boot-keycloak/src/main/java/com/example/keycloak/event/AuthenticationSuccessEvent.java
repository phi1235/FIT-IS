package com.example.keycloak.event;

import lombok.Getter;

/**
 * Event khi authentication thành công
 */
@Getter
public class AuthenticationSuccessEvent extends BaseEvent {
    
    private final String username;
    private final String authType; // database, federation
    private final String role;
    
    public AuthenticationSuccessEvent(Object source, String username, String authType, 
                                       String role, String ipAddress) {
        super(source, "AUTHENTICATION_SUCCESS", username, ipAddress);
        this.username = username;
        this.authType = authType;
        this.role = role;
    }
}
