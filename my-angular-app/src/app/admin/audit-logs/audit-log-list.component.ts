import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuditEvent, AuditService } from '../../services/audit.service';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-audit-log-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './audit-log-list.component.html',
  styleUrl: './audit-log-list.component.css'
})
export class AuditLogListComponent implements OnInit {
  events: AuditEvent[] = [];
  loading = false;
  currentPage = 0;
  pageSize = 20;
  totalElements = 0;
  totalPages = 0;
  pageSizeOptions = [10, 20, 50];
  expandedRows = new Set<string>();

  filters = {
    eventType: '',
    username: '',
    fromDate: '',
    toDate: ''
  };

  constructor(
    private auditService: AuditService,
    private toastService: ToastService
  ) {}

  ngOnInit(): void {
    this.loadEvents();
  }

  loadEvents(): void {
    this.loading = true;
    const f = this.filters;
    this.auditService.getEvents(this.currentPage, this.pageSize, {
      eventType: f.eventType || undefined,
      username: f.username || undefined,
      fromDate: f.fromDate ? f.fromDate + 'T00:00:00' : undefined,
      toDate: f.toDate ? f.toDate + 'T23:59:59' : undefined
    }).subscribe({
      next: (page) => {
        this.events = page.content;
        this.totalElements = page.totalElements;
        this.totalPages = page.totalPages;
        this.loading = false;
      },
      error: (err) => {
        console.error('Failed to load audit events', err);
        this.toastService.error('Không thể tải danh sách audit logs');
        this.loading = false;
      }
    });
  }

  applyFilters(): void {
    this.currentPage = 0;
    this.loadEvents();
  }

  resetFilters(): void {
    this.filters = { eventType: '', username: '', fromDate: '', toDate: '' };
    this.currentPage = 0;
    this.loadEvents();
  }

  onPageChange(page: number): void {
    if (page >= 0 && page < this.totalPages) {
      this.currentPage = page;
      this.loadEvents();
    }
  }

  toggleRow(id: string): void {
    if (this.expandedRows.has(id)) {
      this.expandedRows.delete(id);
    } else {
      this.expandedRows.add(id);
    }
  }

  isExpanded(id: string): boolean {
    return this.expandedRows.has(id);
  }

  onPageSizeChange(event: Event): void {
    this.pageSize = Number((event.target as HTMLSelectElement).value);
    this.currentPage = 0;
    this.loadEvents();
  }

  getPageNumbers(): number[] {
    const maxVisible = 5;
    let start = Math.max(0, this.currentPage - Math.floor(maxVisible / 2));
    let end = Math.min(this.totalPages, start + maxVisible);
    if (end - start < maxVisible) start = Math.max(0, end - maxVisible);
    return Array.from({ length: end - start }, (_, i) => start + i);
  }

  formatMetadata(metadata: string | undefined): string {
    if (!metadata) return '';
    try {
      return JSON.stringify(JSON.parse(metadata), null, 2);
    } catch {
      return metadata;
    }
  }

  getStatusClass(code: number | undefined): string {
    if (!code) return '';
    if (code < 300) return 'status-ok';
    if (code < 400) return 'status-redirect';
    if (code < 500) return 'status-client-error';
    return 'status-server-error';
  }
}
