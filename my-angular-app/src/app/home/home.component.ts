import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { TicketService, TicketDTO, TicketStatus } from '../services/ticket.service';
import { NotificationService, NotificationDTO } from '../services/notification.service';
import { UserInfo } from '../models/api.models';
import { Subscription } from 'rxjs';

interface StatCard {
  label: string;
  value: number;
  displayValue: number;
  icon: string;
  color: string;
  gradientFrom: string;
  gradientTo: string;
  status: string;
}

interface DonutSegment {
  label: string;
  value: number;
  percentage: number;
  color: string;
  offset: number;
}

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterModule, DatePipe],
  templateUrl: './home.component.html',
  styleUrl: './home.component.css'
})
export class HomeComponent implements OnInit, OnDestroy {
  userInfo: UserInfo | null = null;
  isLoggedIn = false;
  recentTickets: TicketDTO[] = [];
  isLoading = true;
  today = new Date();

  stats: StatCard[] = [
    { label: 'Tổng Ticket', value: 0, displayValue: 0, icon: 'bi-ticket-perforated-fill', color: '#6366f1', gradientFrom: '#6366f1', gradientTo: '#818cf8', status: '' },
    { label: 'Chờ duyệt', value: 0, displayValue: 0, icon: 'bi-hourglass-split', color: '#f59e0b', gradientFrom: '#f59e0b', gradientTo: '#fbbf24', status: 'PENDING' },
    { label: 'Đã duyệt', value: 0, displayValue: 0, icon: 'bi-check-circle-fill', color: '#10b981', gradientFrom: '#10b981', gradientTo: '#34d399', status: 'APPROVED' },
    { label: 'Từ chối', value: 0, displayValue: 0, icon: 'bi-x-circle-fill', color: '#ef4444', gradientFrom: '#ef4444', gradientTo: '#f87171', status: 'REJECTED' },
  ];

  donutSegments: DonutSegment[] = [];
  donutTotal = 0;

  announcements: NotificationDTO[] = [];

  readonly NOTIF_ICON: Record<string, string> = {
    info:    'bi-info-circle-fill',
    warning: 'bi-exclamation-triangle-fill',
    success: 'bi-check-circle-fill',
    danger:  'bi-x-octagon-fill'
  };

  readonly STATUS_LABELS: Record<string, string> = {
    DRAFT: 'Nháp', PENDING: 'Chờ duyệt', SUBMITTED: 'Đã nộp',
    APPROVED: 'Đã duyệt', REJECTED: 'Từ chối', COMPLETED: 'Hoàn thành'
  };

  readonly STATUS_CLASS: Record<string, string> = {
    DRAFT: 'badge-draft', PENDING: 'badge-pending', SUBMITTED: 'badge-submitted',
    APPROVED: 'badge-approved', REJECTED: 'badge-rejected', COMPLETED: 'badge-completed'
  };

  private animationTimers: any[] = [];
  private subscriptions: Subscription[] = [];

  constructor(
    public authService: AuthService,
    private ticketService: TicketService,
    private notificationService: NotificationService
  ) {}

  ngOnInit() {
    this.userInfo = this.authService.userInfo;
    this.isLoggedIn = this.authService.isAuthenticated;

    // Subscribe to auth changes
    const sub = this.authService.isAuthenticated$.subscribe(isAuth => {
      this.isLoggedIn = isAuth;
      this.userInfo = this.authService.userInfo;
      if (isAuth) {
        this.loadTicketStats();
        this.loadNotifications();
      }
    });
    this.subscriptions.push(sub);

    // Keep userInfo in sync (e.g. when department/position loads after login)
    const userSub = this.authService.userInfo$.subscribe(info => {
      this.userInfo = info;
    });
    this.subscriptions.push(userSub);

    if (this.isLoggedIn) {
      this.loadTicketStats();
      this.loadNotifications();
    } else {
      this.isLoading = false;
    }
  }

  ngOnDestroy() {
    this.animationTimers.forEach(t => clearInterval(t));
    this.subscriptions.forEach(s => s.unsubscribe());
  }

  get displayName(): string {
    if (this.userInfo?.firstName) return this.userInfo.firstName;
    return this.userInfo?.username || 'Người dùng';
  }

  get greeting(): string {
    const h = new Date().getHours();
    if (h < 12) return 'Chào buổi sáng';
    if (h < 18) return 'Chào buổi chiều';
    return 'Chào buổi tối';
  }

