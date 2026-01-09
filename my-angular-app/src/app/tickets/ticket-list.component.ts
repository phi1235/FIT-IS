import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TicketService, TicketDTO, TicketStatus, PagedTicketResponse } from '../services/ticket.service';
import { KeycloakService } from '../services/keycloak.service';

@Component({
  selector: 'app-ticket-list',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './ticket-list.component.html',
  styleUrls: ['./ticket-list.component.css']
})
export class TicketListMainComponent implements OnInit {
  tickets: TicketDTO[] = [];
  filteredTickets: TicketDTO[] = [];
  selectedStatus: string = 'ALL';
  loading = false;
  error: string | null = null;

  // Pagination
  currentPage = 0;
  pageSize = 10;
  pageSizeOptions = [10, 20, 50, 100];
  totalElements = 0;
  totalPages = 0;
  searchQuery = '';
  private searchTimeout: any;

  // Export state
  exporting = false;
  exportProgress = 0;
  exportMessage: string | null = null;
  exportError: string | null = null;
  exportFormat: string = '';

  constructor(
    private ticketService: TicketService,
    private keycloakService: KeycloakService,
    private router: Router
  ) { 
    console.log('TicketListComponent initialized');
  }

  ngOnInit(): void {
    this.loadTickets();
  }

  loadTickets(): void {
    this.loading = true;
    this.error = null;
    this.ticketService.getTicketsPaginated(this.currentPage, this.pageSize, this.searchQuery, this.selectedStatus).subscribe({
      next: (response: PagedTicketResponse) => {
        this.tickets = response.content;
        this.filteredTickets = response.content;
        this.totalElements = response.totalElements;
        this.totalPages = response.totalPages;
        this.loading = false;
      },
      error: (err) => {
        console.error('Failed to load tickets:', err);
        this.error = 'Không thể tải danh sách tickets. Vui lòng thử lại.';
        this.loading = false;
      }
    });
  }

  onSearch(event: Event): void {
    const query = (event.target as HTMLInputElement).value;
    this.searchQuery = query;
    clearTimeout(this.searchTimeout);
    this.searchTimeout = setTimeout(() => {
      this.currentPage = 0;
      this.loadTickets();
    }, 300);
  }

  goToPage(page: number): void {
    if (page >= 0 && page < this.totalPages) {
      this.currentPage = page;
      this.loadTickets();
    }
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages - 1) {
      this.currentPage++;
      this.loadTickets();
    }
  }

  previousPage(): void {
    if (this.currentPage > 0) {
      this.currentPage--;
      this.loadTickets();
    }
  }

  onPageSizeChange(event: Event): void {
    this.pageSize = Number((event.target as HTMLSelectElement).value);
    this.currentPage = 0;
    this.loadTickets();
  }

  getPageNumbers(): number[] {
    const pages: number[] = [];
    const maxVisiblePages = 5;
    let start = Math.max(0, this.currentPage - Math.floor(maxVisiblePages / 2));
    let end = Math.min(this.totalPages, start + maxVisiblePages);

    if (end - start < maxVisiblePages) {
      start = Math.max(0, end - maxVisiblePages);
    }

    for (let i = start; i < end; i++) {
      pages.push(i);
    }
    return pages;
  }

  filterStatus(status: string): void {
    this.selectedStatus = status;
    this.currentPage = 0; // Reset to first page when changing status
    this.loadTickets();
  }

  isAdminView(): boolean {
    return this.router.url.startsWith('/admin');
  }

  canCreate(): boolean {
    return this.keycloakService.isAuthenticated();
  }

  getStatusClass(status: TicketStatus): string {
    return 'badge badge-' + status.toLowerCase();
  }

  getStatusLabel(status: TicketStatus): string {
    const labels: any = {
      'DRAFT': 'Bản nháp',
      'PENDING': 'Chờ duyệt',
      'SUBMITTED': 'Đã gửi',
      'APPROVED': 'Đã duyệt',
      'REJECTED': 'Bị từ chối',
      'COMPLETED': 'Hoàn tất'
    };
    return labels[status] || status;
  }

  downloadReport(format: string): void {
    if (this.exporting) return;

    this.exporting = true;
    this.exportProgress = 0;
    this.exportFormat = format;
    this.exportMessage = `Đang khởi tạo xuất báo cáo ${format.toUpperCase()}...`;
    this.exportError = null;

    this.ticketService.generateReport(format).subscribe({
      next: (response) => {
        const jobId = response.jobId;
        this.exportMessage = `Đang xử lý...`;
        this.pollJobStatus(jobId, format);
      },
      error: (err) => {
        console.error('Error starting report:', err);
        this.exportError = `Không thể bắt đầu xuất báo cáo. Lỗi: ${err.status || 'Unknown'}`;
        this.exporting = false;
        this.exportMessage = null;
      }
    });
  }

  private pollJobStatus(jobId: string, format: string): void {
    const pollInterval = setInterval(() => {
      this.ticketService.getReportStatus(jobId).subscribe({
        next: (status) => {
          this.exportProgress = status.progress || 0;
          this.exportMessage = `Đang xử lý... ${status.progress}%`;

          if (status.status === 'COMPLETED') {
            clearInterval(pollInterval);
            this.downloadFile(jobId, format);
          } else if (status.status === 'FAILED') {
            clearInterval(pollInterval);
            this.exportError = `Xuất báo cáo thất bại: ${status.error || 'Unknown error'}`;
            this.exporting = false;
            this.exportMessage = null;
          }
        },
        error: (err) => {
          clearInterval(pollInterval);
          this.exportError = `Lỗi kiểm tra trạng thái: ${err.message}`;
          this.exporting = false;
          this.exportMessage = null;
        }
      });
    }, 2000); // Poll every 2 seconds to avoid 429
  }

  private downloadFile(jobId: string, format: string): void {
    this.exportMessage = 'Đang tải xuống...';

    this.ticketService.downloadReportFile(jobId).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `tickets_report.${format}`;
        link.click();
        window.URL.revokeObjectURL(url);

        this.exporting = false;
        this.exportMessage = null;
        this.exportProgress = 0;
      },
      error: (err) => {
        console.error('Download error:', err);
        this.exportError = 'Không thể tải xuống file báo cáo';
        this.exporting = false;
        this.exportMessage = null;
      }
    });
  }
}
