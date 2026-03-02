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
<div class="tc-page">

  <!-- Page Header -->
  <div class="tc-header">
    <div class="tc-header-inner">
      <!-- Breadcrumb inside header (maker/checker only) -->
      <nav *ngIf="!router.url.startsWith('/admin')" class="tc-breadcrumb">
        <a routerLink="/home" class="tc-bc-link"><i class="bi bi-house-fill"></i> Trang chủ</a>
        <i class="bi bi-chevron-right tc-bc-sep"></i>
        <a routerLink="/tickets" class="tc-bc-link">Tickets</a>
        <i class="bi bi-chevron-right tc-bc-sep"></i>
        <span class="tc-bc-current">Tạo mới</span>
      </nav>
      <div class="tc-header-title">
        <div class="tc-header-icon"><i class="bi bi-plus-lg"></i></div>
        <div>
          <h1>Tạo Ticket mới</h1>
          <p>Điền thông tin yêu cầu để gửi phê duyệt</p>
        </div>
      </div>
    </div>
  </div>

  <!-- Body -->
  <div class="tc-body">
    <form (ngSubmit)="onSubmit()" #ticketForm="ngForm" class="tc-layout">

      <!-- Left: Form -->
      <div class="tc-form-col">

        <!-- Tiêu đề -->
        <div class="tc-field-group">
          <label class="tc-label">
            <i class="bi bi-card-heading"></i> Tiêu đề yêu cầu <span class="tc-required">*</span>
          </label>
          <input type="text" class="tc-input" name="title"
                 [(ngModel)]="request.title" required #title="ngModel"
                 [class.tc-input-error]="title.invalid && title.touched"
                 placeholder="Ví dụ: Mua sắm máy tính cho phòng IT...">
          <div *ngIf="title.invalid && title.touched" class="tc-error-msg">
            <i class="bi bi-exclamation-circle-fill"></i> Tiêu đề là bắt buộc
          </div>
        </div>

        <!-- Mô tả -->
        <div class="tc-field-group">
          <label class="tc-label">
            <i class="bi bi-text-paragraph"></i> Mô tả chi tiết
          </label>
          <textarea class="tc-input tc-textarea" name="description" rows="6"
                    [(ngModel)]="request.description"
                    placeholder="Mô tả cụ thể về yêu cầu: mục đích, số lượng, thông số kỹ thuật nếu có..."></textarea>
          <div class="tc-char-hint">{{ (request.description?.length || 0) }} ký tự</div>
        </div>

        <!-- Số tiền -->
        <div class="tc-field-group">
          <label class="tc-label">
            <i class="bi bi-cash-stack"></i> Số tiền dự kiến (VND)
          </label>
          <div class="tc-amount-wrap">
            <input type="text" class="tc-input tc-amount-input" name="amountDisplay"
                   [(ngModel)]="amountDisplay"
                   (input)="onAmountInput($event)"
                   (blur)="onAmountBlur()"
                   placeholder="Nhập số tiền..." autocomplete="off" inputmode="numeric">
            <span class="tc-amount-suffix">VND</span>
          </div>
          <div class="tc-amount-preview" *ngIf="request.amount && request.amount > 0">
            {{ amountToWords() }}
          </div>
        </div>

        <!-- Actions -->
        <div class="tc-actions">
          <a [routerLink]="router.url.startsWith('/admin') ? '/admin/tickets' : '/tickets'"
             class="tc-btn tc-btn-ghost">
            <i class="bi bi-x-lg"></i> Hủy bỏ
          </a>
          <button type="button" class="tc-btn tc-btn-draft"
                  [disabled]="ticketForm.invalid || loading"
                  (click)="onSaveDraft()">
            <span *ngIf="loading && isDraft" class="tc-spinner"></span>
            <i *ngIf="!(loading && isDraft)" class="bi bi-floppy"></i>
            {{ loading && isDraft ? 'Đang lưu...' : 'Lưu nháp' }}
          </button>
          <button type="submit" class="tc-btn tc-btn-submit"
                  [disabled]="ticketForm.invalid || loading">
            <span *ngIf="loading && !isDraft" class="tc-spinner tc-spinner-white"></span>
            <i *ngIf="!(loading && !isDraft)" class="bi bi-send-fill"></i>
            {{ loading && !isDraft ? 'Đang gửi...' : 'Gửi phê duyệt' }}
          </button>
        </div>

      </div>

      <!-- Right: Sidebar info -->
      <div class="tc-sidebar-col">

        <div class="tc-info-card">
          <div class="tc-info-title">
            <i class="bi bi-info-circle-fill"></i> Hướng dẫn
          </div>
          <ul class="tc-info-list">
            <li><i class="bi bi-check2"></i> Tiêu đề ngắn gọn, rõ ràng mục đích</li>
            <li><i class="bi bi-check2"></i> Mô tả đầy đủ: số lượng, thông số, lý do</li>
            <li><i class="bi bi-check2"></i> Số tiền là dự kiến, có thể điều chỉnh</li>
          </ul>
        </div>

        <div class="tc-flow-card">
          <div class="tc-info-title">
            <i class="bi bi-diagram-3-fill"></i> Quy trình phê duyệt
          </div>
          <div class="tc-flow">
            <div class="tc-flow-step tc-flow-active">
              <div class="tc-flow-dot"></div>
              <div class="tc-flow-text">
                <strong>Tạo ticket</strong>
                <span>Maker điền thông tin</span>
              </div>
            </div>
            <div class="tc-flow-step">
              <div class="tc-flow-dot"></div>
              <div class="tc-flow-text">
                <strong>Chờ duyệt</strong>
                <span>Checker xem xét</span>
              </div>
            </div>
            <div class="tc-flow-step">
              <div class="tc-flow-dot"></div>
              <div class="tc-flow-text">
                <strong>Phê duyệt</strong>
                <span>Hoàn tất quy trình</span>
              </div>
            </div>
          </div>
        </div>

        <div class="tc-draft-card">
          <i class="bi bi-floppy-fill"></i>
          <div>
            <strong>Lưu nháp</strong>
            <p>Chưa sẵn sàng? Lưu nháp để chỉnh sửa và gửi sau.</p>
          </div>
        </div>

      </div>
    </form>
  </div>
