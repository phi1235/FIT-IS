import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AdminService, User, PagedResponse } from '../services/admin.service';
import { TicketService, TicketDTO, TicketStatus } from '../services/ticket.service';
import { KeycloakService } from '../services/keycloak.service';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './admin.component.html',
  styleUrl: './admin.component.css'
})
export class AdminComponent implements OnInit {
  loading = false;
  error: string | null = null;

  // User Stats — dynamic by role
  totalUsers = 0;
  activeCount = 0;
  roleGroups: { role: string; count: number }[] = [];

  // Ticket Stats
  totalTickets = 0;
  draftTickets = 0;
  pendingTickets = 0;
  approvedTickets = 0;
  rejectedTickets = 0;

  canViewUsers = false;
  canViewTickets = false;

  readonly CIRCUMFERENCE = 2 * Math.PI * 52;

  // Fixed colors for known roles; unknown roles cycle through palette
  private readonly ROLE_COLORS: Record<string, string> = {
    ADMIN:   '#7c3aed',
    CHECKER: '#0891b2',
    MAKER:   '#d97706',
    USER:    '#1d4ed8',
  };
  private readonly COLOR_PALETTE = [
    '#7c3aed', '#0891b2', '#d97706', '#1d4ed8',
    '#e11d48', '#059669', '#ea580c', '#0d9488', '#db2777',
  ];

  private roleColor(role: string, idx: number): string {
    return this.ROLE_COLORS[role.toUpperCase()] ?? this.COLOR_PALETTE[idx % this.COLOR_PALETTE.length];
  }

  private formatRole(role: string): string {
    return role.charAt(0).toUpperCase() + role.slice(1).toLowerCase();
  }

  get ticketSegments() {
    if (this.totalTickets === 0) return [];
    const C = this.CIRCUMFERENCE;
    const items = [
      { label: 'Chờ phê duyệt', count: this.pendingTickets,  color: '#f59e0b' },
      { label: 'Bản nháp',      count: this.draftTickets,    color: '#9ca3af' },
      { label: 'Đã phê duyệt', count: this.approvedTickets, color: '#10b981' },
      { label: 'Đã từ chối',   count: this.rejectedTickets,  color: '#ef4444' },
    ];
    let cumDash = 0;
    return items.map(item => {
      const dash = (item.count / this.totalTickets) * C;
      const result = {
        ...item,
        pct: Math.round((item.count / this.totalTickets) * 100),
        strokeDasharray: `${dash} ${C}`,
        strokeDashoffset: -cumDash,
      };
      cumDash += dash;
      return result;
    });
  }

  // Donut segments — one per role
  get userSegments() {
    if (this.totalUsers === 0) return [];
    const C = this.CIRCUMFERENCE;
    let cumDash = 0;
    return this.roleGroups.map((g, i) => {
      const dash = (g.count / this.totalUsers) * C;
      const result = {
        label: this.formatRole(g.role),
        count: g.count,
        color: this.roleColor(g.role, i),
        pct: Math.round((g.count / this.totalUsers) * 100),
        strokeDasharray: `${dash} ${C}`,
        strokeDashoffset: -cumDash,
      };
      cumDash += dash;
      return result;
    });
  }

  // Progress bars — role rows + active row
  get userStats() {
    if (this.totalUsers === 0) return [];
    const pct = (n: number) => Math.round((n / this.totalUsers) * 100);
    const roleRows = this.roleGroups.map((g, i) => ({
      label: this.formatRole(g.role),
      count: g.count,
      color: this.roleColor(g.role, i),
      pct: pct(g.count),
    }));
    return [
      ...roleRows,
      { label: 'Đang hoạt động', count: this.activeCount, color: '#059669', pct: pct(this.activeCount) },
    ];
  }

  constructor(
    private adminService: AdminService,
    private ticketService: TicketService,
    private keycloakService: KeycloakService,
    private authService: AuthService
  ) { }

  ngOnInit() {
    this.canViewUsers = this.authService.hasPermission('USER_VIEW');
    this.canViewTickets = this.authService.hasPermission('TICKET_VIEW');
    this.loadData();
  }

  loadData() {
    this.loading = true;
    this.error = null;
    if (this.canViewUsers) this.loadUserStats();
    if (this.canViewTickets) this.loadTicketStats();
  }

  private loadUserStats() {
    this.adminService.getUsersPaginated(0, 100, '').subscribe({
      next: (response: PagedResponse<User>) => {
        this.totalUsers = response.totalElements;
        this.activeCount = response.content.filter(u => u.enabled).length;

        // Group dynamically by role
        const roleMap = new Map<string, number>();
        response.content.forEach(u => {
          const r = (u.role ?? 'unknown').toUpperCase();
          roleMap.set(r, (roleMap.get(r) ?? 0) + 1);
        });
        this.roleGroups = Array.from(roleMap.entries())
          .sort((a, b) => b[1] - a[1])   // sort by count desc
          .map(([role, count]) => ({ role, count }));

        this.loading = false;
      },
      error: (err) => {
        console.error('Failed to load user stats:', err);
        this.error = `Không thể tải thống kê users. Lỗi: ${err.status}`;
        this.loading = false;
      }
    });
  }

  private loadTicketStats() {
    this.ticketService.getAllTickets().subscribe({
      next: (tickets: TicketDTO[]) => {
        this.totalTickets = tickets.length;
        this.draftTickets     = tickets.filter(t => t.status === TicketStatus.DRAFT).length;
        this.pendingTickets   = tickets.filter(t => t.status === TicketStatus.PENDING).length;
        this.approvedTickets  = tickets.filter(t => t.status === TicketStatus.APPROVED).length;
        this.rejectedTickets  = tickets.filter(t => t.status === TicketStatus.REJECTED).length;
        this.loading = false;
      },
      error: (err) => {
        console.error('Failed to load ticket stats:', err);
        this.error = (this.error ? this.error + ' ' : '') + 'Không thể tải thống kê tickets.';
        this.loading = false;
      }
    });
  }

  getDisplayName(): string {
    return this.authService.getDisplayName();
  }

  downloadReport(type: string, format: string) {
    if (type === 'tickets') {
      window.location.href = '/admin/tickets';
    } else {
      const url = `/api/reports/users?format=${format}`;
      window.open(url, '_blank');
    }
  }
}
