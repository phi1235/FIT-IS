import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { TicketService, TicketDTO, TicketStatus } from '../services/ticket.service';
import { KeycloakService } from '../services/keycloak.service';

@Component({
  selector: 'app-ticket-list',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="p-4">
      <div class="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h2 class="fw-bold mb-1">{{ isAdminView() ? 'Quản lý Tickets' : 'Yêu cầu của tôi' }}</h2>
          <p class="text-muted small mb-0">{{ isAdminView() ? 'Theo dõi và xử lý các yêu cầu nghiệp vụ' : 'Danh sách các yêu cầu bạn đã gửi hệ thống' }}</p>
        </div>
        <div class="d-flex gap-2">
          <div class="dropdown" *ngIf="isAdminView()">
            <button class="btn btn-outline-secondary dropdown-toggle" type="button" data-bs-toggle="dropdown">
              Xuất báo cáo
            </button>
            <ul class="dropdown-menu">
              <li><a class="dropdown-item" (click)="downloadReport('pdf')">Bản PDF</a></li>
              <li><a class="dropdown-item" (click)="downloadReport('xlsx')">Bản Excel</a></li>
            </ul>
          </div>
          <a *ngIf="canCreate()" routerLink="create" class="btn btn-primary d-flex align-items-center gap-2">
            <span>+</span> Tạo mới Ticket
          </a>
        </div>
      </div>

      <div class="card border-0 shadow-sm">
        <div class="card-header bg-white py-3">
          <ul class="nav nav-pills card-header-pills">
            <li class="nav-item">
              <button class="nav-link" [class.active]="selectedStatus === 'ALL'" (click)="filterStatus('ALL')">Tất cả</button>
            </li>
            <li class="nav-item">
              <button class="nav-link" [class.active]="selectedStatus === 'DRAFT'" (click)="filterStatus('DRAFT')">Bản nháp</button>
            </li>
            <li class="nav-item">
              <button class="nav-link" [class.active]="selectedStatus === 'SUBMITTED'" (click)="filterStatus('SUBMITTED')">Chờ duyệt</button>
            </li>
            <li class="nav-item">
              <button class="nav-link" [class.active]="selectedStatus === 'APPROVED'" (click)="filterStatus('APPROVED')">Đã duyệt</button>
            </li>
            <li class="nav-item">
              <button class="nav-link" [class.active]="selectedStatus === 'REJECTED'" (click)="filterStatus('REJECTED')">Bị từ chối</button>
            </li>
          </ul>
        </div>
        <div class="card-body p-0">
          <div class="table-responsive">
            <table class="table table-hover mb-0">
              <thead class="bg-light">
                <tr>
                  <th class="ps-4">Mã ID</th>
                  <th>Tiêu đề</th>
                  <th>Trạng thái</th>
                  <th>Số tiền</th>
                  <th>Người tạo</th>
                  <th>Ngày tạo</th>
                  <th class="text-end pe-4">Thao tác</th>
                </tr>
              </thead>
              <tbody>
                <tr *ngFor="let ticket of filteredTickets" class="align-middle">
                  <td class="ps-4"><span class="fw-bold text-primary">#{{ ticket.id }}</span></td>
                  <td>
                    <div class="fw-medium text-truncate" style="max-width: 250px;">{{ ticket.title }}</div>
                  </td>
                  <td>
                    <span [class]="'badge ' + getStatusClass(ticket.status)">{{ getStatusLabel(ticket.status) }}</span>
                  </td>
                  <td>{{ ticket.amount | currency:'VND':'symbol':'1.0-2' }}</td>
                  <td>
                    <div class="d-flex align-items-center gap-2">
                      <div class="avatar-sm">{{ ticket.maker.charAt(0).toUpperCase() }}</div>
                      <span>{{ ticket.maker }}</span>
                    </div>
                  </td>
                  <td>{{ ticket.createdAt | date:'dd/MM/yyyy HH:mm' }}</td>
                  <td class="text-end pe-4">
                    <a [routerLink]="[ticket.id]" class="btn btn-sm btn-light border">Chi tiết</a>
                  </td>
                </tr>
                <tr *ngIf="filteredTickets.length === 0">
                  <td colspan="7" class="text-center py-5 text-muted">
                    <div class="mb-2" style="font-size: 2rem;">📂</div>
                    Không tìm thấy dữ liệu ticket nào
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .nav-link { 
      color: #64748b; 
      font-weight: 500; 
      border-radius: 8px;
      padding: 0.5rem 1rem;
      border: none;
      background: none;
    }
    .nav-link.active { 
      background-color: var(--fis-primary) !important; 
      color: white !important; 
    }
    .avatar-sm {
      width: 24px;
      height: 24px;
      background: #e2e8f0;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 0.7rem;
      font-weight: bold;
      color: #475569;
    }
    .badge-draft { background-color: #f1f5f9; color: #475569; }
    .badge-submitted { background-color: #eff6ff; color: #2563eb; }
    .badge-approved { background-color: #f0fdf4; color: #16a34a; }
    .badge-rejected { background-color: #fef2f2; color: #dc2626; }
    .badge-completed { background-color: #f0f9ff; color: #0284c7; }
  `]
})
export class TicketListComponent implements OnInit {
  tickets: TicketDTO[] = [];
  filteredTickets: TicketDTO[] = [];
  selectedStatus: string = 'ALL';

  constructor(
    private ticketService: TicketService,
    private keycloakService: KeycloakService,
    private router: Router
  ) { }

  ngOnInit(): void {
    this.loadTickets();
  }

  loadTickets(): void {
    this.ticketService.getAllTickets().subscribe(data => {
      this.tickets = data;
      this.filterStatus(this.selectedStatus);
    });
  }

  filterStatus(status: string): void {
    this.selectedStatus = status;
    if (status === 'ALL') {
      this.filteredTickets = this.tickets;
    } else {
      this.filteredTickets = this.tickets.filter(t => t.status === status);
    }
  }

  isAdminView(): boolean {
    return this.router.url.startsWith('/admin');
  }

  canCreate(): boolean {
    // Cho phép tất cả user đã đăng nhập đều có thể tạo ticket
    return this.keycloakService.isAuthenticated();
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

  downloadReport(format: string): void {
    this.ticketService.downloadReport(format);
  }
}
