import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AdminService, User, PagedResponse } from '../services/admin.service';
import { TicketService, TicketDTO, TicketStatus } from '../services/ticket.service';
import { KeycloakService } from '../services/keycloak.service';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './admin.component.html',
  styleUrl: './admin.component.css'
})
export class AdminComponent implements OnInit {
  loading = false;
  error: string | null = null;

  // User Stats
  totalUsers = 0;
  adminCount = 0;
  userCount = 0;
  activeCount = 0;

  // Ticket Stats
  totalTickets = 0;
  pendingTickets = 0;
  approvedTickets = 0;
  rejectedTickets = 0;

  isAdmin = false;

  constructor(
    private adminService: AdminService,
    private ticketService: TicketService,
    private keycloakService: KeycloakService,
    private authService: AuthService
  ) { }

  ngOnInit() {
    this.isAdmin = this.authService.isAdmin || this.keycloakService.hasRole('admin');
    this.loadData();
  }

  loadData() {
    this.loading = true;
    this.error = null;

    if (this.isAdmin) {
      this.loadUserStats();
    }
    this.loadTicketStats();
  }

  private loadUserStats() {
    // Load first page just to get total count for stats
    this.adminService.getUsersPaginated(0, 100, '').subscribe({
      next: (response: PagedResponse<User>) => {
        this.totalUsers = response.totalElements;
        // Calculate stats from the sample
        this.adminCount = response.content.filter(u => u.role === 'admin').length;
        this.userCount = response.content.filter(u => u.role === 'user').length;
        this.activeCount = response.content.filter(u => u.enabled).length;
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
        this.pendingTickets = tickets.filter(t => t.status === TicketStatus.SUBMITTED).length;
        this.approvedTickets = tickets.filter(t => t.status === TicketStatus.APPROVED).length;
        this.rejectedTickets = tickets.filter(t => t.status === TicketStatus.REJECTED).length;
        this.loading = false;
      },
      error: (err) => {
        console.error('Failed to load ticket stats:', err);
        this.error = (this.error ? this.error + ' ' : '') + 'Không thể tải thống kê tickets.';
        this.loading = false;
      }
    });
  }

  downloadReport(type: string, format: string) {
    if (type === 'tickets') {
      this.ticketService.downloadReport(format);
    } else {
      const url = `/api/reports/users?format=${format}`;
      window.open(url, '_blank');
    }
  }
}
