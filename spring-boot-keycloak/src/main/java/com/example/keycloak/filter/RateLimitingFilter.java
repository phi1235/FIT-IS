package com.example.keycloak.filter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate Limiting Filter sử dụng Bucket4j
 * 
 * - Giới hạn 100 requests/phút cho mỗi IP
 * - Giới hạn 10 requests/phút cho login endpoints
 * - Giới hạn 20 requests/phút cho mỗi user đã authenticated
 */
@Slf4j
@Component
@Order(1)
public class RateLimitingFilter extends OncePerRequestFilter {

    // Bucket cho mỗi IP address
    private final Map<String, Bucket> ipBuckets = new ConcurrentHashMap<>();

    // Bucket cho mỗi user (sau khi authenticated)
    private final Map<String, Bucket> userBuckets = new ConcurrentHashMap<>();

    // Rate limits from configuration
    @Value("${security.ratelimit.ip:100}")
    private int ipRateLimit;

    @Value("${security.ratelimit.login:10}")
    private int loginRateLimit;

    @Value("${security.ratelimit.user:20}")
    private int userRateLimit;

    @Value("${security.ratelimit.report:60}")
    private int reportStatusRateLimit;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String clientIp = getClientIpAddress(request);
        String path = request.getRequestURI();
        boolean isLoginEndpoint = path.contains("/api/auth/login");
        boolean isReportStatusEndpoint = path.contains("/api/reports/status/");

        // Kiểm tra rate limit cho IP
        Bucket ipBucket = getIpBucket(clientIp, isLoginEndpoint);

        if (!ipBucket.tryConsume(1)) {
            log.warn("RATE_LIMIT_EXCEEDED | ip={} | path={} | limit={}/min",
                    clientIp, path, isLoginEndpoint ? loginRateLimit : ipRateLimit);

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write(
                    String.format(
                            "{\"error\":\"Rate limit exceeded\",\"message\":\"Too many requests. Please try again later.\",\"retryAfter\":60}"));
            return;
        }

        // Kiểm tra rate limit cho user (nếu đã authenticated)
        // Reports status endpoint có rate limit riêng cao hơn
        String username = (String) request.getAttribute("username");
        if (username != null) {
            Bucket userBucket = getUserBucket(username, isReportStatusEndpoint);
            if (!userBucket.tryConsume(1)) {
                log.warn("USER_RATE_LIMIT_EXCEEDED | user={} | ip={} | path={}",
                        username, clientIp, path);

                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"error\":\"User rate limit exceeded\",\"message\":\"Too many requests for this user.\",\"retryAfter\":60}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private Bucket getIpBucket(String ip, boolean isLoginEndpoint) {
        return ipBuckets.computeIfAbsent(ip, k -> {
            int limit = isLoginEndpoint ? loginRateLimit : ipRateLimit;
            Bandwidth limitBandwidth = Bandwidth.simple(limit, Duration.ofMinutes(1));
            return Bucket.builder().addLimit(limitBandwidth).build();
        });
    }

    private Bucket getUserBucket(String username, boolean isReportStatus) {
        String bucketKey = isReportStatus ? username + ":reports" : username;
        return userBuckets.computeIfAbsent(bucketKey, k -> {
            int limit = isReportStatus ? reportStatusRateLimit : userRateLimit;
            Bandwidth limitBandwidth = Bandwidth.simple(limit, Duration.ofMinutes(1));
            return Bucket.builder().addLimit(limitBandwidth).build();
        });
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
