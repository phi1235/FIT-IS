import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../services/auth.service';
import { ToastService } from '../services/toast.service';
import { CryptoService } from '../services/crypto.service';
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

    // Password migration state
    showMigrationDialog = false;
    migrationUsername = '';
    migrationCurrentPassword = '';

    constructor(
        private fb: FormBuilder,
        private http: HttpClient,
        private router: Router,
        private authService: AuthService,
        private toastService: ToastService,
        private cryptoService: CryptoService
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

        const plainPassword = this.loginForm.value.password;
        // Note: For v1 users, send plain password. V2 migration will hash on backend.
        // TODO: Once all users migrated to v2, re-enable client-side SHA256 hashing
        // const hashedPassword = this.cryptoService.hashPassword(plainPassword);

        const loginData = {
            username: this.loginForm.value.username.trim(),
            password: plainPassword // Send plain password for v1 compatibility
        };

        this.http.post<any>('/api/auth/login/database', loginData).subscribe({
            next: (response) => {
                this.isLoading = false;
                console.log('Login response:', response);

                // Check if password migration is required
                if (response.requiresPasswordMigration) {
                    this.showMigrationDialog = true;
                    this.migrationUsername = loginData.username;
                    this.migrationCurrentPassword = plainPassword; // Keep plain for migration
                    this.errorMessage = 'Bạn cần cập nhật mật khẩu để sử dụng định dạng bảo mật mới.';
                    return;
                }

                // Parse roles - handle both string and array formats
                let roles: string[] = [];
                const roleData = response.user?.role || response.userInfo?.roles || response.user?.roles;
                if (typeof roleData === 'string') {
                    const cleanRole = roleData.replace(/[\[\]]/g, '').trim();
                    if (cleanRole) {
                        roles = [cleanRole];
                    }
                } else if (Array.isArray(roleData)) {
                    roles = roleData;
                }

                const user = response.user || response.userInfo || {};
                const userInfo: UserInfo = {
                    username: user.username || loginData.username,
                    email: user.email,
                    firstName: user.firstName,
                    lastName: user.lastName,
                    roles: roles
                };

                console.log('Parsed userInfo:', userInfo);

                const token = response.token || response.tokenInfo || {};

                this.authService.setAuth(
                    token.accessToken || '',
                    token.refreshToken || '',
                    userInfo
                );

                this.toastService.success('Đăng nhập thành công!');

                const isAdmin = userInfo.roles.some(role =>
                    role === 'admin' || role === 'ROLE_admin' || role.toLowerCase().includes('admin')
                );

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

    /**
     * Handle password migration
     * User enters new password, which is hashed and sent to backend
     */
    migratePassword(newPassword: string): void {
        if (!newPassword || newPassword.length < 8) {
            this.errorMessage = 'Mật khẩu mới phải có ít nhất 8 ký tự';
            return;
        }

        this.isLoading = true;
        const hashedNewPassword = this.cryptoService.hashPassword(newPassword);

        const migrateData = {
            username: this.migrationUsername,
            currentPassword: this.migrationCurrentPassword, // Plain password for verification
            newPassword: hashedNewPassword // SHA256 hashed
        };

        this.http.post<any>('/api/auth/password/migrate', migrateData).subscribe({
            next: (response) => {
                this.isLoading = false;
                this.showMigrationDialog = false;
                this.toastService.success('Mật khẩu đã được cập nhật. Vui lòng đăng nhập lại!');

                // Update password in form and retry login
                this.loginForm.patchValue({ password: newPassword });
                this.onSubmit();
            },
            error: (error: HttpErrorResponse) => {
                this.isLoading = false;
                this.errorMessage = error.error?.message || 'Không thể cập nhật mật khẩu';
            }
        });
    }

    closeMigrationDialog(): void {
        this.showMigrationDialog = false;
        this.migrationUsername = '';
        this.migrationCurrentPassword = '';
    }
}
