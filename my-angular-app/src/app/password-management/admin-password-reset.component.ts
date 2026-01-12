import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { PasswordManagementService } from '../services/password-management.service';
import { AdminService } from '../services/admin.service';
import { ToastService } from '../services/toast.service';

export interface UserForReset {
  id: string;
  username: string;
  email: string;
  firstName?: string;
  lastName?: string;
}

@Component({
  selector: 'app-admin-password-reset',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  template: `
    <div class="admin-reset-container">
      <div class="reset-card">
        <div class="card-header">
          <h2>Admin Password Reset</h2>
          <p class="subtitle">Reset password for users</p>
        </div>

        <!-- Step 1: Select User -->
        <div *ngIf="currentStep === 1" class="step-content">
          <form [formGroup]="userSelectionForm" (ngSubmit)="submitUserSelection()">
            <div class="form-group">
              <label for="userId">Select User</label>
              <select
                id="userId"
                class="form-control"
                formControlName="userId"
                [disabled]="isLoading || isLoadingUsers"
              >
                <option value="">-- Choose a user --</option>
                <option *ngFor="let user of users" [value]="user.id">
                  {{ user.firstName || user.username }} {{ user.lastName || '' }} ({{ user.email }})
                </option>
              </select>
              <small *ngIf="userSelectionForm.get('userId')?.hasError('required')" class="text-danger">
                Please select a user
              </small>
            </div>

            <div class="form-group">
              <label for="reason">Reason (Optional)</label>
              <textarea
                id="reason"
                class="form-control"
                formControlName="reason"
                placeholder="Why are you resetting this user's password?"
                rows="4"
                [disabled]="isLoading"
              ></textarea>
            </div>

            <button
              type="submit"
              class="btn btn-primary w-100"
              [disabled]="userSelectionForm.invalid || isLoading"
            >
              {{ isLoading ? 'Processing...' : 'Generate Reset Code' }}
            </button>
          </form>
        </div>

        <!-- Step 2: Show Reset Code -->
        <div *ngIf="currentStep === 2" class="step-content success-container">
          <div class="success-icon">✓</div>
          <h3>Password Reset Code Generated</h3>
          
          <div class="reset-details">
            <div class="detail-item">
              <span class="label">User:</span>
              <span class="value">{{ selectedUser?.firstName || selectedUser?.username }} {{ selectedUser?.lastName || '' }}</span>
            </div>
            <div class="detail-item">
              <span class="label">Email:</span>
              <span class="value">{{ selectedUser?.email }}</span>
            </div>
          </div>

          <div class="reset-code-box">
            <label>Share this code with the user (expires in {{ adminCodeExpiryMinutes }} minutes):</label>
            <div class="code-display">
              <span class="code">{{ resetCode }}</span>
              <button 
                type="button" 
                class="copy-btn" 
                (click)="copyToClipboard()"
                [class.copied]="codeCopied"
              >
                {{ codeCopied ? '✓ Copied' : 'Copy' }}
              </button>
            </div>
          </div>

          <p class="info-text">
            The user will need to enter this code on the password reset page along with their email to set a new password.
          </p>

          <div class="button-group">
            <button 
              type="button" 
              class="btn btn-secondary" 
              (click)="resetForm()"
            >
              Reset Another User
            </button>
            <button 
              type="button" 
              class="btn btn-primary" 
              (click)="goToAdminDashboard()"
            >
              Back to Dashboard
            </button>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .admin-reset-container {
      padding: 20px;
    }

    .reset-card {
      background: white;
      border-radius: 8px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
      max-width: 600px;
      margin: 0 auto;
      overflow: hidden;
    }

    .card-header {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: white;
      padding: 30px;
      text-align: center;
    }

    .card-header h2 {
      margin: 0;
      font-size: 24px;
      font-weight: 600;
    }

    .subtitle {
      margin: 10px 0 0 0;
      font-size: 14px;
      opacity: 0.9;
    }

    .step-content {
      padding: 30px;
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
      font-family: inherit;
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

    textarea.form-control {
      resize: vertical;
    }

    .text-danger {
      color: #dc3545;
      display: block;
      margin-top: 5px;
      font-size: 12px;
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

    .btn-secondary:hover {
      background-color: #e0e0e0;
    }

    .w-100 {
      width: 100%;
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
      margin-bottom: 20px;
      font-size: 20px;
    }

    .reset-details {
      background-color: #f9f9f9;
      border: 1px solid #e0e0e0;
      border-radius: 4px;
      padding: 15px;
      margin-bottom: 20px;
      text-align: left;
    }

    .detail-item {
      display: flex;
      padding: 8px 0;
      border-bottom: 1px solid #f0f0f0;
    }

    .detail-item:last-child {
      border-bottom: none;
    }

    .detail-item .label {
      font-weight: 600;
      color: #666;
      min-width: 100px;
    }

    .detail-item .value {
      color: #333;
      flex-grow: 1;
    }

    .reset-code-box {
      background-color: #fff3cd;
      border: 1px solid #ffc107;
      border-radius: 4px;
      padding: 15px;
      margin-bottom: 20px;
      text-align: left;
    }

    .reset-code-box label {
      display: block;
      margin-bottom: 10px;
      font-weight: 500;
      color: #856404;
      margin: 0 0 10px 0;
    }

    .code-display {
      display: flex;
      align-items: center;
      gap: 10px;
      background-color: white;
      padding: 15px;
      border-radius: 4px;
      border: 1px solid #ffc107;
    }

    .code {
      font-size: 28px;
      font-weight: bold;
      font-family: 'Courier New', monospace;
      color: #333;
      flex-grow: 1;
      letter-spacing: 4px;
    }

    .copy-btn {
      padding: 8px 16px;
      background-color: #ffc107;
      border: none;
      border-radius: 4px;
      cursor: pointer;
      font-weight: 500;
      font-size: 12px;
      transition: all 0.3s;
      white-space: nowrap;
    }

    .copy-btn:hover {
      background-color: #ffb300;
    }

    .copy-btn.copied {
      background-color: #28a745;
      color: white;
    }

    .info-text {
      background-color: #e7f3ff;
      border-left: 4px solid #2196F3;
      padding: 12px;
      margin-bottom: 20px;
      border-radius: 4px;
      color: #0c5aa0;
      font-size: 13px;
      text-align: left;
      margin: 0 0 20px 0;
    }

    .button-group {
      display: flex;
      gap: 12px;
      justify-content: center;
    }

    .button-group .btn {
      flex: 1;
      max-width: 250px;
    }
  `]
})
export class AdminPasswordResetComponent implements OnInit, OnDestroy {
  currentStep = 1;
  isLoading = false;
  isLoadingUsers = false;

