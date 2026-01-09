import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { TicketService, TicketRequest } from '../services/ticket.service';

@Component({
  selector: 'app-ticket-create',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  template: `
    <div class="container-fluid py-4">
      <div class="row justify-content-center">
        <div class="col-md-8 col-lg-6">
          <div class="card shadow-sm border-0">
            <div class="card-header bg-white py-3">
              <h5 class="mb-0 fw-bold text-primary">Tạo Ticket Mới</h5>
              <p class="text-muted small mb-0">Vui lòng cung cấp thông tin yêu cầu bên dưới</p>
            </div>
            <div class="card-body p-4">
              <form (ngSubmit)="onSubmit()" #ticketForm="ngForm">
                <div class="mb-4">
                  <label class="form-label fw-semibold small text-uppercase text-secondary">Tiêu đề yêu cầu <span class="text-danger">*</span></label>
                  <input type="text" class="form-control form-control-lg border-2 shadow-none" 
                         style="border-color: #f1f5f9; background: #f8fafc;"
                         name="title" [(ngModel)]="request.title" 
                         required #title="ngModel" placeholder="Ví dụ: Mua sắm máy tính...">
                  <div *ngIf="title.invalid && (title.dirty || title.touched)" class="text-danger mt-1 small">
                    Tiêu đề là bắt buộc.
                  </div>
                </div>

                <div class="mb-4">
                  <label class="form-label fw-semibold small text-uppercase text-secondary">Mô tả chi tiết</label>
                  <textarea class="form-control border-2 shadow-none" 
                            style="border-color: #f1f5f9; background: #f8fafc;"
                            name="description" rows="4" 
                            [(ngModel)]="request.description"
                            placeholder="Mô tả cụ thể về yêu cầu của bạn..."></textarea>
                </div>

                <div class="mb-4">
                  <label class="form-label fw-semibold small text-uppercase text-secondary">Số tiền dự kiến (VND)</label>
                  <div class="input-group border-2 rounded-2 overflow-hidden" style="border-color: #f1f5f9;">
                    <span class="input-group-text bg-light border-0">₫</span>
                    <input type="number" class="form-control form-control-lg border-0 bg-light shadow-none" 
                           name="amount" [(ngModel)]="request.amount">
                  </div>
                </div>

                <div class="d-flex gap-3 pt-3">
                  <a [routerLink]="router.url.startsWith('/admin') ? '/admin/tickets' : '/tickets'" 
                     class="btn btn-light border flex-grow-1">Hủy bỏ</a>
                  <button type="submit" class="btn btn-primary flex-grow-1 fw-bold" [disabled]="ticketForm.invalid || loading">
                    <span *ngIf="loading" class="spinner-border spinner-border-sm me-2"></span>
                    {{ loading ? 'Đang xử lý...' : 'Tạo Ticket' }}
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .form-control:focus {
      background: white !important;
      border-color: #2563eb !important;
    }
  `]
})
export class TicketCreateComponent {
  request: TicketRequest = {
    title: '',
    description: '',
    amount: 0
  };
  loading = false;

  constructor(
    private ticketService: TicketService,
    public router: Router
  ) { }

  onSubmit(): void {
    this.loading = true;
    this.ticketService.createTicket(this.request).subscribe({
      next: (ticket) => {
        this.loading = false;
        // Chuyển hướng về trang chi tiết tương ứng với context (Portal hay Standalone)
        const currentUrl = this.router.url;
        const basePath = currentUrl.startsWith('/admin') ? '/admin/tickets' : '/tickets';
        this.router.navigate([basePath, ticket.id]);
      },
      error: (err) => {
        this.loading = false;
        alert('Có lỗi xảy ra khi tạo ticket: ' + (err.error?.message || err.message));
      }
    });
  }
}
