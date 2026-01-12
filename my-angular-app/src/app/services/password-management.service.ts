import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { environment } from '../../environments/environment';

export interface ForgotPasswordRequest {
  email: string;
}

export interface ForgotPasswordResponse {
  sessionId: string;
  message: string;
  expiresAt: string;
  codeExpiryMinutes: number;
}

export interface VerifyResetCodeRequest {
  sessionId: string;
  resetCode: string;
}

export interface VerifyResetCodeResponse {
  verificationToken: string;
  verified: boolean;
  message: string;
}

export interface SetNewPasswordRequest {
  verificationToken: string;
  newPassword: string;
  confirmPassword: string;
}

export interface SetNewPasswordResponse {
  success: boolean;
  message: string;
  redirectUrl: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
}

export interface ChangePasswordResponse {
  success: boolean;
  message: string;
}

export interface AdminResetPasswordRequest {
  userId: string;
  reason?: string;
}

export interface AdminResetPasswordResponse {
  sessionId: string;
  resetCode: string;
  expiresAt: string;
  codeExpiryMinutes: number;
  message: string;
}

@Injectable({
  providedIn: 'root'
})
export class PasswordManagementService {
  private apiUrl = '/api/auth';
  private resetSessionSubject = new BehaviorSubject<string | null>(null);
  public resetSession$ = this.resetSessionSubject.asObservable();

  constructor(private http: HttpClient) {}

  /**
   * Initiate forgot password flow
   */
  forgotPassword(email: string): Observable<ForgotPasswordResponse> {
    const request: ForgotPasswordRequest = { email };
    return this.http.post<ForgotPasswordResponse>(
      `${this.apiUrl}/forgot-password`,
      request
    );
  }

  /**
   * Verify reset code
   */
  verifyResetCode(sessionId: string, resetCode: string): Observable<VerifyResetCodeResponse> {
    const request: VerifyResetCodeRequest = { sessionId, resetCode };
    return this.http.post<VerifyResetCodeResponse>(
      `${this.apiUrl}/verify-reset-code`,
      request
    );
  }

  /**
   * Set new password after verification
   */
  setNewPassword(verificationToken: string, newPassword: string, confirmPassword: string): Observable<SetNewPasswordResponse> {
    const request: SetNewPasswordRequest = { verificationToken, newPassword, confirmPassword };
    return this.http.post<SetNewPasswordResponse>(
      `${this.apiUrl}/set-new-password`,
      request
    );
  }

  /**
   * Change password for authenticated user
   */
  changePassword(currentPassword: string, newPassword: string, confirmPassword: string): Observable<ChangePasswordResponse> {
    const request: ChangePasswordRequest = { currentPassword, newPassword, confirmPassword };
    return this.http.post<ChangePasswordResponse>(
      `${this.apiUrl}/change-password`,
      request
    );
  }

  /**
   * Admin reset password
   */
  adminResetPassword(userId: string, reason?: string): Observable<AdminResetPasswordResponse> {
    const request: AdminResetPasswordRequest = { userId, reason };
    return this.http.post<AdminResetPasswordResponse>(
      `${this.apiUrl}/admin/reset-password`,
      request
    );
  }

  /**
   * Store reset session ID
   */
  setResetSession(sessionId: string): void {
    this.resetSessionSubject.next(sessionId);
    sessionStorage.setItem('password_reset_session', sessionId);
  }

  /**
   * Get stored reset session ID
   */
  getResetSession(): string | null {
    const session = this.resetSessionSubject.value || sessionStorage.getItem('password_reset_session');
    return session;
  }

  /**
   * Clear reset session
   */
  clearResetSession(): void {
    this.resetSessionSubject.next(null);
    sessionStorage.removeItem('password_reset_session');
  }
}
