import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../services/auth.service';
import { ToastService } from '../services/toast.service';
import { LoginResponse, UserInfo } from '../models/api.models';
import { environment } from '../../environments/environment';

@Component({
    selector: 'app-login',
    standalone: true,
    imports: [CommonModule, ReactiveFormsModule, RouterLink],
    templateUrl: './login.component.html',
    styleUrl: './login.component.css'
})
export class LoginComponent implements OnInit {
    loginForm!: FormGroup;
    isLoading = false;
    errorMessage = '';
    successMessage = '';
    showPassword = false;

    constructor(
        private fb: FormBuilder,
        private http: HttpClient,
        private router: Router,
        private authService: AuthService,
        private toastService: ToastService
    ) { }

    ngOnInit(): void {
        // If already logged in, redirect to home
        if (this.authService.isAuthenticated) {
            this.router.navigate(['/home']);
            return;
        }

        this.loginForm = this.fb.group({
            username: ['', [
                Validators.required,
                Validators.minLength(3),
                Validators.maxLength(50),
                Validators.pattern(/^[a-zA-Z0-9._@-]+$/)
            ]],
            password: ['', [
                Validators.required,
                Validators.minLength(8),
                Validators.maxLength(128)
            ]]
        });
    }

    get f() {
        return this.loginForm.controls;
    }

    getErrorMessage(fieldName: string): string {
        const control = this.loginForm.get(fieldName);
        if (!control || !control.errors || !control.touched) return '';

        if (control.errors['required']) {
            return fieldName === 'username' ? 'Vui lòng nhập tên đăng nhập' : 'Vui lòng nhập mật khẩu';
        }
        if (control.errors['minlength']) {
            const minLength = control.errors['minlength'].requiredLength;
            return `Phải có ít nhất ${minLength} ký tự`;
        }
        if (control.errors['maxlength']) {
            const maxLength = control.errors['maxlength'].requiredLength;
            return `Không được vượt quá ${maxLength} ký tự`;
        }
        if (control.errors['pattern']) {
            return 'Chỉ được chứa chữ cái, số, dấu chấm, gạch dưới, @ và gạch ngang';
        }
        return '';
    }

    togglePassword(): void {
        this.showPassword = !this.showPassword;
    }

    onSubmit(): void {
        Object.keys(this.loginForm.controls).forEach(key => {
            this.loginForm.get(key)?.markAsTouched();
        });

        if (this.loginForm.invalid) {
            return;
        }

        this.isLoading = true;
        this.errorMessage = '';
        this.successMessage = '';

        const loginData = {
            username: this.loginForm.value.username.trim(),
            password: this.loginForm.value.password
        };

        this.http.post<any>('/api/auth/login/database', loginData).subscribe({
            next: (response) => {
                this.isLoading = false;
                console.log('Login response:', response);

                // Parse roles - handle both string and array formats
                let roles: string[] = [];
                const roleData = response.user?.role || response.userInfo?.roles || response.user?.roles;
                if (typeof roleData === 'string') {
                    // Handle string like "[ROLE_admin]" or "ROLE_admin" or "admin"
                    const cleanRole = roleData.replace(/[\[\]]/g, '').trim();
                    if (cleanRole) {
                        roles = [cleanRole];
                    }
                } else if (Array.isArray(roleData)) {
                    roles = roleData;
                }

                // Extract user info - handle both response.user and response.userInfo
                const user = response.user || response.userInfo || {};
                const userInfo: UserInfo = {
                    username: user.username || loginData.username,
                    email: user.email,
                    firstName: user.firstName,
                    lastName: user.lastName,
                    roles: roles
                };

                console.log('Parsed userInfo:', userInfo);

                // Extract token - handle both response.token and response.tokenInfo
                const token = response.token || response.tokenInfo || {};

                // Save auth state
                this.authService.setAuth(
                    token.accessToken || '',
                    token.refreshToken || '',
                    userInfo
                );

                // Show success toast
                this.toastService.success('Đăng nhập thành công!');

                // Check if admin role (handles both 'admin' and 'ROLE_admin')
                const isAdmin = userInfo.roles.some(role =>
                    role === 'admin' || role === 'ROLE_admin' || role.toLowerCase().includes('admin')
                );

                // Redirect after 1s
                setTimeout(() => {
                    if (isAdmin) {
                        this.router.navigate(['/admin']);
                    } else {
                        this.router.navigate(['/home']);
                    }
                }, 1000);
            },
            error: (error: HttpErrorResponse) => {
                this.isLoading = false;
                if (error.status === 400 && error.error?.details) {
                    const details = error.error.details;
                    const messages = Object.values(details).join(', ');
                    this.errorMessage = messages;
                } else if (error.status === 401) {
                    this.errorMessage = error.error?.message || 'Tên đăng nhập hoặc mật khẩu không đúng';
                } else if (error.status === 0) {
                    this.errorMessage = 'Không thể kết nối đến server. Vui lòng kiểm tra backend đang chạy.';
                } else {
                    this.errorMessage = 'Đã có lỗi xảy ra. Vui lòng thử lại sau.';
                }
            }
        });
    }
}
