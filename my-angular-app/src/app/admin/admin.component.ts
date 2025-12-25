import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService, User } from '../services/admin.service';
import { TicketService, TicketDTO, TicketStatus } from '../services/ticket.service';
import { KeycloakService } from '../services/keycloak.service';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin.component.html',
  styleUrl: './admin.component.css'
})
export class AdminComponent implements OnInit {
  users: User[] = [];
  tickets: TicketDTO[] = [];
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

  // Form để cập nhật role
  selectedUser: User | null = null;
  newRole: string = 'user';
  showRoleModal = false;
  isAdmin = false;

  constructor(
    private adminService: AdminService,
    private ticketService: TicketService,
    private keycloakService: KeycloakService
  ) { }

  ngOnInit() {
    this.isAdmin = this.keycloakService.hasRole('admin');
    this.loadData();
  }

  loadData() {
    this.loading = true;
    this.error = null;

    if (this.isAdmin) {
      this.adminService.getAllUsers().subscribe({
        next: (users) => {
          this.users = users;
          this.updateUserStats();
          this.loadTickets();
        },
        error: (err) => {
          console.error('Failed to load users:', err);
          this.error = `Không thể tải danh sách users. Lỗi: ${err.status}`;
          this.loadTickets(); // Still try to load tickets
        }
      });
    } else {
      this.loadTickets();
    }
  }

  private loadTickets() {
    this.ticketService.getAllTickets().subscribe({
      next: (tickets) => {
        this.tickets = tickets;
        this.updateTicketStats();
        this.loading = false;
      },
      error: (err) => {
        console.error('Failed to load tickets:', err);
        this.error = (this.error ? this.error + ' ' : '') + 'Không thể tải thống kê tickets.';
        this.loading = false;
      }
    });
  }

  loadUsers() {
    this.loadData();
  }

  openRoleModal(user: User) {
    this.selectedUser = user;
    this.newRole = user.role || 'user';
    this.showRoleModal = true;
  }

  closeRoleModal() {
    this.showRoleModal = false;
    this.selectedUser = null;
  }

  updateUserRole() {
    if (!this.selectedUser) return;

    this.loading = true;
    this.error = null;

    this.adminService.updateUserRole(this.selectedUser.username, this.newRole).subscribe({
      next: (response) => {
        console.log('Role updated:', response);
        if (this.selectedUser) {
          this.selectedUser.role = this.newRole;
        }
        this.closeRoleModal();
        this.loading = false;
        this.loadData();
      },
      error: (err) => {
        console.error('Failed to update role:', err);
        this.error = 'Không thể cập nhật role. Vui lòng thử lại.';
        this.loading = false;
      }
    });
  }

  private updateUserStats() {
    this.totalUsers = this.users.length;
    this.adminCount = this.users.filter(u => u.role === 'admin').length;
    this.userCount = this.users.filter(u => u.role === 'user').length;
    this.activeCount = this.users.filter(u => u.enabled).length;
  }

  private updateTicketStats() {
    this.totalTickets = this.tickets.length;
    this.pendingTickets = this.tickets.filter(t => t.status === TicketStatus.SUBMITTED).length;
    this.approvedTickets = this.tickets.filter(t => t.status === TicketStatus.APPROVED).length;
    this.rejectedTickets = this.tickets.filter(t => t.status === TicketStatus.REJECTED).length;
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
