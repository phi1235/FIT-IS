import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TicketService, TicketDTO, TicketStatus, TicketRequest } from '../services/ticket.service';
import { AuthService } from '../services/auth.service';
import { ToastService } from '../services/toast.service';

@Component({
  selector: 'app-ticket-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  template: `
<div class="td-page" *ngIf="ticket">

  <!-- Header -->
  <div class="td-header">
    <div class="td-header-inner">
      <a [routerLink]="router.url.startsWith('/admin') ? '/admin/tickets' : '/tickets'" class="td-back">
        <i class="bi bi-arrow-left"></i> Quay lại
      </a>
      <div class="td-header-main">
        <div class="td-header-left">
          <div class="td-ticket-icon"><i class="bi bi-file-earmark-text-fill"></i></div>
          <div>
            <h1 *ngIf="!canEdit()">{{ ticket.title }}</h1>
            <input *ngIf="canEdit()" type="text" class="td-title-input"
                   [(ngModel)]="editTitle" placeholder="Tiêu đề ticket">
            <div class="td-meta">
              <span><i class="bi bi-hash"></i>{{ ticket.code }}</span>
              <span class="td-sep">·</span>
              <span><i class="bi bi-person-fill"></i>{{ ticket.makerName }}</span>
              <span class="td-sep">·</span>
              <span><i class="bi bi-calendar3"></i>{{ ticket.createdAt | date:'dd/MM/yyyy HH:mm' }}</span>
            </div>
          </div>
        </div>
        <div class="td-header-actions">
          <span class="td-badge" [ngClass]="'td-badge-' + ticket.status.toLowerCase()">
            <i [class]="'bi ' + getStatusIcon(ticket.status)"></i>
            {{ getStatusLabel(ticket.status) }}
          </span>
          <ng-container *ngIf="canEdit()">
            <button (click)="saveChanges()" class="td-btn td-btn-warn" [disabled]="saving">
              <span *ngIf="saving" class="td-spin"></span>
              <i *ngIf="!saving" class="bi bi-floppy-fill"></i>
              Lưu thay đổi
            </button>
            <button (click)="submit()" class="td-btn td-btn-primary" [disabled]="saving">
              <i class="bi bi-send-fill"></i> Gửi duyệt
            </button>
          </ng-container>
          <ng-container *ngIf="canApproveOrReject()">
            <button (click)="approve()" class="td-btn td-btn-success">
              <i class="bi bi-check-circle-fill"></i> Phê duyệt
            </button>
            <button (click)="showRejectForm = !showRejectForm" class="td-btn td-btn-danger">
              <i class="bi bi-x-circle-fill"></i> Từ chối
            </button>
          </ng-container>
        </div>
      </div>
    </div>
  </div>

  <!-- Body -->
  <div class="td-body">

    <!-- Rejection banner -->
    <div *ngIf="ticket.rejectionReason" class="td-rejection-banner">
      <i class="bi bi-exclamation-triangle-fill"></i>
      <div>
        <strong>Lý do từ chối:</strong> {{ ticket.rejectionReason }}
        <span *ngIf="canEdit()"> — Bạn có thể chỉnh sửa và gửi lại.</span>
      </div>
    </div>

    <div class="td-layout">

      <!-- Left column -->
      <div class="td-main-col">

        <!-- Mô tả -->
        <div class="td-card">
          <div class="td-card-title"><i class="bi bi-text-paragraph"></i> Mô tả yêu cầu</div>
          <p *ngIf="!canEdit()" class="td-desc">{{ ticket.description || 'Không có mô tả.' }}</p>
          <textarea *ngIf="canEdit()" class="td-textarea" rows="6"
                    [(ngModel)]="editDescription"
                    placeholder="Mô tả chi tiết về yêu cầu..."></textarea>
        </div>

        <!-- Workflow diagram -->
        <div class="td-card td-workflow-card">
          <div class="td-card-title"><i class="bi bi-diagram-3-fill"></i> Sơ đồ quy trình</div>
          <div class="td-workflow">
            <ng-container *ngFor="let step of workflowSteps; let last = last">
              <div class="td-wf-step" [ngClass]="{
                'td-wf-active':   step.status === ticket.status,
                'td-wf-done':     isStepDone(step.status),
                'td-wf-rejected': step.status === 'REJECTED' && ticket.status === 'REJECTED'
              }">
                <div class="td-wf-icon-wrap">
                  <i [class]="'bi ' + step.icon"></i>
                </div>
                <div class="td-wf-label">{{ step.label }}</div>
                <div class="td-wf-sub">{{ step.sub }}</div>
                <div class="td-wf-who" *ngIf="step.status === 'DRAFT'">{{ ticket.makerName }}</div>
                <div class="td-wf-who" *ngIf="step.status === 'APPROVED' && ticket.checkerName">{{ ticket.checkerName }}</div>
                <div class="td-wf-who" *ngIf="step.status === 'REJECTED' && ticket.checkerName">{{ ticket.checkerName }}</div>
              </div>
              <div *ngIf="!last" class="td-wf-arrow"
                   [class.td-wf-arrow-done]="isStepDone(step.status) || ticket.status === step.status">
                <i class="bi bi-chevron-right"></i>
              </div>
            </ng-container>
          </div>
        </div>


      </div>

      <!-- Right sidebar -->
      <div class="td-sidebar-col">

        <!-- Số tiền -->
        <div class="td-card td-amount-card">
          <div class="td-amount-label">Số tiền yêu cầu</div>
          <div *ngIf="!canEdit()" class="td-amount-value">
            {{ ticket.amount | number:'1.0-0' }} <span>VND</span>
          </div>
          <div *ngIf="canEdit()" class="td-amount-edit">
            <input type="number" class="td-input" [(ngModel)]="editAmount" placeholder="0">
            <span class="td-amount-edit-unit">VND</span>
          </div>
        </div>

        <!-- Thông tin -->
        <div class="td-card">
          <div class="td-card-title"><i class="bi bi-person-lines-fill"></i> Thông tin</div>
          <div class="td-info-row">
            <div class="td-info-label">Mã ticket</div>
            <div class="td-info-val td-code">{{ ticket.code }}</div>
          </div>
          <div class="td-info-row">
            <div class="td-info-label">Người lập</div>
            <div class="td-info-val"><i class="bi bi-person-fill td-icon-orange"></i> {{ ticket.makerName }}</div>
          </div>
          <div class="td-info-row">
            <div class="td-info-label">Ngày tạo</div>
            <div class="td-info-val">{{ ticket.createdAt | date:'dd/MM/yyyy HH:mm' }}</div>
          </div>
          <div class="td-info-row">
            <div class="td-info-label">Người duyệt</div>
            <div class="td-info-val">
              <span *ngIf="ticket.checkerName"><i class="bi bi-person-check-fill td-icon-green"></i> {{ ticket.checkerName }}</span>
              <span *ngIf="!ticket.checkerName" class="td-empty">Chưa có</span>
            </div>
          </div>
          <div class="td-info-row">
            <div class="td-info-label">Cập nhật</div>
            <div class="td-info-val">{{ ticket.updatedAt | date:'dd/MM/yyyy HH:mm' }}</div>
          </div>
        </div>

      </div>
    </div>
  </div>
</div>

<!-- Reject Modal Overlay -->
<div *ngIf="showRejectForm" class="reject-overlay" (click)="showRejectForm = false">
  <div class="reject-modal" (click)="$event.stopPropagation()">
    <div class="reject-modal-header">
      <div class="reject-modal-title">
        <i class="bi bi-x-circle-fill"></i>
        Từ chối ticket
      </div>
      <button class="reject-modal-close" (click)="showRejectForm = false">
        <i class="bi bi-x-lg"></i>
      </button>
    </div>
    <div class="reject-modal-body">
      <p class="reject-modal-hint">Vui lòng nhập lý do từ chối để maker có thể chỉnh sửa lại.</p>
      <textarea class="td-textarea" rows="4" [(ngModel)]="rejectionReason"
                placeholder="Mô tả lý do từ chối cụ thể..."
                autofocus></textarea>
    </div>
    <div class="reject-modal-footer">
      <button (click)="showRejectForm = false" class="td-btn td-btn-ghost">
        <i class="bi bi-arrow-left"></i> Hủy
      </button>
      <button (click)="reject()" class="td-btn td-btn-danger-solid"
              [disabled]="!rejectionReason || !rejectionReason.trim()">
        <i class="bi bi-x-circle-fill"></i> Xác nhận từ chối
      </button>
    </div>
  </div>
</div>
  `,
  styles: [`
    .td-page { min-height: calc(100vh - 68px); background: #f8fafc; }

    /* Header */
    .td-header {
      background: linear-gradient(135deg, #b94500 0%, #d45f02 60%, #e87c10 100%);
      padding: 20px 0 24px;
    }
    .td-header-inner { max-width: 1100px; margin: 0 auto; padding: 0 32px; }
    .td-back {
      color: rgba(255,255,255,0.75); text-decoration: none;
      font-size: 0.83rem; display: inline-flex; align-items: center;
      gap: 6px; margin-bottom: 12px; transition: color 0.2s;
    }
    .td-back:hover { color: #fff; }
    .td-header-main { display: flex; justify-content: space-between; align-items: flex-start; gap: 20px; flex-wrap: wrap; }
    .td-header-left { display: flex; align-items: flex-start; gap: 14px; }
    .td-ticket-icon {
      width: 44px; height: 44px; background: rgba(255,255,255,0.2);
      border-radius: 10px; display: flex; align-items: center; justify-content: center;
      font-size: 1.3rem; color: #fff; flex-shrink: 0;
    }
    .td-header-left h1 { margin: 0; font-size: 1.25rem; font-weight: 700; color: #fff; line-height: 1.3; }
    .td-title-input {
      background: rgba(255,255,255,0.15); border: 1.5px solid rgba(255,255,255,0.3);
      border-radius: 8px; color: #fff; font-size: 1.1rem; font-weight: 700;
      padding: 6px 12px; outline: none; width: 100%;
    }
    .td-title-input::placeholder { color: rgba(255,255,255,0.5); }
    .td-meta { display: flex; align-items: center; gap: 6px; margin-top: 5px; font-size: 0.8rem; color: rgba(255,255,255,0.75); flex-wrap: wrap; }
    .td-meta i { font-size: 0.75rem; margin-right: 2px; }
    .td-sep { opacity: 0.4; }
    .td-header-actions { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }

    /* Badges */
    .td-badge { display: inline-flex; align-items: center; gap: 5px; padding: 5px 12px; border-radius: 20px; font-size: 0.8rem; font-weight: 700; }
    .td-badge i { font-size: 0.75rem; }
    .td-badge-draft    { background: #f1f5f9; color: #475569; }
    .td-badge-pending  { background: #fef3c7; color: #92400e; }
    .td-badge-approved { background: #dcfce7; color: #166534; }
    .td-badge-rejected { background: #fee2e2; color: #991b1b; }
    .td-badge-completed{ background: #dbeafe; color: #1e40af; }

    /* Buttons */
    .td-btn { display: inline-flex; align-items: center; gap: 6px; padding: 8px 16px; border-radius: 8px; font-size: 0.85rem; font-weight: 600; border: none; cursor: pointer; transition: all 0.2s; white-space: nowrap; }
    .td-btn:disabled { opacity: 0.5; cursor: not-allowed; }
    .td-btn-primary { background: rgba(255,255,255,0.2); color: #fff; border: 1px solid rgba(255,255,255,0.35); }
    .td-btn-primary:hover:not(:disabled) { background: rgba(255,255,255,0.3); }
    .td-btn-warn    { background: #fef3c7; color: #92400e; }
    .td-btn-warn:hover:not(:disabled) { background: #fde68a; }
    .td-btn-success { background: #dcfce7; color: #166534; }
    .td-btn-success:hover:not(:disabled) { background: #bbf7d0; }
    .td-btn-danger  { background: #fee2e2; color: #991b1b; }
    .td-btn-danger:hover:not(:disabled) { background: #fecaca; }
    .td-btn-ghost   { background: #f1f5f9; color: #64748b; }
    .td-btn-ghost:hover:not(:disabled) { background: #e2e8f0; }
    .td-spin { width: 14px; height: 14px; border: 2px solid #f59e0b; border-top-color: transparent; border-radius: 50%; animation: tdspin 0.7s linear infinite; }
    @keyframes tdspin { to { transform: rotate(360deg); } }

    /* Body */
    .td-body { max-width: 1100px; margin: 0 auto; padding: 28px 32px 48px; }
    .td-rejection-banner {
      display: flex; align-items: flex-start; gap: 10px;
      background: #fef2f2; border: 1px solid #fecaca; border-radius: 10px;
      padding: 14px 18px; margin-bottom: 20px; font-size: 0.88rem; color: #991b1b;
    }
    .td-rejection-banner i { font-size: 1rem; flex-shrink: 0; margin-top: 1px; }

    .td-layout { display: grid; grid-template-columns: 1fr 280px; gap: 24px; align-items: start; }

    /* Cards */
    .td-card { background: #fff; border-radius: 14px; border: 1px solid #e8ecf0; padding: 24px; box-shadow: 0 1px 8px rgba(0,0,0,0.05); margin-bottom: 20px; }
    .td-card:last-child { margin-bottom: 0; }
    .td-card-title { display: flex; align-items: center; gap: 8px; font-size: 0.8rem; font-weight: 700; color: #d45f02; text-transform: uppercase; letter-spacing: 0.06em; margin-bottom: 16px; }
    .td-desc { white-space: pre-wrap; line-height: 1.7; color: #374151; min-height: 80px; margin: 0; }
    .td-textarea { width: 100%; border: 1.5px solid #e2e8f0; border-radius: 10px; padding: 12px; font-size: 0.92rem; line-height: 1.6; resize: vertical; outline: none; box-sizing: border-box; }
    .td-textarea:focus { border-color: #d45f02; box-shadow: 0 0 0 3px rgba(212,95,2,0.1); }
    .td-input { width: 100%; border: 1.5px solid #e2e8f0; border-radius: 8px; padding: 10px 12px; font-size: 0.92rem; outline: none; box-sizing: border-box; }
    .td-input:focus { border-color: #d45f02; }

    /* Workflow */
    .td-workflow-card { }
    .td-workflow { display: flex; align-items: flex-start; justify-content: center; gap: 0; flex-wrap: wrap; }
    .td-wf-step {
      display: flex; flex-direction: column; align-items: center; gap: 6px;
      width: 110px; text-align: center; padding: 16px 8px;
      border-radius: 12px; transition: all 0.2s;
    }
    .td-wf-icon-wrap {
      width: 44px; height: 44px; border-radius: 50%;
      background: #f1f5f9; color: #94a3b8;
      display: flex; align-items: center; justify-content: center; font-size: 1.1rem;
      border: 2px solid #e2e8f0; transition: all 0.3s;
    }
    .td-wf-label { font-size: 0.82rem; font-weight: 700; color: #94a3b8; }
    .td-wf-sub   { font-size: 0.72rem; color: #cbd5e1; }
    .td-wf-who   { font-size: 0.72rem; color: #d45f02; font-weight: 600; margin-top: 2px; }

    /* Active step */
    .td-wf-active .td-wf-icon-wrap { background: #fff7ed; color: #d45f02; border-color: #d45f02; box-shadow: 0 0 0 4px rgba(212,95,2,0.12); }
    .td-wf-active .td-wf-label { color: #d45f02; }
    .td-wf-active .td-wf-sub   { color: #fb923c; }
    .td-wf-active { background: #fff7ed; }

    /* Done step */
    .td-wf-done .td-wf-icon-wrap { background: #dcfce7; color: #16a34a; border-color: #16a34a; }
    .td-wf-done .td-wf-label { color: #16a34a; }

    /* Rejected step */
    .td-wf-rejected .td-wf-icon-wrap { background: #fee2e2; color: #dc2626; border-color: #dc2626; box-shadow: 0 0 0 4px rgba(220,38,38,0.1); }
    .td-wf-rejected .td-wf-label { color: #dc2626; }
    .td-wf-rejected { background: #fef2f2; }

    /* Arrow */
    .td-wf-arrow { display: flex; align-items: center; color: #d1d5db; font-size: 1rem; padding: 0 2px; margin-top: 14px; transition: color 0.2s; }
    .td-wf-arrow-done { color: #d45f02; }

    /* Reject form */
    .td-reject-card { border-color: #fecaca; background: #fff5f5; }
    .td-reject-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 12px; }

    /* Amount card */
    .td-amount-card { background: linear-gradient(135deg, #fff7ed, #fff); border-color: #fed7aa; }
    .td-amount-label { font-size: 0.75rem; font-weight: 700; color: #d45f02; text-transform: uppercase; letter-spacing: 0.06em; margin-bottom: 8px; }
    .td-amount-value { font-size: 1.8rem; font-weight: 800; color: #b94500; line-height: 1.2; }
    .td-amount-value span { font-size: 0.9rem; font-weight: 600; color: #d45f02; }
    .td-amount-edit { display: flex; align-items: center; gap: 8px; }
    .td-amount-edit-unit { font-size: 0.85rem; font-weight: 700; color: #d45f02; }

    /* Info rows */
    .td-info-row { display: flex; justify-content: space-between; align-items: center; padding: 8px 0; border-bottom: 1px solid #f1f5f9; font-size: 0.83rem; }
    .td-info-row:last-child { border-bottom: none; }
    .td-info-label { color: #94a3b8; font-weight: 500; }
    .td-info-val { color: #1e293b; font-weight: 600; text-align: right; }
    .td-code { font-family: monospace; font-size: 0.78rem; color: #d45f02; }
    .td-empty { color: #cbd5e1; font-style: italic; font-weight: 400; }
    .td-icon-orange { color: #d45f02; margin-right: 4px; }
    .td-icon-green  { color: #16a34a; margin-right: 4px; }

    /* Reject Modal */
    .reject-overlay {
      position: fixed; inset: 0; background: rgba(0,0,0,0.5);
      display: flex; align-items: center; justify-content: center;
      z-index: 1000; backdrop-filter: blur(2px);
      animation: fadeIn 0.15s ease;
    }
    @keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }

    .reject-modal {
      background: #fff; border-radius: 16px; width: 90%; max-width: 480px;
      box-shadow: 0 20px 60px rgba(0,0,0,0.2);
      animation: slideUp 0.2s ease;
    }
    @keyframes slideUp { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: translateY(0); } }

    .reject-modal-header {
      display: flex; align-items: center; justify-content: space-between;
      padding: 20px 24px 16px;
      border-bottom: 1px solid #fee2e2;
    }
    .reject-modal-title {
      display: flex; align-items: center; gap: 8px;
      font-size: 1rem; font-weight: 700; color: #991b1b;
    }
    .reject-modal-title i { font-size: 1.1rem; }
    .reject-modal-close {
      background: none; border: none; cursor: pointer;
      color: #9ca3af; font-size: 1rem; padding: 4px;
      border-radius: 6px; transition: all 0.2s;
    }
    .reject-modal-close:hover { background: #f3f4f6; color: #374151; }

    .reject-modal-body { padding: 16px 24px; }
    .reject-modal-hint {
      font-size: 0.83rem; color: #6b7280; margin: 0 0 12px;
    }

    .reject-modal-footer {
      display: flex; justify-content: flex-end; gap: 10px;
      padding: 16px 24px; border-top: 1px solid #f3f4f6;
    }

    .td-btn-danger-solid {
      background: #dc2626; color: #fff;
      display: inline-flex; align-items: center; gap: 6px;
      padding: 9px 18px; border-radius: 8px; font-size: 0.88rem;
      font-weight: 600; border: none; cursor: pointer; transition: all 0.2s;
    }
    .td-btn-danger-solid:hover:not(:disabled) { background: #b91c1c; }
    .td-btn-danger-solid:disabled { opacity: 0.45; cursor: not-allowed; }

    @media (max-width: 900px) {
      .td-layout { grid-template-columns: 1fr; }
      .td-body { padding: 20px 16px 40px; }
      .td-header-inner { padding: 0 16px; }
    }
  `]
})
export class TicketDetailComponent implements OnInit {
  ticket?: TicketDTO;
  rejectionReason = '';
  showRejectForm = false;
  saving = false;

  // Edit fields
  editTitle = '';
  editDescription = '';
  editAmount: number = 0;

  private userId = '';

  constructor(
    private route: ActivatedRoute,
    public router: Router,
    private ticketService: TicketService,
    private authService: AuthService,
    private toast: ToastService
  ) { }

  ngOnInit(): void {
    this.userId = this.authService.getUserIdFromToken();
    const id = this.route.snapshot.paramMap.get('id');
    if (id) this.loadTicket(id);
  }

  loadTicket(id: string): void {
    this.ticketService.getTicketById(id).subscribe({
      next: (data) => {
        this.ticket = data;
        this.editTitle = data.title;
        this.editDescription = data.description || '';
        this.editAmount = data.amount || 0;
      },
      error: (err) => this.toast.error('Không thể tải thông tin ticket: ' + err.message)
    });
  }

  canEdit(): boolean {
    if (!this.ticket) return false;
    return (this.ticket.status === TicketStatus.DRAFT || this.ticket.status === TicketStatus.REJECTED)
      && this.ticket.makerUserId === this.userId;
  }

  canApproveOrReject(): boolean {
    if (!this.ticket) return false;
    return this.ticket.status === TicketStatus.PENDING
      && this.authService.hasRole('CHECKER')
      && this.ticket.makerUserId !== this.userId;
  }

  saveChanges(): void {
    if (!this.ticket || !this.editTitle.trim()) return;
    this.saving = true;
    const req: TicketRequest = { title: this.editTitle, description: this.editDescription, amount: this.editAmount };
    this.ticketService.updateTicket(this.ticket.id, req).subscribe({
      next: (updated) => {
        this.saving = false;
        this.ticket = updated;
        this.toast.success('Đã lưu thay đổi thành công.');
      },
      error: (err) => {
        this.saving = false;
        this.toast.error('Lỗi khi lưu: ' + (err.error?.message || err.message));
      }
    });
  }

  submit(): void {
    if (!this.ticket) return;
    this.ticketService.submitTicket(this.ticket.id).subscribe({
      next: () => {
        this.toast.success('Đã gửi ticket để phê duyệt.');
        this.loadTicket(this.ticket!.id);
      },
      error: (err) => this.toast.error('Lỗi: ' + (err.error?.message || err.message))
    });
  }

  approve(): void {
    if (!this.ticket) return;
    this.ticketService.approveTicket(this.ticket.id).subscribe({
      next: () => {
        this.toast.success('Phê duyệt ticket thành công!');
        this.loadTicket(this.ticket!.id);
      },
      error: (err) => this.toast.error('Lỗi phê duyệt: ' + (err.error?.message || err.message))
    });
  }

  reject(): void {
    if (!this.ticket || !this.rejectionReason) return;
    this.ticketService.rejectTicket(this.ticket.id, this.rejectionReason).subscribe({
      next: () => {
        this.toast.success('Đã từ chối ticket.');
        this.showRejectForm = false;
        this.rejectionReason = '';
        this.loadTicket(this.ticket!.id);
      },
      error: (err) => this.toast.error('Lỗi từ chối: ' + (err.error?.message || err.message))
    });
  }

  readonly workflowSteps = [
    { status: 'DRAFT',    label: 'Tạo ticket',  sub: 'Maker soạn',     icon: 'bi-pencil-square' },
    { status: 'PENDING',  label: 'Chờ duyệt',   sub: 'Đang xem xét',   icon: 'bi-hourglass-split' },
    { status: 'APPROVED', label: 'Phê duyệt',   sub: 'Checker duyệt',  icon: 'bi-check-circle-fill' },
    { status: 'REJECTED', label: 'Từ chối',      sub: 'Checker từ chối',icon: 'bi-x-circle-fill' },
  ];

  isStepDone(stepStatus: string): boolean {
    if (!this.ticket) return false;
    const order: Record<string, number> = { DRAFT: 0, PENDING: 1, APPROVED: 2, REJECTED: 2, COMPLETED: 3 };
    return order[this.ticket.status] > order[stepStatus];
  }

  getStatusLabel(status: TicketStatus): string {
    const labels: Record<string, string> = {
      DRAFT: 'Bản nháp', PENDING: 'Chờ duyệt',
      APPROVED: 'Đã duyệt', REJECTED: 'Bị từ chối', COMPLETED: 'Hoàn tất'
    };
    return labels[status] || status;
  }

  getStatusIcon(status: TicketStatus): string {
    const icons: Record<string, string> = {
      DRAFT: 'bi-file-earmark', PENDING: 'bi-hourglass-split',
      APPROVED: 'bi-check-circle-fill', REJECTED: 'bi-x-circle-fill', COMPLETED: 'bi-trophy-fill'
    };
    return icons[status] || 'bi-circle';
  }
}
