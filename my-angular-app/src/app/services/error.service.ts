import { Injectable } from '@angular/core';
import { ToastService } from './toast.service';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { ErrorResponse } from '../models/api.models';

@Injectable({
    providedIn: 'root'
})
export class ErrorService {

    constructor(
        private toastService: ToastService,
        private router: Router
    ) { }

    /**
     * Handle HTTP errors centrally
     */
    handleError(error: HttpErrorResponse): void {
        console.error('API Error:', error);

        if (error.status === 0) {
            // Network error or server not reachable
            this.toastService.error('Không thể kết nối đến server');
            return;
        }

        if (error.status === 401) {
            // Unauthorized - redirect to login
            this.toastService.error('Phiên đăng nhập hết hạn');
            this.router.navigate(['/login']);
            return;
        }

        if (error.status === 403) {
            // Forbidden
            this.toastService.error('Bạn không có quyền truy cập');
            return;
        }

        if (error.status === 404) {
            this.toastService.error('Không tìm thấy dữ liệu');
            return;
        }

        if (error.status === 400) {
            // Validation error
            const errorResponse = error.error as ErrorResponse;
            if (errorResponse?.details) {
                const messages = Object.values(errorResponse.details).join(', ');
                this.toastService.error(messages);
            } else {
                this.toastService.error(errorResponse?.message || 'Dữ liệu không hợp lệ');
            }
            return;
        }

        if (error.status >= 500) {
            this.toastService.error('Lỗi server. Vui lòng thử lại sau');
            return;
        }

        // Default error message
        const errorResponse = error.error as ErrorResponse;
        this.toastService.error(errorResponse?.message || 'Đã có lỗi xảy ra');
    }

    /**
     * Extract error message from response
     */
    getErrorMessage(error: HttpErrorResponse): string {
        if (error.status === 0) {
            return 'Không thể kết nối đến server';
        }

        const errorResponse = error.error as ErrorResponse;

        if (errorResponse?.details) {
            return Object.values(errorResponse.details).join(', ');
        }

        return errorResponse?.message || 'Đã có lỗi xảy ra';
    }
}
