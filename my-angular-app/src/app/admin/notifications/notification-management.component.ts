import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NotificationService, NotificationDTO, CreateNotificationRequest } from '../../services/notification.service';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-notification-management',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './notification-management.component.html',
  styleUrl: './notification-management.component.css'
})
export class NotificationManagementComponent implements OnInit {
  notifications: NotificationDTO[] = [];
  loading = true;
  totalElements = 0;
  page = 0;
  pageSize = 20;

  showForm = false;
  editingId: string | null = null;
  saving = false;

  form: CreateNotificationRequest & { expiresAtStr?: string } = {
    title: '',
    content: '',
    type: 'info',
    expiresAt: null,
    expiresAtStr: ''
  };

  readonly typeOptions = [
    { value: 'info',    label: 'Thông tin',  icon: 'bi-info-circle-fill' },
    { value: 'warning', label: 'Cảnh báo',   icon: 'bi-exclamation-triangle-fill' },
    { value: 'success', label: 'Thành công', icon: 'bi-check-circle-fill' },
    { value: 'danger',  label: 'Nguy hiểm',  icon: 'bi-x-octagon-fill' }
  ];

  constructor(
    private notificationService: NotificationService,
    private toastService: ToastService
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.notificationService.getAll(this.page, this.pageSize).subscribe({
      next: (res) => {
        this.notifications = res.content;
        this.totalElements = res.totalElements;
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  openCreate(): void {
    this.editingId = null;
    this.form = { title: '', content: '', type: 'info', expiresAt: null, expiresAtStr: '' };
    this.showForm = true;
  }

  openEdit(n: NotificationDTO): void {
    this.editingId = n.id;
    this.form = {
      title: n.title,
      content: n.content,
      type: n.type,
      expiresAt: n.expiresAt || null,
      expiresAtStr: n.expiresAt ? n.expiresAt.substring(0, 16) : ''
    };
    this.showForm = true;
  }

  cancelForm(): void {
    this.showForm = false;
    this.editingId = null;
  }

  saveForm(): void {
    if (!this.form.title?.trim() || !this.form.content?.trim()) {
      this.toastService.error('Vui long nhap tieu de va noi dung');
      return;
    }
    this.saving = true;
    const req: CreateNotificationRequest = {
      title: this.form.title.trim(),
      content: this.form.content.trim(),
      type: this.form.type,
      expiresAt: this.form.expiresAtStr ? new Date(this.form.expiresAtStr).toISOString() : null
    };

    const call = this.editingId
      ? this.notificationService.update(this.editingId, req)
      : this.notificationService.create(req);

    call.subscribe({
      next: () => {
        this.saving = false;
        this.showForm = false;
        this.toastService.success(this.editingId ? 'Da cap nhat thong bao' : 'Da tao thong bao moi');
        this.load();
      },
      error: () => {
        this.saving = false;
        this.toastService.error('Co loi xay ra');
      }
    });
  }

  toggle(n: NotificationDTO): void {
    this.notificationService.toggle(n.id).subscribe({
      next: (updated) => {
        n.active = updated.active;
        this.toastService.success(updated.active ? 'Da kich hoat' : 'Da tat thong bao');
      },
      error: () => this.toastService.error('Co loi xay ra')
    });
  }

  delete(n: NotificationDTO): void {
    if (!confirm(`Xoa thong bao "${n.title}"?`)) return;
    this.notificationService.delete(n.id).subscribe({
      next: () => {
        this.notifications = this.notifications.filter(x => x.id !== n.id);
        this.toastService.success('Da xoa thong bao');
      },
      error: () => this.toastService.error('Co loi xay ra')
    });
  }

  getTypeLabel(type: string): string {
    return this.typeOptions.find(t => t.value === type)?.label || type;
  }

  getTypeIcon(type: string): string {
    return this.typeOptions.find(t => t.value === type)?.icon || 'bi-bell';
  }

  get totalPages(): number {
    return Math.ceil(this.totalElements / this.pageSize);
  }

  goPage(p: number): void {
    this.page = p;
    this.load();
  }
}
