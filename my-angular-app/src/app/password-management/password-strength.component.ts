import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';

export interface PasswordStrength {
  score: number; // 0-5
  level: 'weak' | 'fair' | 'good' | 'strong' | 'very-strong';
  message: string;
  color: string;
}

@Component({
  selector: 'app-password-strength',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="password-strength-container" *ngIf="password">
      <div class="strength-bar">
        <div 
          class="strength-indicator" 
          [ngClass]="'strength-' + strength.level"
          [style.width.%]="strength.score * 20">
        </div>
      </div>
      <div class="strength-text" [style.color]="strength.color">
        <strong>{{ strength.message }}</strong>
      </div>
      <ul class="password-requirements">
        <li [class.met]="hasUppercase">
          <span class="requirement-icon">{{ hasUppercase ? '✓' : '✗' }}</span>
          At least one uppercase letter
        </li>
        <li [class.met]="hasLowercase">
          <span class="requirement-icon">{{ hasLowercase ? '✓' : '✗' }}</span>
          At least one lowercase letter
        </li>
        <li [class.met]="hasNumber">
          <span class="requirement-icon">{{ hasNumber ? '✓' : '✗' }}</span>
          At least one number
        </li>
        <li [class.met]="hasSpecialChar">
          <span class="requirement-icon">{{ hasSpecialChar ? '✓' : '✗' }}</span>
          At least one special character (!&#64;#$%^&amp;*)
        </li>
        <li [class.met]="hasMinLength">
          <span class="requirement-icon">{{ hasMinLength ? '✓' : '✗' }}</span>
          At least 12 characters
        </li>
      </ul>
    </div>
  `,
  styles: [`
    .password-strength-container {
      margin: 15px 0;
    }

    .strength-bar {
      height: 6px;
      background-color: #e0e0e0;
      border-radius: 3px;
      overflow: hidden;
      margin-bottom: 8px;
    }

    .strength-indicator {
      height: 100%;
      transition: width 0.3s ease, background-color 0.3s ease;
    }

    .strength-weak {
      background-color: #dc3545;
    }

    .strength-fair {
      background-color: #ff9800;
    }

    .strength-good {
      background-color: #ffc107;
    }

    .strength-strong {
      background-color: #66bb6a;
    }

    .strength-very-strong {
      background-color: #2e7d32;
    }

    .strength-text {
      font-size: 14px;
      margin-bottom: 10px;
      font-weight: 500;
    }

    .password-requirements {
      list-style: none;
      padding: 0;
      margin: 0;
      font-size: 13px;
    }

    .password-requirements li {
      padding: 5px 0;
      color: #999;
      display: flex;
      align-items: center;
    }

    .password-requirements li.met {
      color: #4caf50;
      font-weight: 500;
    }

    .requirement-icon {
      display: inline-block;
      width: 20px;
      margin-right: 8px;
      text-align: center;
      font-weight: bold;
    }
  `]
})
export class PasswordStrengthComponent implements OnChanges {
  @Input() password: string = '';

  strength: PasswordStrength = {
    score: 0,
    level: 'weak',
    message: 'Enter a password',
    color: '#999'
  };

  hasUppercase = false;
  hasLowercase = false;
  hasNumber = false;
  hasSpecialChar = false;
  hasMinLength = false;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['password']) {
      this.evaluatePasswordStrength();
    }
  }

  private evaluatePasswordStrength(): void {
    if (!this.password) {
      this.strength = {
        score: 0,
        level: 'weak',
        message: 'Enter a password',
        color: '#999'
      };
      this.resetRequirements();
      return;
    }

    // Check requirements
    this.hasUppercase = /[A-Z]/.test(this.password);
    this.hasLowercase = /[a-z]/.test(this.password);
    this.hasNumber = /\d/.test(this.password);
    this.hasSpecialChar = /[!@#$%^&*()_+\-=\[\]{};':",./<>?]/.test(this.password);
    this.hasMinLength = this.password.length >= 12;

    // Calculate score
    let score = 0;
    if (this.hasUppercase) score++;
    if (this.hasLowercase) score++;
    if (this.hasNumber) score++;
    if (this.hasSpecialChar) score++;
    if (this.hasMinLength) score++;

    // Determine level
    let level: 'weak' | 'fair' | 'good' | 'strong' | 'very-strong';
    let message: string;
    let color: string;

    if (score === 0) {
      level = 'weak';
      message = 'Very Weak';
      color = '#dc3545';
    } else if (score === 1) {
      level = 'weak';
      message = 'Weak';
      color = '#dc3545';
    } else if (score === 2) {
      level = 'fair';
      message = 'Fair';
      color = '#ff9800';
    } else if (score === 3) {
      level = 'good';
      message = 'Good';
      color = '#ffc107';
    } else if (score === 4) {
      level = 'strong';
      message = 'Strong';
      color = '#66bb6a';
    } else {
      level = 'very-strong';
      message = 'Very Strong';
      color = '#2e7d32';
    }

    this.strength = {
      score,
      level,
      message,
      color
    };
  }

  private resetRequirements(): void {
    this.hasUppercase = false;
    this.hasLowercase = false;
    this.hasNumber = false;
    this.hasSpecialChar = false;
    this.hasMinLength = false;
  }
}
