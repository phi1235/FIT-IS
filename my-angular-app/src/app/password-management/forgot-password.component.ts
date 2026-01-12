import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { PasswordManagementService } from '../services/password-management.service';
import { ToastService } from '../services/toast.service';
import { PasswordStrengthComponent } from './password-strength.component';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, PasswordStrengthComponent],
  template: `
    <div class="forgot-password-container">
      <div class="forgot-password-card">
        <div class="card-header">
          <h2>Reset Your Password</h2>
          <p class="subtitle">{{ getCurrentStepDescription() }}</p>
        </div>

        <!-- Step 1: Enter Email -->
        <div *ngIf="currentStep === 1" class="step-content">
          <form [formGroup]="emailForm" (ngSubmit)="submitEmail()">
            <div class="form-group">
              <label for="email">Email Address</label>
              <input
                type="email"
                id="email"
                class="form-control"
                formControlName="email"
                placeholder="Enter your email"
                [disabled]="isLoading"
              />
              <small *ngIf="emailForm.get('email')?.hasError('required')" class="text-danger">
                Email is required
              </small>
              <small *ngIf="emailForm.get('email')?.hasError('email')" class="text-danger">
                Please enter a valid email
              </small>
            </div>
            <button
              type="submit"
              class="btn btn-primary w-100"
              [disabled]="emailForm.invalid || isLoading"
            >
              {{ isLoading ? 'Sending...' : 'Send Reset Code' }}
            </button>
          </form>
          <p class="text-center mt-3">
            Remember your password? <a href="/login">Login here</a>
          </p>
        </div>

        <!-- Step 2: Enter Reset Code -->
        <div *ngIf="currentStep === 2" class="step-content">
          <p class="info-message">
            We've sent a 6-digit code to your email. Please enter it below.
          </p>
          <form [formGroup]="codeForm" (ngSubmit)="submitCode()">
            <div class="form-group">
              <label for="resetCode">Reset Code</label>
              <input
                type="text"
                id="resetCode"
                class="form-control code-input"
                formControlName="resetCode"
                placeholder="000000"
                maxlength="6"
                inputmode="numeric"
                [disabled]="isLoading"
              />
              <small *ngIf="codeForm.get('resetCode')?.hasError('required')" class="text-danger">
                Reset code is required
              </small>
              <small *ngIf="codeForm.get('resetCode')?.hasError('pattern')" class="text-danger">
                Code must be 6 digits
              </small>
            </div>
            <button
              type="submit"
              class="btn btn-primary w-100"
              [disabled]="codeForm.invalid || isLoading"
            >
              {{ isLoading ? 'Verifying...' : 'Verify Code' }}
            </button>
            <button
              type="button"
              class="btn btn-link w-100 mt-2"
              (click)="goBackToEmail()"
              [disabled]="isLoading"
            >
              Use a different email
            </button>
          </form>
          <p class="text-center text-muted small mt-3">
            Code expires in {{ codeExpiryMinutes }} minutes
          </p>
        </div>

        <!-- Step 3: Set New Password -->
        <div *ngIf="currentStep === 3" class="step-content">
          <p class="success-message">✓ Code verified successfully</p>
          <form [formGroup]="passwordForm" (ngSubmit)="submitNewPassword()">
            <div class="form-group">
              <label for="newPassword">New Password</label>
              <input
                type="password"
                id="newPassword"
                class="form-control"
                formControlName="newPassword"
                placeholder="Enter your new password"
                [disabled]="isLoading"
              />
            </div>

            <!-- Password Strength Indicator -->
            <app-password-strength
              [password]="passwordForm.get('newPassword')?.value || ''"
            ></app-password-strength>

            <div class="form-group">
              <label for="confirmPassword">Confirm Password</label>
              <input
                type="password"
                id="confirmPassword"
                class="form-control"
                formControlName="confirmPassword"
                placeholder="Confirm your new password"
                [disabled]="isLoading"
              />
              <small *ngIf="passwordForm.get('confirmPassword')?.hasError('required')" class="text-danger">
                Password confirmation is required
              </small>
            </div>

            <button
              type="submit"
              class="btn btn-primary w-100"
              [disabled]="passwordForm.invalid || isLoading"
            >
              {{ isLoading ? 'Resetting Password...' : 'Reset Password' }}
            </button>
          </form>
        </div>

        <!-- Success Message -->
        <div *ngIf="currentStep === 4" class="step-content success-container">
          <div class="success-icon">✓</div>
          <h3>Password Reset Successfully</h3>
          <p>Your password has been changed. You can now login with your new password.</p>
          <button class="btn btn-primary w-100" (click)="goToLogin()">
            Go to Login
          </button>
        </div>
      </div>

      <!-- Progress Indicator -->
      <div class="progress-indicator" *ngIf="currentStep < 4">
        <span class="step" [class.active]="currentStep >= 1">1</span>
        <span class="connector" [class.active]="currentStep >= 2"></span>
        <span class="step" [class.active]="currentStep >= 2">2</span>
        <span class="connector" [class.active]="currentStep >= 3"></span>
        <span class="step" [class.active]="currentStep >= 3">3</span>
      </div>
    </div>
  `,
  styles: [`
    .forgot-password-container {
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: 100vh;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      padding: 20px;
    }

    .forgot-password-card {
      background: white;
      border-radius: 8px;
      box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
      width: 100%;
      max-width: 500px;
      padding: 40px;
    }

    .card-header {
      text-align: center;
      margin-bottom: 30px;
    }

    .card-header h2 {
      margin: 0;
      font-size: 28px;
      color: #333;
      font-weight: 600;
    }

    .subtitle {
      color: #666;
      margin: 10px 0 0 0;
      font-size: 14px;
    }

    .step-content {
      animation: fadeIn 0.3s ease-in;
    }

    @keyframes fadeIn {
      from {
        opacity: 0;
        transform: translateY(10px);
      }
      to {
        opacity: 1;
        transform: translateY(0);
      }
    }

    .form-group {
      margin-bottom: 20px;
    }

    .form-group label {
      display: block;
      margin-bottom: 8px;
      font-weight: 500;
      color: #333;
      font-size: 14px;
    }

    .form-control {
      width: 100%;
      padding: 10px 12px;
      border: 1px solid #ddd;
      border-radius: 4px;
      font-size: 14px;
      transition: border-color 0.3s;
    }

    .form-control:focus {
      outline: none;
      border-color: #667eea;
      box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
    }

    .form-control:disabled {
      background-color: #f5f5f5;
      cursor: not-allowed;
    }

    .code-input {
      text-align: center;
      font-size: 24px;
      letter-spacing: 10px;
      font-weight: bold;
    }

    .btn {
      padding: 10px 20px;
      border: none;
      border-radius: 4px;
      font-size: 14px;
      font-weight: 500;
      cursor: pointer;
      transition: all 0.3s;
    }

    .btn-primary {
      background-color: #667eea;
      color: white;
    }

    .btn-primary:hover:not(:disabled) {
      background-color: #5568d3;
      transform: translateY(-2px);
      box-shadow: 0 5px 15px rgba(102, 126, 234, 0.4);
    }

    .btn-primary:disabled {
      background-color: #ccc;
      cursor: not-allowed;
    }

    .btn-link {
      background: none;
      color: #667eea;
      text-decoration: none;
      padding: 8px 20px;
    }

    .btn-link:hover:not(:disabled) {
      text-decoration: underline;
    }

    .w-100 {
      width: 100%;
    }

    .mt-2 {
      margin-top: 15px;
    }

    .mt-3 {
      margin-top: 20px;
    }

    .text-center {
      text-align: center;
    }

    .text-danger {
      color: #dc3545;
      display: block;
      margin-top: 5px;
      font-size: 12px;
    }

    .text-muted {
      color: #999;
    }

    .small {
      font-size: 12px;
    }

    .info-message {
      background-color: #e7f3ff;
      border-left: 4px solid #2196F3;
      padding: 12px;
      margin-bottom: 20px;
      border-radius: 4px;
      color: #0c5aa0;
      font-size: 14px;
    }

    .success-message {
      background-color: #d4edda;
      border-left: 4px solid #28a745;
      padding: 12px;
      margin-bottom: 20px;
      border-radius: 4px;
      color: #155724;
      font-size: 14px;
    }

    .success-container {
      text-align: center;
    }

    .success-icon {
      font-size: 64px;
      color: #28a745;
      margin-bottom: 20px;
    }

    .success-container h3 {
      color: #333;
      margin-bottom: 10px;
    }

    .success-container p {
      color: #666;
      margin-bottom: 20px;
    }

    .progress-indicator {
      display: flex;
      justify-content: center;
      align-items: center;
      margin-top: 40px;
      gap: 10px;
    }

    .step {
      width: 40px;
      height: 40px;
      border-radius: 50%;
      background-color: #e0e0e0;
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: 600;
      color: #999;
      transition: all 0.3s;
    }

    .step.active {
      background-color: #667eea;
      color: white;
      box-shadow: 0 5px 15px rgba(102, 126, 234, 0.4);
    }

    .connector {
      width: 30px;
      height: 3px;
      background-color: #e0e0e0;
      transition: all 0.3s;
    }

    .connector.active {
      background-color: #667eea;
    }
  `]
})
export class ForgotPasswordComponent implements OnInit, OnDestroy {
  currentStep = 1;
  isLoading = false;
  codeExpiryMinutes = 15;

