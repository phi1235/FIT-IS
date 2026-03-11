import { Component, OnInit, OnDestroy, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NotificationService, NotificationDTO } from '../services/notification.service';

@Component({
  selector: 'app-notification-bell',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="bell-wrap">
      <!-- Bell button -->
      <button class="bell-btn" (click)="toggleDropdown()" [class.bell-active]="open" title="Thông báo hệ thống">
        <i class="bi bi-bell{{ hasNew ? '-fill' : '' }}"></i>
        <span class="badge" *ngIf="notifications.length > 0">{{ notifications.length > 9 ? '9+' : notifications.length }}</span>
      </button>

      <!-- Dropdown -->
      <div class="dropdown" *ngIf="open">
        <div class="dd-header">
          <span class="dd-title">Thông báo hệ thống</span>
          <span class="dd-count">{{ notifications.length }} active</span>
        </div>

        <div class="dd-body" *ngIf="notifications.length > 0; else emptyTpl">
          <div class="notif-item" *ngFor="let n of notifications" [ngClass]="'type-' + n.type">
            <div class="notif-icon">
              <i class="bi" [ngClass]="iconClass(n.type)"></i>
            </div>
            <div class="notif-text">
              <p class="notif-title">{{ n.title }}</p>
              <p class="notif-content">{{ n.content }}</p>
              <p class="notif-time">{{ formatTime(n.createdAt) }}</p>
            </div>
          </div>
        </div>

        <ng-template #emptyTpl>
          <div class="dd-empty">
            <i class="bi bi-check-circle"></i>
            <p>Không có thông báo mới</p>
          </div>
        </ng-template>

        <div class="dd-footer" *ngIf="lastPolled">
          Cập nhật lúc {{ lastPolled | date:'HH:mm:ss' }}
        </div>
      </div>
    </div>
  `,
  styles: [`
    .bell-wrap { position: relative; }

    .bell-btn {
      position: relative;
      width: 36px; height: 36px; border-radius: 8px;
      border: 1px solid #E2E8F0; background: #fff;
      color: #64748B; font-size: 16px; cursor: pointer;
      display: flex; align-items: center; justify-content: center;
      transition: background .15s, border-color .15s, color .15s;
    }
    .bell-btn:hover { background: #F8FAFC; border-color: #CBD5E1; color: #334155; }
    .bell-btn.bell-active { background: #FFF3E0; border-color: #E65100; color: #E65100; }

    .badge {
      position: absolute; top: -5px; right: -5px;
      min-width: 17px; height: 17px; padding: 0 4px;
      background: #E65100; color: #fff;
      font-size: 9px; font-weight: 700; border-radius: 999px;
      display: flex; align-items: center; justify-content: center;
      border: 2px solid #fff;
    }

    /* Dropdown */
    .dropdown {
      position: absolute; top: calc(100% + 8px); right: 0;
      width: 320px; background: #fff;
      border: 1px solid #E2E8F0; border-radius: 12px;
      box-shadow: 0 8px 24px rgba(0,0,0,.1);
      z-index: 1000; overflow: hidden;
    }

    .dd-header {
      display: flex; align-items: center; justify-content: space-between;
      padding: 12px 16px; border-bottom: 1px solid #F1F5F9;
      background: #F8FAFC;
    }
    .dd-title { font-size: 12px; font-weight: 700; color: #1E293B; }
    .dd-count  { font-size: 10px; font-weight: 700; color: #94A3B8; }

    .dd-body { max-height: 320px; overflow-y: auto; }
    .dd-body::-webkit-scrollbar { width: 4px; }
    .dd-body::-webkit-scrollbar-thumb { background: #CBD5E1; border-radius: 99px; }

    .notif-item {
      display: flex; gap: 10px; padding: 12px 16px;
      border-bottom: 1px solid #F8FAFC;
      transition: background .1s;
    }
    .notif-item:hover { background: #F8FAFC; }
    .notif-item:last-child { border-bottom: none; }

    .notif-icon {
      width: 30px; height: 30px; border-radius: 8px; flex-shrink: 0;
      display: flex; align-items: center; justify-content: center; font-size: 14px;
    }
    .type-info    .notif-icon { background: #EFF6FF; color: #2563EB; }
    .type-success .notif-icon { background: #F0FDF4; color: #16A34A; }
    .type-warning .notif-icon { background: #FFF7ED; color: #EA580C; }
    .type-danger  .notif-icon { background: #FEF2F2; color: #DC2626; }

    .notif-text { flex: 1; min-width: 0; }
    .notif-title {
      font-size: 12px; font-weight: 700; color: #1E293B;
      margin-bottom: 2px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
    }
    .notif-content {
      font-size: 11px; color: #64748B; line-height: 1.4;
      display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
    }
    .notif-time { font-size: 10px; color: #94A3B8; margin-top: 4px; }

    .dd-empty {
      padding: 32px 16px; text-align: center; color: #94A3B8;
    }
    .dd-empty i { font-size: 28px; color: #CBD5E1; margin-bottom: 8px; display: block; }
    .dd-empty p { font-size: 12px; font-weight: 500; }

    .dd-footer {
      padding: 8px 16px; border-top: 1px solid #F1F5F9;
      font-size: 10px; color: #94A3B8; text-align: center;
      background: #F8FAFC;
    }
  `]
})
export class NotificationBellComponent implements OnInit, OnDestroy {
  notifications: NotificationDTO[] = [];
  open = false;
  hasNew = false;
  lastPolled: Date | null = null;
  private pollInterval: any;
  private prevCount = 0;

  constructor(private notifService: NotificationService) {}

  ngOnInit(): void {
    this.poll();
    this.pollInterval = setInterval(() => this.poll(), 30_000);
  }

  ngOnDestroy(): void {
    clearInterval(this.pollInterval);
  }

  private poll(): void {
    this.notifService.getActive().subscribe({
      next: (data) => {
        if (data.length > this.prevCount) this.hasNew = true;
        this.prevCount = data.length;
        this.notifications = data;
        this.lastPolled = new Date();
      },
      error: () => {} // silent fail — don't disrupt UI
    });
  }

  toggleDropdown(): void {
    this.open = !this.open;
    if (this.open) this.hasNew = false;
  }

  @HostListener('document:click', ['$event'])
  onClickOutside(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    if (!target.closest('app-notification-bell')) {
      this.open = false;
    }
  }

  iconClass(type: string): string {
    return ({
      info:    'bi-info-circle-fill',
      success: 'bi-check-circle-fill',
      warning: 'bi-exclamation-triangle-fill',
      danger:  'bi-x-circle-fill',
    } as any)[type] ?? 'bi-bell-fill';
  }

  formatTime(iso: string): string {
    if (!iso) return '';
    const d = new Date(iso);
    const now = new Date();
    const diff = Math.floor((now.getTime() - d.getTime()) / 1000);
    if (diff < 60)   return 'Vừa xong';
    if (diff < 3600) return `${Math.floor(diff / 60)} phút trước`;
    if (diff < 86400) return `${Math.floor(diff / 3600)} giờ trước`;
    return d.toLocaleDateString('vi-VN');
  }
}
