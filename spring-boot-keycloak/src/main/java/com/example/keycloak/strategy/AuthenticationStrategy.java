package com.example.keycloak.strategy;

import com.example.keycloak.dto.LoginRequest;
import com.example.keycloak.dto.LoginResponse;

/**
 * Strategy interface cho các phương thức authentication khác nhau
 * Áp dụng Strategy Pattern để tách biệt các thuật toán authentication
 */
public interface AuthenticationStrategy {
    
    /**
     * Thực hiện authentication với strategy cụ thể
     * 
     * @param request LoginRequest chứa thông tin đăng nhập
     * @return LoginResponse chứa token và thông tin user
     * @throws AuthenticationException nếu authentication thất bại
     */
    LoginResponse authenticate(LoginRequest request) throws AuthenticationException;
    
    /**
     * Kiểm tra strategy này có hỗ trợ loại authentication này không
     * 
     * @param authType Loại authentication (database, federation, ldap, etc.)
     * @return true nếu strategy này hỗ trợ authType
     */
    boolean supports(String authType);
}

