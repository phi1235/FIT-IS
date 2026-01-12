import { Component, EventEmitter, OnDestroy, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { PasswordManagementService } from '../services/password-management.service';
import { ToastService } from '../services/toast.service';
import { AuthService } from '../services/auth.service';
import { PasswordStrengthComponent } from './password-strength.component';

@Component({
  selector: 'app-change-password-modal',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, PasswordStrengthComponent],
  template: `
    <div class="modal-overlay" *ngIf="isOpen" (click)="closeModal()">
      <div class="modal-content" (click)="$event.stopPropagation()">
        <div class="modal-header">
          <h2>Change Password</h2>
          <button class="close-btn" (click)="closeModal()" [disabled]="isLoading">×</button>
        </div>

        <div class="modal-body">
          <form [formGroup]="changePasswordForm" (ngSubmit)="submitChangePassword()">
            <!-- Current Password -->
            <div class="form-group">
              <label for="currentPassword">Current Password</label>
              <input
                type="password"
                id="currentPassword"
                class="form-control"
                formControlName="currentPassword"
                placeholder="Enter your current password"
                [disabled]="isLoading"
              />
              <small *ngIf="changePasswordForm.get('currentPassword')?.touched && changePasswordForm.get('currentPassword')?.hasError('required')" class="text-danger">
                Current password is required
              </small>
            </div>

            <!-- New Password -->
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
              <small *ngIf="changePasswordForm.get('newPassword')?.touched && changePasswordForm.get('newPassword')?.hasError('required')" class="text-danger">
                New password is required
              </small>
            </div>

            <!-- Password Strength Indicator -->
            <app-password-strength
              [password]="changePasswordForm.get('newPassword')?.value || ''"
            ></app-password-strength>

            <!-- Confirm Password -->
            <div class="form-group">
              <label for="confirmPassword">Confirm New Password</label>
              <input
                type="password"
                id="confirmPassword"
                class="form-control"
                formControlName="confirmPassword"
                placeholder="Confirm your new password"
                [disabled]="isLoading"
              />
              <small *ngIf="changePasswordForm.get('confirmPassword')?.touched && changePasswordForm.get('confirmPassword')?.hasError('required')" class="text-danger">
                Password confirmation is required
              </small>
              <small *ngIf="changePasswordForm.get('confirmPassword')?.touched && passwordMismatch" class="text-danger">
                Passwords do not match
              </small>
            </div>

            <!-- Form Actions -->
            <div class="modal-footer">
              <button
                type="button"
                class="btn btn-secondary"
                (click)="closeModal()"
                [disabled]="isLoading"
              >
                Cancel
              </button>
              <button
                type="submit"
                class="btn btn-primary"
                [disabled]="changePasswordForm.invalid || isLoading || passwordMismatch"
              >
                {{ isLoading ? 'Changing...' : 'Change Password' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .modal-overlay {
      position: fixed;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      background-color: rgba(0, 0, 0, 0.5);
      display: flex;
      justify-content: center;
      align-items: center;
      z-index: 1000;
      animation: fadeIn 0.3s ease-in;
    }

    @keyframes fadeIn {
      from {
        opacity: 0;
      }
      to {
        opacity: 1;
      }
    }

    .modal-content {
      background: white;
      border-radius: 8px;
      box-shadow: 0 10px 40px rgba(0, 0, 0, 0.3);
      width: 100%;
      max-width: 450px;
      animation: slideUp 0.3s ease-out;
    }

    @keyframes slideUp {
      from {
        transform: translateY(50px);
        opacity: 0;
      }
      to {
        transform: translateY(0);
        opacity: 1;
      }
    }

    .modal-header {
      padding: 24px;
      border-bottom: 1px solid #e0e0e0;
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    .modal-header h2 {
      margin: 0;
      font-size: 20px;
      font-weight: 600;
      color: #333;
    }

    .close-btn {
      background: none;
      border: none;
      font-size: 28px;
      color: #999;
      cursor: pointer;
      padding: 0;
      width: 32px;
      height: 32px;
      display: flex;
      align-items: center;
      justify-content: center;
      transition: color 0.3s;
    }

    .close-btn:hover:not(:disabled) {
      color: #333;
    }

    .close-btn:disabled {
      cursor: not-allowed;
      opacity: 0.5;
    }

    .modal-body {
      padding: 24px;
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

    .text-danger {
      color: #dc3545;
      display: block;
      margin-top: 5px;
      font-size: 12px;
    }

    .modal-footer {
      padding: 24px;
      border-top: 1px solid #e0e0e0;
      display: flex;
      justify-content: flex-end;
      gap: 12px;
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
      box-shadow: 0 5px 15px rgba(102, 126, 234, 0.4);
    }

    .btn-primary:disabled {
      background-color: #ccc;
      cursor: not-allowed;
    }

    .btn-secondary {
      background-color: #f0f0f0;
      color: #333;
    }

    .btn-secondary:hover:not(:disabled) {
      background-color: #e0e0e0;
    }

    .btn-secondary:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }
  `]
})
export class ChangePasswordModalComponent implements OnDestroy {
  @Output() close = new EventEmitter<void>();

  isOpen = false;
  isLoading = false;
  changePasswordForm!: FormGroup;

  private destroy$ = new Subject<void>();

  get passwordMismatch(): boolean {
    const form = this.changePasswordForm;
    const newPassword = form.get('newPassword')?.value;
    const confirmPassword = form.get('confirmPassword')?.value;
    return newPassword && confirmPassword && newPassword !== confirmPassword;
  }

  constructor(
    private fb: FormBuilder,
    private passwordService: PasswordManagementService,
    private toastService: ToastService,
    private authService: AuthService
  ) {
    this.initializeForm();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private initializeForm(): void {
    this.changePasswordForm = this.fb.group({
      currentPassword: ['', [Validators.required]],
      newPassword: ['', [Validators.required]],
      confirmPassword: ['', [Validators.required]]
    });
  }

  openModal(): void {
    this.isOpen = true;
    this.changePasswordForm.reset();
  }

  closeModal(): void {
    this.isOpen = false;
    this.changePasswordForm.reset();
    this.close.emit();
  }

  submitChangePassword(): void {
    if (this.changePasswordForm.invalid || this.passwordMismatch) {
      return;
    }

    this.isLoading = true;
    const currentPassword = this.changePasswordForm.get('currentPassword')?.value;
    const newPassword = this.changePasswordForm.get('newPassword')?.value;
    const confirmPassword = this.changePasswordForm.get('confirmPassword')?.value;

    this.passwordService.changePassword(currentPassword, newPassword, confirmPassword)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.isLoading = false;
          this.toastService.success(response.message);
          this.closeModal();
        },
        error: (error) => {
          this.isLoading = false;
          const message = error.error?.message || 'Failed to change password';
          this.toastService.error(message);
        }
      });
  }
}
