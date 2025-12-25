import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

export interface Toast {
    id: number;
    message: string;
    type: 'success' | 'error' | 'warning' | 'info';
    duration?: number;
}

@Injectable({
    providedIn: 'root'
})
export class ToastService {
    private toasts: Toast[] = [];
    private toastsSubject = new BehaviorSubject<Toast[]>([]);
    private idCounter = 0;

    public toasts$: Observable<Toast[]> = this.toastsSubject.asObservable();

    show(message: string, type: 'success' | 'error' | 'warning' | 'info' = 'info', duration: number = 4000): void {
        const toast: Toast = {
            id: ++this.idCounter,
            message,
            type,
            duration
        };

        this.toasts.push(toast);
        this.toastsSubject.next([...this.toasts]);

        // Auto remove after duration
        if (duration > 0) {
            setTimeout(() => this.remove(toast.id), duration);
        }
    }

    success(message: string, duration?: number): void {
        this.show(message, 'success', duration);
    }

    error(message: string, duration?: number): void {
        this.show(message, 'error', duration);
    }

    warning(message: string, duration?: number): void {
        this.show(message, 'warning', duration);
    }

    info(message: string, duration?: number): void {
        this.show(message, 'info', duration);
    }

    remove(id: number): void {
        this.toasts = this.toasts.filter(t => t.id !== id);
        this.toastsSubject.next([...this.toasts]);
    }

    clear(): void {
        this.toasts = [];
        this.toastsSubject.next([]);
    }
}