  emailForm!: FormGroup;
  codeForm!: FormGroup;
  passwordForm!: FormGroup;

  private destroy$ = new Subject<void>();

  constructor(
    private fb: FormBuilder,
    private passwordService: PasswordManagementService,
    private toastService: ToastService,
    private router: Router
  ) {
    this.initializeForms();
  }

  ngOnInit(): void {}

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private initializeForms(): void {
    this.emailForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]]
    });

    this.codeForm = this.fb.group({
      resetCode: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]]
    });

    this.passwordForm = this.fb.group({
      newPassword: ['', [Validators.required]],
      confirmPassword: ['', [Validators.required]]
    });
  }

  getCurrentStepDescription(): string {
    switch (this.currentStep) {
      case 1:
        return 'Enter your email to receive a password reset code';
      case 2:
        return 'Enter the 6-digit code sent to your email';
      case 3:
        return 'Create your new password';
      default:
        return '';
    }
  }

  submitEmail(): void {
    if (this.emailForm.invalid) return;

    this.isLoading = true;
    const email = this.emailForm.get('email')?.value;

    this.passwordService.forgotPassword(email)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.isLoading = false;
          this.passwordService.setResetSession(response.sessionId);
          this.codeExpiryMinutes = response.codeExpiryMinutes;
          this.currentStep = 2;
          this.toastService.success(response.message);
        },
        error: (error) => {
          this.isLoading = false;
          const message = error.error?.message || 'Failed to send reset code';
          this.toastService.error(message);
        }
      });
  }

  submitCode(): void {
    if (this.codeForm.invalid) return;

    this.isLoading = true;
    const sessionId = this.passwordService.getResetSession();
    const resetCode = this.codeForm.get('resetCode')?.value;

    if (!sessionId) {
      this.toastService.error('Session expired. Please start over.');
      this.goBackToEmail();
      return;
    }

    this.passwordService.verifyResetCode(sessionId, resetCode)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.isLoading = false;
          sessionStorage.setItem('verification_token', response.verificationToken);
          this.currentStep = 3;
          this.toastService.success(response.message);
        },
        error: (error) => {
          this.isLoading = false;
          const message = error.error?.message || 'Invalid reset code';
          this.toastService.error(message);
        }
      });
  }

  submitNewPassword(): void {
    if (this.passwordForm.invalid) return;

    const newPassword = this.passwordForm.get('newPassword')?.value;
    const confirmPassword = this.passwordForm.get('confirmPassword')?.value;

    if (newPassword !== confirmPassword) {
      this.toastService.error('Passwords do not match');
      return;
    }

    this.isLoading = true;
    const verificationToken = sessionStorage.getItem('verification_token');

    if (!verificationToken) {
      this.toastService.error('Verification token not found. Please start over.');
      this.goBackToEmail();
      return;
    }

    this.passwordService.setNewPassword(verificationToken, newPassword, confirmPassword)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.isLoading = false;
          this.currentStep = 4;
          this.toastService.success(response.message);
          this.passwordService.clearResetSession();
          sessionStorage.removeItem('verification_token');
        },
        error: (error) => {
          this.isLoading = false;
          const message = error.error?.message || 'Failed to reset password';
          this.toastService.error(message);
        }
      });
  }

  goBackToEmail(): void {
    this.currentStep = 1;
    this.emailForm.reset();
    this.codeForm.reset();
    this.passwordService.clearResetSession();
    sessionStorage.removeItem('verification_token');
  }

  goToLogin(): void {
    this.router.navigate(['/login']);
  }
}
