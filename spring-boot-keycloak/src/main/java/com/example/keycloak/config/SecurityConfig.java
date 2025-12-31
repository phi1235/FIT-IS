package com.example.keycloak.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;

/**
 * Security Configuration cho Local JWT Authentication
 * 
 * KHÔNG sử dụng Keycloak adapter để validate token
 * Thay vào đó sử dụng JwtAuthenticationFilter để validate local JWT
 * 
 * Security Features:
 * - JWT Authentication Filter cho local token
 * - STATELESS session (không dùng session)
 * - CORS configuration
 * - Role-based access control
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${security.cors.allowed-origins:http://localhost:4200}")
    private String allowedOrigins;

    @Value("${security.csp.connect-src:http://localhost:8082 http://localhost:8080}")
    private String cspConnectSrc;

    /**
     * BCrypt Password Encoder với work factor 12 (bank-level)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .cors().configurationSource(corsConfigurationSource()).and()

                // Authorization rules
                .authorizeRequests()
                .antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .antMatchers("/api/auth/public/**").permitAll()
                .antMatchers("/api/auth/public-key").permitAll()
                .antMatchers("/api/auth/login/**").permitAll()
                .antMatchers("/api/auth/register").permitAll()
                .antMatchers("/api/auth/password/migrate").permitAll()
                // Remote User Federation API endpoints (internal use only)
                .antMatchers(HttpMethod.GET, "/api").permitAll()
                .antMatchers(HttpMethod.POST, "/api/login").permitAll()
                .antMatchers("/actuator/health").permitAll()
                // Admin endpoints
                .antMatchers("/api/users/admin/**").hasRole("admin")
                // Report endpoints - cần authentication
                .antMatchers("/api/reports/users/**").hasRole("admin")
                .antMatchers("/api/reports/tickets/**").authenticated()
                .anyRequest().authenticated()
                .and()

                // Thêm JWT Authentication Filter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                // Tắt CSRF cho REST API
                .csrf().disable()

                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()

                // Security Headers
                .headers()
                .frameOptions().deny()
                .xssProtection().block(true)
                .and()
                .contentSecurityPolicy(
                        "default-src 'self' " + cspConnectSrc + "; " +
                                "connect-src 'self' " + cspConnectSrc + "; " +
                                "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
                                "style-src 'self' 'unsafe-inline'; " +
                                "img-src 'self' data:; " +
                                "font-src 'self'; " +
                                "frame-ancestors 'none';");
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        if (allowedOrigins != null && !allowedOrigins.isEmpty()) {
            configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        } else {
            configuration.setAllowedOrigins(Collections.singletonList("*"));
        }

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Cache-Control",
                "X-Requested-With", "Origin", "Accept", "X-CSRF-TOKEN"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(Collections.singletonList("X-CSRF-TOKEN"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
