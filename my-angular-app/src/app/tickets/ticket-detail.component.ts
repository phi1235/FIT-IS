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
    <div class="container-fluid py-4">
      <div *ngIf="ticket" class="row justify-content-center">
        <div class="col-xl-9">
          <!-- Action Bar -->
          <div class="d-flex justify-content-between align-items-center mb-4">
            <a [routerLink]="router.url.startsWith('/admin') ? '/admin/tickets' : '/tickets'" class="btn btn-outline-secondary btn-sm">
              <i class="bi bi-arrow-left"></i> Quay lại danh sách
            </a>
            <div class="d-flex gap-2">
              <button *ngIf="canSubmit()" (click)="submit()" class="btn btn-success btn-sm">Gửi phê duyệt</button>
              <button *ngIf="canApproveOrReject()" (click)="approve()" class="btn btn-primary btn-sm">Phê duyệt</button>
              <button *ngIf="canApproveOrReject()" (click)="showRejectForm = !showRejectForm" class="btn btn-danger btn-sm">Từ chối</button>
            </div>
          </div>

          <!-- Main Info -->
          <div class="card shadow-sm border-0 mb-4">
            <div class="card-header bg-white py-3 d-flex justify-content-between align-items-center">
              <div>
                <h5 class="mb-0 fw-bold">{{ ticket.title }}</h5>
                <span class="text-muted small">Mã: {{ ticket.code }} | ID: #{{ ticket.id.substring(0,8) }}</span>
              </div>
              <span class="badge" [ngClass]="'badge-' + ticket.status.toLowerCase()">{{ getStatusLabel(ticket.status) }}</span>
            </div>
            <div class="card-body">
              <div class="row">
                <div class="col-lg-8">
                  <h6 class="fw-bold mb-3 text-secondary text-uppercase small">Mô tả</h6>
                  <p class="text-dark bg-light p-3 rounded" style="white-space: pre-wrap; min-height: 120px;">
                    {{ ticket.description || 'Không có mô tả.' }}
                  </p>

                  <div *ngIf="ticket.rejectionReason" class="alert alert-danger mt-4">
                    <h6 class="alert-heading fw-bold">Lý do từ chối:</h6>
                    <p class="mb-0">{{ ticket.rejectionReason }}</p>
                  </div>

                  <!-- Reject Form -->
                  <div *ngIf="showRejectForm" class="card border-danger mt-4">
                    <div class="card-body">
                      <h6 class="fw-bold mb-3">Lý do từ chối</h6>
                      <textarea class="form-control mb-3" rows="3" [(ngModel)]="rejectionReason" placeholder="Nhập lý do chi tiết..."></textarea>
                      <div class="d-flex justify-content-end gap-2">
                        <button (click)="showRejectForm = false" class="btn btn-light btn-sm">Hủy</button>
                        <button (click)="reject()" class="btn btn-danger btn-sm" [disabled]="!rejectionReason">Xác nhận từ chối</button>
                      </div>
                    </div>
                  </div>
                </div>

                <div class="col-lg-4 border-start">
                  <div class="ps-lg-3">
                    <div class="mb-4">
                      <h6 class="fw-bold mb-1 text-secondary text-uppercase small">Số tiền yêu cầu</h6>
                      <h3 class="fw-bold text-primary">{{ ticket.amount | currency:'VND':'symbol':'1.0-0' }}</h3>
                    </div>

                    <div class="mb-3">
                      <h6 class="fw-bold mb-1 text-secondary text-uppercase small">Người lập (Maker)</h6>
                      <p class="mb-0 fw-semibold">{{ ticket.makerName }}</p>
                      <span class="text-muted small">Lúc {{ ticket.createdAt | date:'dd/MM/yyyy HH:mm' }}</span>
                    </div>

                    <div class="mb-3">
                      <h6 class="fw-bold mb-1 text-secondary text-uppercase small">Người duyệt (Checker)</h6>
                      <p class="mb-0 fw-semibold" *ngIf="ticket.checkerName; else noChecker">{{ ticket.checkerName }}</p>
                      <ng-template #noChecker><span class="text-muted italic small">Chưa duyệt</span></ng-template>
                    </div>

                    <div class="mt-4 pt-3 border-top">
                      <span class="text-muted small">Cập nhật lúc: {{ ticket.updatedAt | date:'dd/MM/yyyy HH:mm' }}</span>
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
    .badge { padding: 0.5em 1em; font-weight: 700; }
    .badge-draft { background-color: #f8fafc; color: #475569; border: 1px solid #e2e8f0; }
    .badge-pending { background-color: #fffbeb; color: #92400e; border: 1px solid #fde68a; }
    .badge-approved { background-color: #f0fdf4; color: #166534; border: 1px solid #bbf7d0; }
    .badge-rejected { background-color: #fef2f2; color: #991b1b; border: 1px solid #fecaca; }
  `]
})
export class TicketDetailComponent implements OnInit {
  ticket?: TicketDTO;
  rejectionReason: string = '';
  showRejectForm: boolean = false;
  userId: string = '';

  constructor(
    private route: ActivatedRoute,
    public router: Router,
    private ticketService: TicketService,
    private keycloakService: KeycloakService
  ) { }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    const authUser = this.keycloakService.getUserInfo();
    this.userId = authUser?.id || '';
    if (id) {
      this.loadTicket(id);
    }
  }

  loadTicket(id: string): void {
    this.ticketService.getTicketById(id).subscribe({
      next: (data) => this.ticket = data,
      error: (err) => alert('Không thể tải thông tin ticket: ' + err.message)
    });
  }

  canSubmit(): boolean {
    if (!this.ticket) return false;
    return (this.ticket.status === TicketStatus.DRAFT || this.ticket.status === TicketStatus.REJECTED)
      && this.ticket.makerUserId === this.userId;
  }

  canApproveOrReject(): boolean {
    if (!this.ticket) return false;
    const isChecker = this.keycloakService.hasRole('checker') || this.keycloakService.hasRole('admin');
    return this.ticket.status === TicketStatus.PENDING && isChecker && this.ticket.makerUserId !== this.userId;
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
      'PENDING': 'Chờ duyệt',
      'APPROVED': 'Đã duyệt',
      'REJECTED': 'Bị từ chối',
      'CLOSED': 'Hoàn tất'
    };
    return labels[status] || status;
  }
}
