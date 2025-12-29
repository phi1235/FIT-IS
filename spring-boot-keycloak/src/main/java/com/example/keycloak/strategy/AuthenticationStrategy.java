package com.example.keycloak.strategy;

import com.example.keycloak.dto.LoginRequest;
import com.example.keycloak.dto.LoginResponse;

public interface AuthenticationStrategy {

    /**
     * Thực hiện authentication với strategy cụ thể
     * 
     * @param request LoginRequest chứa thông tin đăng nhập
     * @return LoginResponse chứa token và thông tin user
     * @throws AuthenticationException nếu authentication thất bại
     */
    LoginResponse authenticate(LoginRequest request) throws AuthenticationException;

    boolean supports(String authType);
}
