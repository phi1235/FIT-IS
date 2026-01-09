package com.example.gateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Enumeration;

@RestController
@Slf4j
public class ProxyController {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${AUTH_SERVICE_URL}")
    private String authServiceUrl;

    @Value("${USER_SERVICE_URL}")
    private String userServiceUrl;

    @Value("${TICKET_SERVICE_URL}")
    private String ticketServiceUrl;

    @Value("${REPORT_SERVICE_URL}")
    private String reportServiceUrl;

    @RequestMapping("/api/auth/**")
    public ResponseEntity<byte[]> proxyAuth(HttpServletRequest request, @RequestBody(required = false) byte[] body) throws URISyntaxException {
        return proxyRequest(request, body, authServiceUrl);
    }

    @RequestMapping("/api/users/**")
    public ResponseEntity<byte[]> proxyUser(HttpServletRequest request, @RequestBody(required = false) byte[] body) throws URISyntaxException {
        return proxyRequest(request, body, userServiceUrl);
    }

    @RequestMapping("/api/tickets/**")
    public ResponseEntity<byte[]> proxyTicket(HttpServletRequest request, @RequestBody(required = false) byte[] body) throws URISyntaxException {
        return proxyRequest(request, body, ticketServiceUrl);
    }

    @RequestMapping("/api/reports/**")
    public ResponseEntity<byte[]> proxyReport(HttpServletRequest request, @RequestBody(required = false) byte[] body) throws URISyntaxException {
        return proxyRequest(request, body, reportServiceUrl);
    }

    private ResponseEntity<byte[]> proxyRequest(HttpServletRequest request, byte[] body, String targetUrl) throws URISyntaxException {
        String path = request.getRequestURI();
        String query = request.getQueryString();
        String url = targetUrl + path + (query != null ? "?" + query : "");

        log.info("Proxying {} {} to {}", request.getMethod(), path, url);

        HttpHeaders headers = new HttpHeaders();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            // Skip Host and Content-Length headers - they will be set correctly by RestTemplate
            if (!headerName.equalsIgnoreCase("Host") && 
                !headerName.equalsIgnoreCase("Content-Length") &&
                !headerName.equalsIgnoreCase("Transfer-Encoding")) {
                headers.set(headerName, request.getHeader(headerName));
            }
        }

        HttpEntity<byte[]> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(new URI(url), HttpMethod.valueOf(request.getMethod()), entity, byte[].class);
            // Remove Transfer-Encoding header to avoid chunked encoding issues with Angular dev server
            HttpHeaders responseHeaders = new HttpHeaders();
            response.getHeaders().forEach((key, value) -> {
                if (!key.equalsIgnoreCase("Transfer-Encoding")) {
                    responseHeaders.put(key, value);
                }
            });
            byte[] responseBody = response.getBody();
            if (responseBody != null) {
                responseHeaders.setContentLength(responseBody.length);
            }
            return ResponseEntity.status(response.getStatusCode())
                    .headers(responseHeaders)
                    .body(responseBody);
        } catch (HttpStatusCodeException e) {
            log.error("Proxy HTTP error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            HttpHeaders errorHeaders = new HttpHeaders();
            if (e.getResponseHeaders() != null) {
                e.getResponseHeaders().forEach((key, value) -> {
                    if (!key.equalsIgnoreCase("Transfer-Encoding")) {
                        errorHeaders.put(key, value);
                    }
                });
            }
            byte[] errorBody = e.getResponseBodyAsByteArray();
            errorHeaders.setContentLength(errorBody.length);
            return ResponseEntity.status(e.getStatusCode())
                    .headers(errorHeaders)
                    .body(errorBody);
        } catch (Exception e) {
            log.error("Proxy error for {}: {}", url, e.getMessage(), e);
            byte[] errorBody = ("Proxy error: " + e.getMessage()).getBytes();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentLength(errorBody.length)
                    .body(errorBody);
        }
    }
}
