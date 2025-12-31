
package com.example.keycloak.event;

import lombok.Getter;

/**
 * Event khi password được thay đổi
 */
@Getter
public class PasswordChangedEvent extends BaseEvent {
    
    private final String username;
    private final String changeType; // CHANGE, RESET
    
    public PasswordChangedEvent(Object source, String username, String changeType) {
        super(source, "PASSWORD_CHANGED", username);
        this.username = username;
        this.changeType = changeType;
    }
}
