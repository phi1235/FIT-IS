import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ApprovalRequest, WorkflowService } from '../../services/workflow.service';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-workflow-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './workflow-list.component.html',
  styleUrl: './workflow-list.component.css'
})
export class WorkflowListComponent implements OnInit {
  requests: ApprovalRequest[] = [];
  loading = false;
  currentPage = 0;
  pageSize = 20;
  totalElements = 0;
  totalPages = 0;
  pageSizeOptions = [10, 20, 50];

  filters = {
    status: '',
    requestType: ''
  };

  constructor(
    private workflowService: WorkflowService,
    private router: Router,
    private toastService: ToastService
  ) {}

  ngOnInit(): void {
    this.loadRequests();
  }

  loadRequests(): void {
    this.loading = true;
    this.workflowService.getRequests(
      this.currentPage, this.pageSize,
      this.filters.status || undefined,
      this.filters.requestType || undefined
    ).subscribe({
      next: (page) => {
        this.requests = page.content;
        this.totalElements = page.totalElements;
        this.totalPages = page.totalPages;
        this.loading = false;
      },
      error: (err) => {
        console.error('Failed to load workflow requests', err);
        this.toastService.error('Không thể tải danh sách workflow');
        this.loading = false;
      }
    });
  }

  applyFilters(): void {
    this.currentPage = 0;
    this.loadRequests();
  }

  resetFilters(): void {
    this.filters = { status: '', requestType: '' };
    this.currentPage = 0;
    this.loadRequests();
  }

  onPageChange(page: number): void {
    if (page >= 0 && page < this.totalPages) {
      this.currentPage = page;
      this.loadRequests();
    }
  }

  viewDetail(id: string): void {
    this.router.navigate(['/admin/workflow', id]);
  }

  onPageSizeChange(event: Event): void {
    this.pageSize = Number((event.target as HTMLSelectElement).value);
    this.currentPage = 0;
    this.loadRequests();
  }

  getPageNumbers(): number[] {
    const maxVisible = 5;
    let start = Math.max(0, this.currentPage - Math.floor(maxVisible / 2));
    let end = Math.min(this.totalPages, start + maxVisible);
    if (end - start < maxVisible) start = Math.max(0, end - maxVisible);
    return Array.from({ length: end - start }, (_, i) => start + i);
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'PENDING': return 'status-pending';
      case 'APPROVED':
      case 'COMPLETED': return 'status-completed';
      case 'REJECTED': return 'status-rejected';
      default: return '';
    }
  }
}
