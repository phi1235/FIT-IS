package com.example.keycloak.config;

import com.example.keycloak.service.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Filter để validate JWT token trong mỗi request
 * Thay thế Keycloak token validation
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        // Skip nếu không có Authorization header hoặc không phải Bearer token
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);

        try {
            // Validate token và extract claims
            if (jwtService.validateToken(jwt)) {
                username = jwtService.extractUsername(jwt);
                String role = jwtService.extractRole(jwt);
                String userId = jwtService.extractUserId(jwt);

                // Tạo authorities từ role
                List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                    new SimpleGrantedAuthority("ROLE_" + role)
                );

                // Set authentication vào SecurityContext
                UsernamePasswordAuthenticationToken authToken = 
                    new UsernamePasswordAuthenticationToken(username, null, authorities);
                
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                
                // Store userId in request attribute for later use
                request.setAttribute("userId", userId);
                request.setAttribute("username", username);
                request.setAttribute("role", role);
                
                SecurityContextHolder.getContext().setAuthentication(authToken);
                
                log.debug("JWT authenticated user: {}, role: {}", username, role);
            }
        } catch (Exception e) {
            log.error("JWT validation error: {}", e.getMessage());
            // Don't set authentication - request will be handled as unauthenticated
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        // Skip JWT filter cho các public endpoints
        return path.startsWith("/api/auth/login") || 
               path.startsWith("/api/auth/register") ||
               path.startsWith("/api/auth/public") ||
               path.equals("/actuator/health") ||
               path.startsWith("/api/auth/mfa/setup");
    }
}