</div>
  `,
  styles: [`
    /* ===== Page Layout ===== */
    .tc-page { min-height: calc(100vh - 68px); background: #f8fafc; }

    .tc-header {
      background: linear-gradient(135deg, #b94500 0%, #d45f02 60%, #e87c10 100%);
      padding: 24px 0 28px;
    }
    .tc-header-inner {
      max-width: 1100px;
      margin: 0 auto;
      padding: 0 32px;
    }
    .tc-breadcrumb {
      display: flex; align-items: center; gap: 6px;
      margin-bottom: 14px; font-size: 0.8rem;
    }
    .tc-bc-link {
      display: inline-flex; align-items: center; gap: 4px;
      color: rgba(255,255,255,0.75) !important; text-decoration: none !important;
      font-weight: 500; transition: color 0.15s;
    }
    .tc-bc-link:hover { color: #fff !important; }
    .tc-bc-sep { color: rgba(255,255,255,0.4); font-size: 0.68rem; }
    .tc-bc-current { color: rgba(255,255,255,0.95); font-weight: 600; }
    .tc-header-title {
      display: flex;
      align-items: center;
      gap: 16px;
    }
    .tc-header-icon {
      width: 48px; height: 48px;
      background: rgba(255,255,255,0.2);
      border-radius: 12px;
      display: flex; align-items: center; justify-content: center;
      font-size: 1.4rem;
      color: #fff;
      flex-shrink: 0;
    }
    .tc-header-title h1 {
      margin: 0;
      font-size: 1.4rem;
      font-weight: 700;
      color: #fff;
    }
    .tc-header-title p {
      margin: 2px 0 0;
      font-size: 0.85rem;
      color: rgba(255,255,255,0.75);
    }

    /* ===== Body ===== */
    .tc-body {
      max-width: 1100px;
      margin: 0 auto;
      padding: 32px 32px 48px;
    }
    .tc-layout {
      display: grid;
      grid-template-columns: 1fr 300px;
      gap: 28px;
      align-items: start;
    }

    /* ===== Form ===== */
    .tc-form-col {
      background: #fff;
      border-radius: 14px;
      border: 1px solid #e8ecf0;
      padding: 32px;
      box-shadow: 0 2px 12px rgba(0,0,0,0.05);
    }
    .tc-field-group { margin-bottom: 24px; }
    .tc-label {
      display: flex;
      align-items: center;
      gap: 7px;
      font-size: 0.82rem;
      font-weight: 700;
      color: #64748b;
      text-transform: uppercase;
      letter-spacing: 0.05em;
      margin-bottom: 8px;
    }
    .tc-label i { color: #d45f02; }
    .tc-required { color: #ef4444; }

    .tc-input {
      width: 100%;
      padding: 12px 16px;
      border: 1.5px solid #e2e8f0;
      border-radius: 10px;
      font-size: 0.95rem;
      color: #1e293b;
      background: #f8fafc;
      transition: all 0.2s;
      outline: none;
      box-sizing: border-box;
    }
    .tc-input:focus {
      border-color: #d45f02;
      background: #fff;
      box-shadow: 0 0 0 3px rgba(212,95,2,0.1);
    }
    .tc-input-error { border-color: #ef4444 !important; }
    .tc-textarea { resize: vertical; min-height: 140px; line-height: 1.6; }
    .tc-char-hint { font-size: 0.75rem; color: #94a3b8; text-align: right; margin-top: 4px; }
    .tc-error-msg {
      font-size: 0.8rem;
      color: #ef4444;
      margin-top: 6px;
      display: flex;
      align-items: center;
      gap: 5px;
    }

    .tc-amount-wrap {
      display: flex;
      align-items: center;
      border: 1.5px solid #e2e8f0;
      border-radius: 10px;
      background: #f8fafc;
      overflow: hidden;
      transition: all 0.2s;
    }
    .tc-amount-wrap:focus-within {
      border-color: #d45f02;
      background: #fff;
      box-shadow: 0 0 0 3px rgba(212,95,2,0.1);
    }
    .tc-amount-input {
      border: none !important;
      border-radius: 0 !important;
      box-shadow: none !important;
      background: transparent !important;
      flex: 1;
    }
    .tc-amount-suffix {
      padding: 12px 14px;
      font-weight: 700;
      color: #d45f02;
      font-size: 0.85rem;
      border-left: 1.5px solid #e2e8f0;
      background: #fff7f0;
      white-space: nowrap;
    }
    .tc-amount-preview {
      font-size: 0.8rem;
      color: #d45f02;
      font-weight: 600;
      margin-top: 6px;
      padding-left: 4px;
    }

    /* ===== Buttons ===== */
    .tc-actions {
      display: flex;
      gap: 12px;
      padding-top: 8px;
      border-top: 1px solid #f1f5f9;
      margin-top: 8px;
    }
    .tc-btn {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      padding: 11px 20px;
      border-radius: 10px;
      font-size: 0.9rem;
      font-weight: 600;
      cursor: pointer;
      border: none;
      transition: all 0.2s;
      text-decoration: none;
      white-space: nowrap;
    }
    .tc-btn:disabled { opacity: 0.55; cursor: not-allowed; }
    .tc-btn-ghost {
      background: #f1f5f9;
      color: #64748b;
      border: 1.5px solid #e2e8f0;
    }
    .tc-btn-ghost:hover { background: #e8ecf0; color: #475569; }
    .tc-btn-draft {
      background: #fff;
      color: #64748b;
      border: 1.5px solid #d1d5db;
    }
    .tc-btn-draft:hover:not(:disabled) { background: #f8fafc; border-color: #9ca3af; color: #374151; }
    .tc-btn-submit {
      flex: 1;
      background: linear-gradient(135deg, #d45f02, #e87c10);
      color: #fff;
      box-shadow: 0 3px 10px rgba(212,95,2,0.35);
      justify-content: center;
    }
    .tc-btn-submit:hover:not(:disabled) {
      background: linear-gradient(135deg, #b94500, #d45f02);
      transform: translateY(-1px);
      box-shadow: 0 5px 14px rgba(212,95,2,0.45);
    }
    .tc-spinner {
      width: 16px; height: 16px;
      border: 2px solid rgba(0,0,0,0.2);
      border-top-color: #64748b;
      border-radius: 50%;
      animation: spin 0.7s linear infinite;
      display: inline-block;
    }
    .tc-spinner-white { border-color: rgba(255,255,255,0.3); border-top-color: #fff; }
    @keyframes spin { to { transform: rotate(360deg); } }

    /* ===== Sidebar ===== */
    .tc-sidebar-col { display: flex; flex-direction: column; gap: 16px; }

    .tc-info-card, .tc-flow-card, .tc-draft-card {
      background: #fff;
      border-radius: 12px;
      border: 1px solid #e8ecf0;
      padding: 20px;
      box-shadow: 0 1px 6px rgba(0,0,0,0.04);
    }
    .tc-info-title {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 0.85rem;
      font-weight: 700;
      color: #d45f02;
      margin-bottom: 14px;
    }
    .tc-info-list { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 9px; }
    .tc-info-list li {
      display: flex; align-items: flex-start; gap: 8px;
      font-size: 0.83rem; color: #475569; line-height: 1.4;
    }
    .tc-info-list li i { color: #22c55e; flex-shrink: 0; margin-top: 1px; }

    /* Flow */
    .tc-flow { display: flex; flex-direction: column; gap: 0; }
    .tc-flow-step {
      display: flex; align-items: flex-start; gap: 12px;
      padding-bottom: 16px;
      position: relative;
    }
    .tc-flow-step:not(:last-child)::after {
      content: '';
      position: absolute;
      left: 7px; top: 18px;
      width: 2px; bottom: 0;
      background: #e2e8f0;
    }
    .tc-flow-dot {
      width: 16px; height: 16px;
      border-radius: 50%;
      border: 2px solid #d1d5db;
      background: #fff;
      flex-shrink: 0;
      margin-top: 2px;
      position: relative;
      z-index: 1;
    }
    .tc-flow-active .tc-flow-dot {
      border-color: #d45f02;
      background: #d45f02;
    }
    .tc-flow-text { display: flex; flex-direction: column; }
    .tc-flow-text strong { font-size: 0.85rem; color: #1e293b; }
    .tc-flow-text span { font-size: 0.78rem; color: #94a3b8; }

    /* Draft card */
    .tc-draft-card {
      display: flex; align-items: flex-start; gap: 12px;
      background: #fff7f0;
      border-color: #fed7aa;
    }
    .tc-draft-card > i { color: #d45f02; font-size: 1.2rem; flex-shrink: 0; margin-top: 2px; }
    .tc-draft-card strong { font-size: 0.85rem; color: #92400e; display: block; margin-bottom: 3px; }
    .tc-draft-card p { font-size: 0.8rem; color: #b45309; margin: 0; line-height: 1.4; }

    @media (max-width: 900px) {
      .tc-layout { grid-template-columns: 1fr; }
      .tc-sidebar-col { order: -1; }
      .tc-body { padding: 20px 16px 40px; }
    }
  `]
})
export class TicketCreateComponent {
  request: TicketRequest = {
    title: '',
    description: '',
    amount: undefined
  };
  loading = false;
  isDraft = false;
  amountDisplay = '';

  onAmountInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    // Chỉ giữ chữ số
    const digits = input.value.replace(/\D/g, '');
    // Format với dấu chấm phân cách hàng nghìn
    const formatted = digits ? Number(digits).toLocaleString('de-DE') : '';
    this.amountDisplay = formatted;
    this.request.amount = digits ? Number(digits) : undefined;
    // Đặt lại cursor về cuối
    input.value = formatted;
  }

  onAmountBlur(): void {
    if (!this.request.amount) {
      this.amountDisplay = '';
    }
  }

  amountToWords(): string {
    const n = this.request.amount;
    if (!n || n <= 0) return '';

    const units = ['', 'một', 'hai', 'ba', 'bốn', 'năm', 'sáu', 'bảy', 'tám', 'chín'];

    const readGroup = (num: number, hasPrefix: boolean): string => {
      const h = Math.floor(num / 100);
      const t = Math.floor((num % 100) / 10);
      const u = num % 10;
      let s = '';

      if (h > 0) s += units[h] + ' trăm ';
      else if (hasPrefix && num < 100) s += 'không trăm ';

      if (t === 0) {
        if (u > 0) s += (h > 0 || hasPrefix ? 'lẻ ' : '') + units[u];
      } else if (t === 1) {
        s += 'mười' + (u > 0 ? ' ' + (u === 5 ? 'lăm' : units[u]) : '');
      } else {
        s += units[t] + ' mươi';
        if (u === 1) s += ' mốt';
        else if (u === 5) s += ' lăm';
        else if (u > 0) s += ' ' + units[u];
      }
      return s.trim();
    };

    const ty    = Math.floor(n / 1_000_000_000);
    const trieu = Math.floor((n % 1_000_000_000) / 1_000_000);
    const nghin = Math.floor((n % 1_000_000) / 1_000);
    const don   = n % 1_000;

    const parts: string[] = [];
    if (ty    > 0) parts.push(readGroup(ty,    false)          + ' tỷ');
    if (trieu > 0) parts.push(readGroup(trieu, parts.length > 0) + ' triệu');
    if (nghin > 0) parts.push(readGroup(nghin, parts.length > 0) + ' nghìn');
    if (don   > 0) parts.push(readGroup(don,   parts.length > 0));

    if (!parts.length) return '';
    const text = parts.join(' ');
    return text.charAt(0).toUpperCase() + text.slice(1) + ' đồng';
  }

  constructor(
    private ticketService: TicketService,
    public router: Router
  ) { }

  onSubmit(): void {
    this.isDraft = false;
    this.submit(false);
  }

  onSaveDraft(): void {
    this.isDraft = true;
    this.submit(true);
  }

  private submit(saveDraft: boolean): void {
    this.loading = true;
    this.ticketService.createTicket({ ...this.request, saveDraft }).subscribe({
      next: (ticket) => {
        this.loading = false;
        const basePath = this.router.url.startsWith('/admin') ? '/admin/tickets' : '/tickets';
        this.router.navigate([basePath, ticket.id]);
      },
      error: (err) => {
        this.loading = false;
        alert('Có lỗi xảy ra khi tạo ticket: ' + (err.error?.message || err.message));
      }
    });
  }
}