  users: UserForReset[] = [];
  selectedUser: UserForReset | null = null;
  resetCode: string = '';
  codeCopied = false;
  adminCodeExpiryMinutes = 60;

  userSelectionForm!: FormGroup;

  private destroy$ = new Subject<void>();

  constructor(
    private fb: FormBuilder,
    private passwordService: PasswordManagementService,
    private adminService: AdminService,
    private toastService: ToastService
  ) {
    this.initializeForm();
  }

  ngOnInit(): void {
    this.loadUsers();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private initializeForm(): void {
    this.userSelectionForm = this.fb.group({
      userId: ['', [Validators.required]],
      reason: ['']
    });
  }

  private loadUsers(): void {
    this.isLoadingUsers = true;
    // Load users from admin service
    this.adminService.getAllUsers()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response: any) => {
          this.isLoadingUsers = false;
          this.users = response.data || response || [];
        },
        error: (error: any) => {
          this.isLoadingUsers = false;
          this.toastService.error('Failed to load users');
        }
      });
  }

  submitUserSelection(): void {
    if (this.userSelectionForm.invalid) return;

    this.isLoading = true;
    const userId = this.userSelectionForm.get('userId')?.value;
    const reason = this.userSelectionForm.get('reason')?.value;

    // Find selected user
    this.selectedUser = this.users.find(u => u.id === userId) || null;

    this.passwordService.adminResetPassword(userId, reason)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.isLoading = false;
          this.resetCode = response.resetCode;
          this.adminCodeExpiryMinutes = response.codeExpiryMinutes;
          this.currentStep = 2;
          this.toastService.success(response.message);
        },
        error: (error) => {
          this.isLoading = false;
          const message = error.error?.message || 'Failed to generate reset code';
          this.toastService.error(message);
        }
      });
  }

  copyToClipboard(): void {
    navigator.clipboard.writeText(this.resetCode).then(() => {
      this.codeCopied = true;
      setTimeout(() => {
        this.codeCopied = false;
      }, 2000);
    }).catch(() => {
      this.toastService.error('Failed to copy code');
    });
  }

  resetForm(): void {
    this.currentStep = 1;
    this.userSelectionForm.reset();
    this.selectedUser = null;
    this.resetCode = '';
    this.codeCopied = false;
  }

  goToAdminDashboard(): void {
    // Navigate back to admin dashboard
    window.location.href = '/admin';
  }
}
