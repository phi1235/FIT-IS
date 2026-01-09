package com.example.auth.strategy;

import com.example.auth.dto.LoginRequest;
import com.example.auth.dto.LoginResponse;

public interface AuthenticationStrategy {

    /**
     * Thực hiện authentication với strategy cụ thể
     * 
     * @param request LoginRequest chứa thông tin đăng nhập
     * @return LoginResponse chứa token và thông tin user
     * @throws AuthenticationException nếu authentication thất bại
     */
    LoginResponse authenticate(LoginRequest request);

    boolean supports(String authType);
}
