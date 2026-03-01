import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { EmailTemplate, EmailTemplateService, Page } from '../../services/email-template.service';
import { Router } from '@angular/router';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-email-template-list',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './email-template-list.component.html',
  styleUrl: './email-template-list.component.css'
})
export class EmailTemplateListComponent implements OnInit {
  templates: EmailTemplate[] = [];
  loading = false;
  currentPage = 0;
  pageSize = 10;
  totalElements = 0;
  totalPages = 0;
  pageSizeOptions = [10, 20, 50];

  constructor(
    private emailTemplateService: EmailTemplateService,
    private router: Router,
    private toastService: ToastService
  ) { }

  ngOnInit(): void {
    this.loadTemplates();
  }

  loadTemplates(): void {
    this.loading = true;
    this.emailTemplateService.getAllTemplates(this.currentPage, this.pageSize).subscribe({
      next: (page: Page<EmailTemplate>) => {
        this.templates = page.content;
        this.totalElements = page.totalElements;
        this.totalPages = page.totalPages;
        this.loading = false;
      },
      error: (err) => {
        console.error('Failed to load email templates', err);
        this.loading = false;
        this.toastService.error('Không thể tải danh sách email templates');
      }
    });
  }

  onPageChange(page: number): void {
    if (page >= 0 && page < this.totalPages) {
      this.currentPage = page;
      this.loadTemplates();
    }
  }

  editTemplate(id: string): void {
    this.router.navigate(['/admin/email-templates', id]);
  }

  createNewTemplate(): void {
    this.router.navigate(['/admin/email-templates/new']);
  }

  toggleTemplateStatus(template: EmailTemplate): void {
    if (!template.id) return;
    
    const updatedTemplate = { ...template, active: !template.active };
    this.emailTemplateService.updateTemplate(template.id, updatedTemplate).subscribe({
      next: () => {
        template.active = !template.active;
        this.toastService.success(`Đã ${template.active ? 'kích hoạt' : 'vô hiệu hóa'} template thành công`);
      },
      error: (err) => {
        console.error('Failed to update template status', err);
        this.toastService.error('Có lỗi xảy ra khi cập nhật trạng thái');
      }
    });
  }

  onPageSizeChange(event: Event): void {
    this.pageSize = Number((event.target as HTMLSelectElement).value);
    this.currentPage = 0;
    this.loadTemplates();
  }

  getPageNumbers(): number[] {
    const maxVisible = 5;
    let start = Math.max(0, this.currentPage - Math.floor(maxVisible / 2));
    let end = Math.min(this.totalPages, start + maxVisible);
    if (end - start < maxVisible) start = Math.max(0, end - maxVisible);
    return Array.from({ length: end - start }, (_, i) => start + i);
  }
}
