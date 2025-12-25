package com.example.keycloak.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import org.springframework.http.HttpStatus;

import javax.annotation.PostConstruct;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * IP Whitelisting/Blacklisting Filter
 *
 * - Chặn các IP trong blacklist
 * - Chỉ cho phép IP trong whitelist (nếu whitelist được bật)
 * - Hỗ trợ CIDR notation cho IP ranges
 */
@Slf4j
@Component
@Order(0) // Chạy trước RateLimitingFilter
public class IpFilter extends OncePerRequestFilter {

    @Value("${security.ip.whitelist.enabled:false}")
    private boolean whitelistEnabled;
    
    @Value("${security.ip.whitelist:}")
    private String whitelistConfig;
    
    @Value("${security.ip.blacklist:}")
    private String blacklistConfig;
    
    private Set<String> whitelist = new HashSet<>();
    private Set<String> blacklist = new HashSet<>();
    private Set<String> whitelistCidr = new HashSet<>();
    
    // Cache để tránh parse lại IP mỗi lần
    private final ConcurrentHashMap<String, Boolean> ipCache = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void init() {
        // Parse whitelist
        if (whitelistConfig != null && !whitelistConfig.isEmpty()) {
            Arrays.stream(whitelistConfig.split(","))
                .map(String::trim)
                .forEach(ip -> {
                    if (ip.contains("/")) {
                        whitelistCidr.add(ip);
                    } else {
                        whitelist.add(ip);
                    }
                });
            log.info("IP Whitelist loaded: {} IPs, {} CIDR ranges", whitelist.size(), whitelistCidr.size());
        }
        
        // Parse blacklist
        if (blacklistConfig != null && !blacklistConfig.isEmpty()) {
            Arrays.stream(blacklistConfig.split(","))
                .map(String::trim)
                .forEach(ip -> blacklist.add(ip));
            log.info("IP Blacklist loaded: {} IPs", blacklist.size());
        }
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) 
            throws ServletException, IOException {
        
        String clientIp = getClientIpAddress(request);
        
        // Kiểm tra blacklist trước
        if (isBlacklisted(clientIp)) {
            log.warn("IP_BLOCKED | ip={} | reason=BLACKLISTED | path={}", 
                    clientIp, request.getRequestURI());
            
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType("application/json");
            response.getWriter().write(
                "{\"error\":\"Access denied\",\"message\":\"Your IP address has been blocked.\"}"
            );
            return;
        }
        
        // Kiểm tra whitelist nếu được bật
        if (whitelistEnabled && !isWhitelisted(clientIp)) {
            log.warn("IP_BLOCKED | ip={} | reason=NOT_WHITELISTED | path={}", 
                    clientIp, request.getRequestURI());
            
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType("application/json");
            response.getWriter().write(
                "{\"error\":\"Access denied\",\"message\":\"Your IP address is not authorized.\"}"
            );
            return;
        }
        
        filterChain.doFilter(request, response);
    }
    
    private boolean isBlacklisted(String ip) {
        // Kiểm tra cache trước
        Boolean cached = ipCache.get("blacklist:" + ip);
        if (cached != null) {
            return cached;
        }
        
        // Kiểm tra exact match
        boolean blacklisted = blacklist.contains(ip);
        
        // Cache kết quả
        ipCache.put("blacklist:" + ip, blacklisted);
        return blacklisted;
    }
    
    private boolean isWhitelisted(String ip) {
        // Kiểm tra cache trước
        Boolean cached = ipCache.get("whitelist:" + ip);
        if (cached != null) {
            return cached;
        }
        
        // Kiểm tra exact match
        if (whitelist.contains(ip)) {
            ipCache.put("whitelist:" + ip, true);
            return true;
        }
        
        // Kiểm tra CIDR ranges
        for (String cidr : whitelistCidr) {
            if (isIpInCidr(ip, cidr)) {
                ipCache.put("whitelist:" + ip, true);
                return true;
            }
        }
        
        ipCache.put("whitelist:" + ip, false);
        return false;
    }
    
    private boolean isIpInCidr(String ip, String cidr) {
        try {
            String[] parts = cidr.split("/");
            if (parts.length != 2) {
                return false;
            }
            
            String cidrIp = parts[0];
            int prefixLength = Integer.parseInt(parts[1]);
            
            long ipLong = ipToLong(ip);
            long cidrIpLong = ipToLong(cidrIp);
            long mask = ~((1L << (32 - prefixLength)) - 1);
            
            return (ipLong & mask) == (cidrIpLong & mask);
        } catch (Exception e) {
            log.error("Error checking CIDR: {}", e.getMessage());
            return false;
        }
    }
    
    private long ipToLong(String ip) {
        String[] parts = ip.split("\\.");
        return (Long.parseLong(parts[0]) << 24) +
               (Long.parseLong(parts[1]) << 16) +
               (Long.parseLong(parts[2]) << 8) +
               Long.parseLong(parts[3]);
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}

