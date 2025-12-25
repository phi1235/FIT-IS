import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TicketService, TicketDTO, TicketStatus } from '../services/ticket.service';
import { KeycloakService } from '../services/keycloak.service';

@Component({
  selector: 'app-ticket-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  template: `
    <div class="p-4">
      <div *ngIf="ticket" class="row justify-content-center">
        <div class="col-md-10">
          <!-- Header Actions -->
          <div class="d-flex justify-content-between align-items-center mb-4">
            <a [routerLink]="router.url.startsWith('/admin') ? '/admin/tickets' : '/tickets'" class="btn btn-light border d-flex align-items-center gap-2">
              <span>←</span> Quay lại danh sách
            </a>
            <div class="d-flex gap-2">
              <button *ngIf="canSubmit()" (click)="submit()" class="btn btn-success shadow-sm">Gửi phê duyệt</button>
              <button *ngIf="canApproveOrReject()" (click)="approve()" class="btn btn-primary shadow-sm">Phê duyệt</button>
              <button *ngIf="canApproveOrReject()" (click)="showRejectForm = !showRejectForm" class="btn btn-danger shadow-sm">Từ chối</button>
            </div>
          </div>

          <div class="card border-0 shadow-sm overflow-hidden mb-4">
            <div class="card-header bg-white d-flex justify-content-between align-items-center py-3 border-bottom">
              <div>
                <span class="text-muted small text-uppercase fw-bold ls-1">Thông tin Ticket</span>
                <h3 class="mb-0 fw-bold">#{{ ticket.id }} - {{ ticket.title }}</h3>
              </div>
              <span [class]="'badge ' + getStatusClass(ticket.status)">{{ getStatusLabel(ticket.status) }}</span>
            </div>
            
            <div class="card-body p-4">
              <div class="row g-4">
                <!-- Main Info -->
                <div class="col-md-8">
                  <div class="mb-4">
                    <h6 class="text-uppercase text-muted fw-bold small mb-2">Mô tả chi tiết</h6>
                    <div class="p-3 bg-light rounded text-dark" style="min-height: 100px; white-space: pre-wrap;">
                      {{ ticket.description || 'Không có mô tả chi tiết cho ticket này.' }}
                    </div>
                  </div>

                  <div *ngIf="ticket.rejectionReason" class="mt-4">
                    <div class="alert alert-danger border-0 shadow-sm">
                      <h6 class="alert-heading fw-bold d-flex align-items-center gap-2">
                        <span>⚠️</span> Lý do từ chối:
                      </h6>
                      <p class="mb-0">{{ ticket.rejectionReason }}</p>
                    </div>
                  </div>

                  <!-- Rejection Form -->
                  <div *ngIf="showRejectForm" class="mt-4 p-4 border rounded-3 bg-light-danger shadow-sm">
                    <h6 class="fw-bold mb-3">Nhập lý do từ chối</h6>
                    <div class="mb-3">
                      <textarea class="form-control border-danger-subtle" rows="3" 
                                [(ngModel)]="rejectionReason" 
                                placeholder="Vui lòng cung cấp lý do cụ thể để Maker điều chỉnh..."></textarea>
                    </div>
                    <div class="d-flex justify-content-end gap-2">
                      <button (click)="showRejectForm = false" class="btn btn-sm btn-light border">Hủy</button>
                      <button (click)="reject()" class="btn btn-sm btn-danger" [disabled]="!rejectionReason">Xác nhận từ chối</button>
                    </div>
                  </div>
                </div>

                <!-- Meta Info -->
                <div class="col-md-4">
                  <div class="p-4 bg-light rounded-3 shadow-none border">
                    <div class="mb-4">
                      <h6 class="text-uppercase text-muted fw-bold small mb-3">Thông số tài chính</h6>
                      <h2 class="fw-bold text-primary mb-0">{{ ticket.amount | currency:'VND':'symbol':'1.0-0' }}</h2>
                      <span class="text-muted small">Tổng giá trị yêu cầu</span>
                    </div>

                    <div class="mb-3 border-top pt-3">
                      <h6 class="text-uppercase text-muted fw-bold small mb-2">Người lập (Maker)</h6>
                      <div class="d-flex align-items-center gap-2">
                        <div class="avatar-sm">{{ ticket.maker.charAt(0).toUpperCase() }}</div>
                        <span class="fw-semibold">{{ ticket.maker }}</span>
                      </div>
                    </div>

                    <div class="mb-3">
                      <h6 class="text-uppercase text-muted fw-bold small mb-2">Người duyệt (Checker)</h6>
                      <div class="d-flex align-items-center gap-2" *ngIf="ticket.checker; else noChecker">
                        <div class="avatar-sm bg-primary-subtle text-primary">{{ ticket.checker.charAt(0).toUpperCase() }}</div>
                        <span class="fw-semibold">{{ ticket.checker }}</span>
                      </div>
                      <ng-template #noChecker><span class="text-muted italic small">Chưa phân công</span></ng-template>
                    </div>

                    <div class="mb-0 border-top pt-3">
                      <div class="d-flex justify-content-between mb-2">
                        <span class="text-muted small">Ngày tạo:</span>
                        <span class="small">{{ ticket.createdAt | date:'dd/MM/yyyy HH:mm' }}</span>
                      </div>
                      <div class="d-flex justify-content-between">
                        <span class="text-muted small">Cập nhật lúc:</span>
                        <span class="small">{{ ticket.updatedAt | date:'dd/MM/yyyy HH:mm' }}</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .ls-1 { letter-spacing: 0.05rem; }
    .bg-light-danger { background-color: #fff5f5; border: 1px solid #feb2b2; }
    .avatar-sm {
      width: 28px;
      height: 28px;
      background: #e2e8f0;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 0.8rem;
      font-weight: bold;
      color: #475569;
    }
    .badge { font-size: 0.85rem; padding: 0.5em 0.8em; }
    .badge-draft { background-color: #f1f5f9; color: #475569; }
    .badge-submitted { background-color: #eff6ff; color: #2563eb; }
    .badge-approved { background-color: #f0fdf4; color: #16a34a; }
    .badge-rejected { background-color: #fef2f2; color: #dc2626; }
    .badge-completed { background-color: #f0f9ff; color: #0284c7; }
  `]
})
export class TicketDetailComponent implements OnInit {
  ticket?: TicketDTO;
  rejectionReason: string = '';
  showRejectForm: boolean = false;
  username: string = '';

  constructor(
    private route: ActivatedRoute,
    public router: Router,
    private ticketService: TicketService,
    private keycloakService: KeycloakService
  ) { }

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.username = this.keycloakService.getUsername();
    this.loadTicket(id);
  }

  loadTicket(id: number): void {
    this.ticketService.getTicketById(id).subscribe({
      next: (data) => this.ticket = data,
      error: (err) => alert('Không thể tải thông tin ticket: ' + err.message)
    });
  }

  canSubmit(): boolean {
    if (!this.ticket) return false;
    return (this.ticket.status === TicketStatus.DRAFT || this.ticket.status === TicketStatus.REJECTED)
      && this.ticket.maker === this.username;
  }

  canApproveOrReject(): boolean {
    if (!this.ticket) return false;
    const isChecker = this.keycloakService.hasRole('checker') || this.keycloakService.hasRole('admin');
    return this.ticket.status === TicketStatus.SUBMITTED && isChecker && this.ticket.maker !== this.username;
  }

  submit(): void {
    if (!this.ticket) return;
    this.ticketService.submitTicket(this.ticket.id).subscribe(() => {
      this.loadTicket(this.ticket!.id);
      alert('Đã gửi ticket để phê duyệt.');
    });
  }

  approve(): void {
    if (!this.ticket) return;
    this.ticketService.approveTicket(this.ticket.id).subscribe(() => {
      this.loadTicket(this.ticket!.id);
      alert('Ticket đã được phê duyệt thành công.');
    });
  }

  reject(): void {
    if (!this.ticket || !this.rejectionReason) return;
    this.ticketService.rejectTicket(this.ticket.id, this.rejectionReason).subscribe(() => {
      this.showRejectForm = false;
      this.rejectionReason = '';
      this.loadTicket(this.ticket!.id);
      alert('Ticket đã bị từ chối.');
    });
  }

  getStatusClass(status: TicketStatus): string {
    return 'badge-' + status.toLowerCase();
  }

  getStatusLabel(status: TicketStatus): string {
    const labels: any = {
      'DRAFT': 'Bản nháp',
      'SUBMITTED': 'Chờ duyệt',
      'APPROVED': 'Đã duyệt',
      'REJECTED': 'Bị từ chối',
      'COMPLETED': 'Hoàn tất'
    };
    return labels[status] || status;
  }
}
