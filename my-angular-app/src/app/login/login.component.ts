import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../services/auth.service';
import { ToastService } from '../services/toast.service';
import { CryptoService } from '../services/crypto.service';
import { UserInfo } from '../models/api.models';

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
        const username = this.loginForm.value.username.trim();

        // Encrypt both username and password with RSA before sending
        this.cryptoService.encryptPassword(username).subscribe({
            next: (encryptedUsername: string) => {
                this.cryptoService.encryptPassword(plainPassword).subscribe({
                    next: (encryptedPassword: string) => {
                        const loginData = {
                            username: encryptedUsername,
                            password: encryptedPassword
                        };
                        this.performLogin(loginData, plainPassword);
                    },
                    error: (err: any) => {
                        console.warn('RSA password encryption failed, falling back to plain:', err);
                        const loginData = {
                            username: encryptedUsername,
                            password: plainPassword
                        };
                        this.performLogin(loginData, plainPassword);
                    }
                });
            },
            error: (err: any) => {
                console.warn('RSA username encryption failed, falling back to plain:', err);
                this.cryptoService.encryptPassword(plainPassword).subscribe({
                    next: (encryptedPassword: string) => {
                        const loginData = {
                            username: username,
                            password: encryptedPassword
                        };
                        this.performLogin(loginData, plainPassword);
                    },
                    error: (pErr: any) => {
                        const loginData = {
                            username: username,
                            password: plainPassword
                        };
                        this.performLogin(loginData, plainPassword);
                    }
                });
            }
        });
    }

    private performLogin(loginData: { username: string; password: string }, plainPassword: string): void {
        this.http.post<any>('/api/auth/login/database', loginData).subscribe({
            next: (response: any) => {
                this.isLoading = false;
                console.log('Login response:', response);

                // Check if password migration is required
                if (response.requiresPasswordMigration) {
                    this.showMigrationDialog = true;
                    this.migrationUsername = loginData.username;
                    this.migrationCurrentPassword = plainPassword;
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
     */
    migratePassword(newPassword: string): void {
        if (!newPassword || newPassword.length < 8) {
            this.errorMessage = 'Mật khẩu mới phải có ít nhất 8 ký tự';
            return;
        }

        this.isLoading = true;

        // Encrypt new password with RSA
        this.cryptoService.encryptPassword(newPassword).subscribe({
            next: (encryptedNewPassword: string) => {
                const migrateData = {
                    username: this.migrationUsername,
                    currentPassword: this.migrationCurrentPassword,
                    newPassword: encryptedNewPassword
                };

                this.http.post<any>('/api/auth/password/migrate', migrateData).subscribe({
                    next: () => {
                        this.isLoading = false;
                        this.showMigrationDialog = false;
                        this.toastService.success('Mật khẩu đã được cập nhật. Vui lòng đăng nhập lại!');
                        this.loginForm.patchValue({ password: newPassword });
                        this.onSubmit();
                    },
                    error: (error: HttpErrorResponse) => {
                        this.isLoading = false;
                        this.errorMessage = error.error?.message || 'Không thể cập nhật mật khẩu';
                    }
                });
            },
            error: () => {
                this.isLoading = false;
                this.errorMessage = 'Không thể mã hóa mật khẩu';
            }
        });
    }

    closeMigrationDialog(): void {
        this.showMigrationDialog = false;
        this.migrationUsername = '';
        this.migrationCurrentPassword = '';
    }
}
