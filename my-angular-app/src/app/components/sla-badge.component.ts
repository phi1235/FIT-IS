import { Component, Input, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SlaStatus } from '../services/ticket.service';

@Component({
  selector: 'app-sla-badge',
  standalone: true,
  imports: [CommonModule],
  template: `
    <ng-container *ngIf="slaStatus && slaStatus !== 'COMPLETED'">
      <span class="sla-badge" [ngClass]="cssClass" [title]="tooltip">
        <i class="bi" [ngClass]="icon"></i>
        <span>{{ label }}</span>
      </span>
    </ng-container>
  `,
  styles: [`
    .sla-badge {
      display: inline-flex; align-items: center; gap: 4px;
      padding: 2px 8px; border-radius: 999px;
      font-size: 10px; font-weight: 700;
      white-space: nowrap;
    }
    .sla-on-time  { background: #F0FDF4; color: #16A34A; }
    .sla-warning  { background: #FFF7ED; color: #EA580C; animation: pulse 1.5s ease-in-out infinite; }
    .sla-breached { background: #FEF2F2; color: #DC2626; }

    @keyframes pulse {
      0%, 100% { opacity: 1; }
      50%       { opacity: .6; }
    }
  `]
})
export class SlaBadgeComponent implements OnInit, OnDestroy {
  @Input() slaStatus?: SlaStatus;
  @Input() slaDeadline?: string;
  @Input() slaRemainingMinutes?: number;

  label = '';
  icon  = '';
  cssClass = '';
  tooltip = '';

  private timer: any;

  ngOnInit(): void {
    this.update();
    // Refresh label mỗi phút để countdown chính xác
    this.timer = setInterval(() => this.update(), 60_000);
  }

  ngOnDestroy(): void {
    clearInterval(this.timer);
  }

  private update(): void {
    if (!this.slaStatus || this.slaStatus === 'COMPLETED') return;

    switch (this.slaStatus) {
      case 'BREACHED':
        this.cssClass = 'sla-badge sla-breached';
        this.icon = 'bi-x-circle-fill';
        this.label = 'SLA Breached';
        this.tooltip = this.slaDeadline ? `Deadline: ${new Date(this.slaDeadline).toLocaleString('vi-VN')}` : 'SLA quá hạn';
        break;
      case 'WARNING':
        this.cssClass = 'sla-badge sla-warning';
        this.icon = 'bi-exclamation-triangle-fill';
        this.label = this.formatRemaining();
        this.tooltip = this.slaDeadline ? `Deadline: ${new Date(this.slaDeadline).toLocaleString('vi-VN')}` : 'Gần hết hạn SLA';
        break;
      case 'ON_TIME':
        this.cssClass = 'sla-badge sla-on-time';
        this.icon = 'bi-clock';
        this.label = this.formatRemaining();
        this.tooltip = this.slaDeadline ? `Deadline: ${new Date(this.slaDeadline).toLocaleString('vi-VN')}` : '';
        break;
    }
  }

  private formatRemaining(): string {
    const mins = this.slaRemainingMinutes;
    if (mins == null) return 'SLA';
    if (mins < 0)    return 'Overdue';
    if (mins < 60)   return `${mins}m left`;
    if (mins < 1440) return `${Math.floor(mins / 60)}h left`;
    return `${Math.floor(mins / 1440)}d left`;
  }
}
