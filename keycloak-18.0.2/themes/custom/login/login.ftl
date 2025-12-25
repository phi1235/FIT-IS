<#--
  Custom Keycloak Login Page - Angular App Style
-->
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Đăng nhập - ${realm.displayName!realm.name}</title>
    <link rel="stylesheet" href="${url.resourcesPath}/css/login.css">
</head>
<body>
<div class="kc-container">
    <div class="kc-card">
        <h1 class="kc-title">Đăng nhập</h1>
        <p class="kc-subtitle">Đăng nhập vào tài khoản của bạn</p>

        <#if message?has_content>
            <div class="kc-alert ${message.type}">${kcSanitize(message.summary)?no_esc}</div>
        </#if>

        <form id="kc-form-login" action="${url.loginAction}" method="post">

            <div class="form-group">
                <label for="username">Username hoặc Email</label>
                <input tabindex="1" 
                       id="username" 
                       name="username" 
                       type="text"
                       value="${(login.username!'')}" 
                       placeholder="Nhập username hoặc email"
                       autofocus>
            </div>

            <div class="form-group">
                <label for="password">Mật khẩu</label>
                <input tabindex="2" 
                       id="password" 
                       name="password" 
                       type="password"
                       placeholder="Nhập mật khẩu">
            </div>

            <#if realm.rememberMe>
                <div class="remember-me">
                    <input tabindex="3" 
                           id="rememberMe" 
                           name="rememberMe" 
                           type="checkbox"
                           <#if login.rememberMe??>checked</#if>>
                    <label for="rememberMe">Ghi nhớ đăng nhập</label>
                </div>
            </#if>

            <div class="kc-actions">
                <button tabindex="4" type="submit">Đăng nhập</button>
            </div>
        </form>

        <#if realm.resetPasswordAllowed>
            <div class="kc-links">
                <a href="${url.loginResetCredentialsUrl}">Quên mật khẩu?</a>
            </div>
        </#if>

        <#if realm.password && realm.registrationAllowed>
            <div class="kc-links" style="margin-top: 12px; border-top: none; padding-top: 0;">
                <span style="color: #666; font-size: 14px;">Chưa có tài khoản? </span>
                <a href="${url.registrationUrl}">Đăng ký</a>
            </div>
        </#if>

        <div class="kc-footer">
            <a href="http://localhost:4200" class="back-link">← Quay về trang chủ</a>
        </div>
    </div>
</div>
</body>
</html>
