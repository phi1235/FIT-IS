package com.example.keycloak.listener;

import com.example.keycloak.event.AuthenticationFailedEvent;
import com.example.keycloak.event.AuthenticationSuccessEvent;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listener chuyên xử lý security-related events
 * Ghi log vào security audit log riêng biệt
 */
@Slf4j
@Component
public class SecurityEventListener {
    
    // Separate logger for security events
    private static final Logger securityLog = LoggerFactory.getLogger("SECURITY");
    
    @Async
    @EventListener
    public void handleAuthenticationSuccess(AuthenticationSuccessEvent event) {
        securityLog.info("AUTH_SUCCESS | user={} | type={} | role={} | ip={} | timestamp={}",
            event.getUsername(),
            event.getAuthType(),
            event.getRole(),
            event.getIpAddress() != null ? event.getIpAddress() : "N/A",
            event.getEventTime()
        );
    }
    
    @Async
    @EventListener
    public void handleAuthenticationFailed(AuthenticationFailedEvent event) {
        securityLog.warn("AUTH_FAILED | user={} | type={} | reason={} | ip={} | timestamp={}",
            event.getUsername(),
            event.getAuthType(),
            event.getFailureReason(),
            event.getIpAddress() != null ? event.getIpAddress() : "N/A",
            event.getEventTime()
        );
    }
}