  get userInitials(): string {
    const name = this.userInfo?.firstName || this.userInfo?.username || 'U';
    return name.charAt(0).toUpperCase();
  }

  get userRoles(): string[] {
    if (!this.userInfo?.roles?.length) return [];
    return this.userInfo.roles.map(r => r.replace(/^ROLE_/i, ''));
  }

  loadNotifications() {
    this.notificationService.getActive().subscribe({
      next: (list) => { this.announcements = list; },
      error: () => { this.announcements = []; }
    });
  }

  getNotifIcon(type: string): string {
    return this.NOTIF_ICON[type] || 'bi-bell-fill';
  }

  loadTicketStats() {
    this.isLoading = true;
    this.ticketService.getTicketsPaginated(0, 100).subscribe({
      next: (res) => {
        const tickets = res.content;
        this.stats[0].value = res.totalElements;
        this.stats[1].value = tickets.filter(t => t.status === 'PENDING' || t.status === 'SUBMITTED').length;
        this.stats[2].value = tickets.filter(t => t.status === 'APPROVED' || t.status === 'COMPLETED').length;
        this.stats[3].value = tickets.filter(t => t.status === 'REJECTED').length;
        this.recentTickets = tickets.slice(0, 5);
        this.isLoading = false;

        // Animate counters
        this.animateCounters();
        // Build donut chart
        this.buildDonutData();
      },
      error: () => { this.isLoading = false; }
    });
  }

  private animateCounters() {
    this.animationTimers.forEach(t => clearInterval(t));
    this.animationTimers = [];

    this.stats.forEach((stat) => {
      stat.displayValue = 0;
      if (stat.value === 0) return;

      const duration = 1200; // ms
      const steps = 30;
      const increment = stat.value / steps;
      let current = 0;
      let step = 0;

      const timer = setInterval(() => {
        step++;
        current += increment;
        if (step >= steps) {
          stat.displayValue = stat.value;
          clearInterval(timer);
        } else {
          stat.displayValue = Math.round(current);
        }
      }, duration / steps);

      this.animationTimers.push(timer);
    });
  }

  private buildDonutData() {
    const total = this.stats[0].value;
    this.donutTotal = total;
    if (total === 0) {
      this.donutSegments = [];
      return;
    }

    const segmentData = [
      { label: 'Chờ duyệt', value: this.stats[1].value, color: '#f59e0b' },
      { label: 'Đã duyệt', value: this.stats[2].value, color: '#10b981' },
      { label: 'Từ chối', value: this.stats[3].value, color: '#ef4444' },
    ];

    // Calculate "other" (Draft, etc.)
    const knownTotal = segmentData.reduce((sum, s) => sum + s.value, 0);
    const other = total - knownTotal;
    if (other > 0) {
      segmentData.push({ label: 'Khác', value: other, color: '#94a3b8' });
    }

    let offset = 0;
    this.donutSegments = segmentData
      .filter(s => s.value > 0)
      .map(s => {
        const percentage = (s.value / total) * 100;
        const segment: DonutSegment = {
          label: s.label,
          value: s.value,
          percentage,
          color: s.color,
          offset
        };
        offset += percentage;
        return segment;
      });
  }

  getStatusLabel(status: string): string {
    return this.STATUS_LABELS[status] || status;
  }

  getStatusClass(status: string): string {
    return this.STATUS_CLASS[status] || '';
  }

  getTimeAgo(dateStr: string): string {
    // Backend trả UTC không có 'Z' → JS hiểu nhầm là local → lệch 7h.
    // Thêm 'Z' để ép parse đúng UTC rồi tự convert sang giờ địa phương.
    const utc = dateStr && !dateStr.endsWith('Z') && !dateStr.includes('+') ? dateStr + 'Z' : dateStr;
    const date = new Date(utc);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    if (diffMins < 1) return 'Vừa xong';
    if (diffMins < 60) return `${diffMins} phút trước`;
    const diffHours = Math.floor(diffMins / 60);
    if (diffHours < 24) return `${diffHours} giờ trước`;
    const diffDays = Math.floor(diffHours / 24);
    if (diffDays < 7) return `${diffDays} ngày trước`;
    return date.toLocaleDateString('vi-VN');
  }
}
