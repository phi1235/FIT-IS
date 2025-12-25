import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ToastService, Toast } from '../services/toast.service';
import { Subscription } from 'rxjs';

@Component({
    selector: 'app-toast',
    standalone: true,
    imports: [CommonModule],
    template: `
    <div class="toast-container">
      <div 
        *ngFor="let toast of toasts" 
        class="toast" 
        [class]="'toast-' + toast.type"
        (click)="close(toast.id)"
      >
        <span class="toast-message">{{ toast.message }}</span>
        <button class="toast-close">x</button>
      </div>
    </div>
  `,
    styles: [`
    .toast-container {
      position: fixed;
      top: 16px;
      right: 16px;
      z-index: 9999;
      display: flex;
      flex-direction: column;
      gap: 8px;
      max-width: 300px;
    }

    .toast {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 10px;
      padding: 10px 14px;
      border-radius: 6px;
      box-shadow: 0 3px 12px rgba(0, 0, 0, 0.12);
      animation: slideIn 0.25s ease-out;
      cursor: pointer;
      transition: transform 0.2s, opacity 0.2s;
    }

    .toast:hover {
      transform: translateX(-2px);
    }

    .toast-success {
      background: #10b981;
      color: white;
    }

    .toast-error {
      background: #ef4444;
      color: white;
    }

    .toast-warning {
      background: #f59e0b;
      color: white;
    }

    .toast-info {
      background: #3b82f6;
      color: white;
    }

    .toast-message {
      font-size: 0.8rem;
      font-weight: 500;
      flex: 1;
    }

    .toast-close {
      background: none;
      border: none;
      color: rgba(255, 255, 255, 0.7);
      font-size: 0.9rem;
      cursor: pointer;
      padding: 0;
      line-height: 1;
    }

    .toast-close:hover {
      color: white;
    }

    @keyframes slideIn {
      from {
        transform: translateX(100%);
        opacity: 0;
      }
      to {
        transform: translateX(0);
        opacity: 1;
      }
    }

    @media (max-width: 480px) {
      .toast-container {
        left: 16px;
        right: 16px;
        max-width: none;
      }
    }
  `]
})
export class ToastComponent implements OnInit, OnDestroy {
    toasts: Toast[] = [];
    private subscription?: Subscription;

    constructor(private toastService: ToastService) { }

    ngOnInit(): void {
        this.subscription = this.toastService.toasts$.subscribe(toasts => {
            this.toasts = toasts;
        });
    }

    ngOnDestroy(): void {
        this.subscription?.unsubscribe();
    }

    close(id: number): void {
        this.toastService.remove(id);
    }
}
