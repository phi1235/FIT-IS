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
    <div class="p-4">
      <div class="row justify-content-center">
        <div class="col-md-8 col-lg-6">
          <div class="card border-0 shadow">
            <div class="card-header bg-white py-4 text-center border-0">
              <h3 class="fw-bold mb-1">Tạo Ticket Mới</h3>
              <p class="text-muted small">Nhập thông tin yêu cầu để gửi duyệt</p>
            </div>
            <div class="card-body px-4 pb-4">
              <form (ngSubmit)="onSubmit()" #ticketForm="ngForm">
                <div class="mb-4">
                  <label for="title" class="form-label fw-semibold">Tiêu đề yêu cầu <span class="text-danger">*</span></label>
                  <input type="text" class="form-control form-control-lg bg-light border-0 shadow-none" 
                         id="title" name="title" [(ngModel)]="request.title" 
                         required #title="ngModel" placeholder="Ví dụ: Yêu cầu mua sắm thiết bị">
                  <div *ngIf="title.invalid && (title.dirty || title.touched)" class="text-danger mt-1 small">
                    Vui lòng nhập tiêu đề.
                  </div>
                </div>

                <div class="mb-4">
                  <label for="description" class="form-label fw-semibold">Chi tiết mô tả</label>
                  <textarea class="form-control bg-light border-0 shadow-none" 
                            id="description" name="description" rows="4" 
                            [(ngModel)]="request.description"
                            placeholder="Mô tả cụ thể về yêu cầu của bạn..."></textarea>
                </div>

                <div class="mb-4">
                  <label for="amount" class="form-label fw-semibold">Số tiền dự kiến (VND)</label>
                  <div class="input-group">
                    <span class="input-group-text bg-light border-0">₫</span>
                    <input type="number" class="form-control form-control-lg bg-light border-0 shadow-none" 
                           id="amount" name="amount" [(ngModel)]="request.amount">
                  </div>
                </div>

                <div class="d-flex gap-3 pt-3">
                  <a [routerLink]="router.url.startsWith('/admin') ? '/admin/tickets' : '/tickets'" class="btn btn-light border flex-grow-1 py-2">Hủy bỏ</a>
                  <button type="submit" class="btn btn-primary flex-grow-1 py-2" [disabled]="ticketForm.invalid || loading">
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
      background-color: white !important;
      border-color: var(--fis-primary) !important;
      box-shadow: 0 0 0 0.25rem rgba(102, 126, 234, 0.1) !important;
    }
    .input-group-text { padding-right: 0.75rem; }
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
