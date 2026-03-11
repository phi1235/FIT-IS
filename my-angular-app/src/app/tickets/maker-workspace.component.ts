import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { TicketService, TicketDTO, PagedTicketResponse } from '../services/ticket.service';
import { AuthService } from '../services/auth.service';
import { SlaBadgeComponent } from '../components/sla-badge.component';

@Component({
  selector: 'app-maker-workspace',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, SlaBadgeComponent],
  template: `
<div class="page">
  <!-- ═══ SIDEBAR ═══ -->
  <aside class="sidebar" [class.expanded]="sidebarOpen"
         (mouseenter)="sidebarOpen=true" (mouseleave)="sidebarOpen=false">
    <!-- Logo -->
    <div class="sb-logo">
      <div class="sb-logo-icon"><i class="bi bi-bank2"></i></div>
      <span class="sb-logo-text">FIS<span class="sb-logo-accent">BANK</span></span>
    </div>

    <!-- Nav -->
    <nav class="sb-nav">
      <a class="sb-item sb-active" [routerLink]="['/workspace']">
        <i class="bi bi-house-fill"></i>
        <span class="sb-label">My Workspace</span>
      </a>
      <a class="sb-item" [routerLink]="['/tickets/create']">
        <i class="bi bi-plus-square"></i>
        <span class="sb-label">New Request</span>
      </a>
      <a class="sb-item" [routerLink]="['/tickets']">
        <i class="bi bi-clock-history"></i>
        <span class="sb-label">My History</span>
      </a>
      <div class="sb-divider"></div>
      <a class="sb-item" [routerLink]="['/home']">
        <i class="bi bi-bell"></i>
        <span class="sb-label">Alerts</span>
      </a>
    </nav>

    <!-- Bottom: logout -->
    <div class="sb-bottom">
      <button class="sb-logout" (click)="logout()">
        <i class="bi bi-box-arrow-left"></i>
        <span class="sb-label">Sign Out</span>
      </button>
    </div>
  </aside>

  <!-- ═══ MAIN ═══ -->
  <div class="main">
    <!-- Top bar -->
    <header class="topbar">
      <div class="topbar-left">
        <h1 class="tb-title">Maker Workspace</h1>
        <span class="tb-badge-green">Active Session</span>
      </div>
      <div class="topbar-right">
        <div class="tb-login-chip">
          <span class="tb-chip-label">Last Login: {{ lastLoginTime }}</span>
        </div>
        <div class="tb-divider"></div>
        <div class="tb-user">
          <div class="tb-user-text">
            <span class="tb-user-name">{{ displayName }}</span>
            <span class="tb-user-meta">Maker ID: #{{ makerId }}</span>
          </div>
          <div class="tb-avatar">{{ avatarInitials }}</div>
        </div>
      </div>
    </header>

    <!-- Scrollable content -->
    <div class="scroll-area">

      <!-- Stats Section -->
      <section class="section">
        <div class="section-hdr">
          <h2 class="section-title">MY DAILY TASKS</h2>
          <span class="section-updated">Updated just now</span>
        </div>
        <div class="stats-grid">
          <div class="stat-card" [class.stat-selected]="selectedTab==='DRAFT'" (click)="setTab('DRAFT')">
            <div class="stat-row">
              <div class="stat-icon icon-slate"><i class="bi bi-pencil-square"></i></div>
              <span class="stat-num">{{ stats.draft }}</span>
            </div>
            <p class="stat-lbl">MY DRAFTS</p>
          </div>
          <div class="stat-card" [class.stat-selected]="selectedTab==='PENDING'" (click)="setTab('PENDING')">
            <div class="stat-row">
              <div class="stat-icon icon-blue"><i class="bi bi-send-check"></i></div>
              <span class="stat-num">{{ stats.pending }}</span>
            </div>
            <p class="stat-lbl">SENT TO CHECKER</p>
          </div>
          <div class="stat-card stat-warn-ring" [class.stat-selected]="selectedTab==='REJECTED'" (click)="setTab('REJECTED')">
            <div class="stat-row">
              <div class="stat-icon icon-orange"><i class="bi bi-exclamation-triangle"></i></div>
              <span class="stat-num stat-orange">{{ stats.rejected }}</span>
            </div>
            <p class="stat-lbl">NEEDS CORRECTION</p>
          </div>
          <div class="stat-card" [class.stat-selected]="selectedTab==='APPROVED'" (click)="setTab('APPROVED')">
            <div class="stat-row">
              <div class="stat-icon icon-green"><i class="bi bi-check-circle-fill"></i></div>
              <span class="stat-num">{{ stats.approved }}</span>
            </div>
            <p class="stat-lbl">SUCCESSFULLY PROCESSED</p>
          </div>
        </div>
      </section>

      <!-- Ticket Table -->
      <section class="table-card">
        <div class="table-hdr">
          <div>
            <h3 class="table-title">Recent Ticket Activity</h3>
            <p class="table-sub">Manage and track your initiated requests</p>
          </div>
          <button class="btn-create" [routerLink]="['/tickets/create']">
            <i class="bi bi-plus-lg"></i> Create New Ticket
          </button>
        </div>

        <!-- Tabs -->
        <div class="tabs">
          <button class="tab" [class.tab-active]="selectedTab==='ALL'" (click)="setTab('ALL')">All Tickets</button>
          <button class="tab" [class.tab-active]="selectedTab==='PENDING'" (click)="setTab('PENDING')">Processing</button>
          <button class="tab" [class.tab-active]="selectedTab==='REJECTED'" (click)="setTab('REJECTED')">Rejected</button>
          <button class="tab" [class.tab-active]="selectedTab==='APPROVED'" (click)="setTab('APPROVED')">Completed</button>
        </div>

        <div *ngIf="loading" class="tbl-msg"><i class="bi bi-arrow-repeat spin"></i> Loading...</div>
        <div *ngIf="error" class="tbl-msg tbl-err">{{ error }}</div>

        <div *ngIf="!loading && !error" class="tbl-wrap">
          <table class="tbl">
            <thead>
              <tr>
                <th>TICKET ID</th>
                <th>TRANSACTION TYPE</th>
                <th>AMOUNT</th>
                <th>CREATED ON</th>
                <th>SLA</th>
                <th>STATUS</th>
                <th class="th-r">ACTIONS</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let t of tickets" [class.row-rework]="t.status==='REJECTED'">
                <td class="td-code">{{ t.code }}</td>
                <td>
                  <div class="td-title">{{ t.title }}</div>
                  <div *ngIf="t.rejectionReason" class="td-sub td-red">
                    <i class="bi bi-arrow-return-left"></i> {{ t.rejectionReason }}
                  </div>
                </td>
                <td class="td-amt">{{ fmtAmt(t.amount) }}</td>
                <td class="td-date">{{ t.createdAt | date:'MMM dd, hh:mm a' }}</td>
                <td>
                  <app-sla-badge
                    [slaStatus]="t.slaStatus"
                    [slaDeadline]="t.slaDeadline"
                    [slaRemainingMinutes]="t.slaRemainingMinutes">
                  </app-sla-badge>
                  <span *ngIf="!t.slaStatus" class="td-date" style="font-size:10px">—</span>
                </td>
                <td>
                  <span class="badge" [ngClass]="badgeClass(t.status)">
                    <span class="badge-dot"></span>{{ badgeLabel(t.status) }}
                  </span>
                </td>
                <td class="td-act">
                  <ng-container [ngSwitch]="t.status">
                    <button *ngSwitchCase="'REJECTED'" class="act-btn act-primary" [routerLink]="['/tickets', t.id]">Modify</button>
                    <button *ngSwitchCase="'DRAFT'"    class="act-btn act-ghost"   [routerLink]="['/tickets', t.id]">Resume</button>
                    <button *ngSwitchDefault           class="act-icon"            [routerLink]="['/tickets', t.id]" title="View">
                      <i class="bi bi-eye"></i>
                    </button>
                  </ng-container>
                </td>
              </tr>
              <tr *ngIf="tickets.length===0">
                <td colspan="6" class="tbl-msg">No tickets found</td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Pagination -->
        <div class="pagination">
          <p class="pg-info">Showing {{ pgStart() }}–{{ pgEnd() }} of {{ totalElements }} initiated tickets</p>
          <div class="pg-btns">
            <button class="pg-btn" (click)="prevPage()" [disabled]="currentPage===0">
              <i class="bi bi-chevron-left"></i>
            </button>
            <button *ngFor="let p of pageNums()" class="pg-btn" [class.pg-active]="p===currentPage" (click)="goPage(p)">
              {{ p+1 }}
            </button>
            <button class="pg-btn" (click)="nextPage()" [disabled]="currentPage>=totalPages-1">
              <i class="bi bi-chevron-right"></i>
            </button>
          </div>
        </div>
      </section>
    </div><!-- /scroll-area -->

    <!-- Footer -->
    <footer class="footer">
      <span>Maker Portal v2.4.0 &bull; Environment: Production</span>
      <div class="footer-r">
        <span><em class="dot-green"></em> System Ready</span>
        <span><em class="dot-green"></em> Auto-Sync Active</span>
      </div>
    </footer>
  </div><!-- /main -->
</div><!-- /page -->
  `,
  styles: [`
    /* ── Root layout ── */
    :host { display: block; height: 100vh; overflow: hidden; }
    * { box-sizing: border-box; margin: 0; padding: 0; }
    .page {
      display: flex; height: 100vh; overflow: hidden;
      font-family: 'Plus Jakarta Sans','Segoe UI',system-ui,sans-serif;
      background: #F8FAFC;
    }

    /* ════ SIDEBAR ════ */
    .sidebar {
      width: 64px; min-width: 64px;
      transition: width .28s cubic-bezier(.4,0,.2,1);
      background: #fff;
      border-right: 1px solid #E2E8F0;
      display: flex; flex-direction: column;
      overflow: hidden; flex-shrink: 0;
      box-shadow: 2px 0 8px rgba(0,0,0,.04);
      z-index: 50;
    }
    .sidebar.expanded { width: 224px; }

    .sb-logo {
      display: flex; align-items: center; gap: 12px;
      padding: 16px 14px; flex-shrink: 0; overflow: hidden;
    }
    .sb-logo-icon {
      min-width: 32px; width: 32px; height: 32px;
      background: #E65100; border-radius: 8px; color: #fff;
      display: flex; align-items: center; justify-content: center;
      font-size: 16px; flex-shrink: 0;
    }
    .sb-logo-text {
      font-size: 17px; font-weight: 700; letter-spacing: -.3px;
      white-space: nowrap; opacity: 0; transition: opacity .2s .05s;
      color: #1E293B;
    }
    .sidebar.expanded .sb-logo-text { opacity: 1; }
    .sb-logo-accent { color: #E65100; }

    .sb-nav { flex: 1; padding: 8px; overflow: hidden; display: flex; flex-direction: column; gap: 2px; }

    .sb-item {
      display: flex; align-items: center; gap: 14px;
      padding: 9px 10px; border-radius: 8px;
      text-decoration: none; font-size: 13px; font-weight: 600;
      color: #64748B; white-space: nowrap;
      transition: background .15s, color .15s;
      overflow: hidden;
    }
    .sb-item i { font-size: 18px; min-width: 20px; flex-shrink: 0; }
    .sb-label { opacity: 0; transition: opacity .15s .06s; }
    .sidebar.expanded .sb-label { opacity: 1; }
    .sb-item:hover { background: #F8FAFC; color: #334155; }
    .sb-active { background: #FFF3E0 !important; color: #E65100 !important; border-right: 2px solid #E65100; }

    .sb-divider { height: 1px; background: #F1F5F9; margin: 8px 10px; }

    .sb-bottom {
      padding: 12px 8px; border-top: 1px solid #F1F5F9;
    }
    .sb-logout {
      display: flex; align-items: center; gap: 14px;
      padding: 9px 10px; border-radius: 8px;
      background: transparent; border: none; cursor: pointer;
      font-size: 13px; font-weight: 600; color: #64748B;
      width: 100%; overflow: hidden;
      transition: background .15s;
    }
    .sb-logout i { font-size: 18px; min-width: 20px; flex-shrink: 0; }
    .sb-logout .sb-label { opacity: 0; transition: opacity .15s .06s; white-space: nowrap; }
    .sidebar.expanded .sb-logout .sb-label { opacity: 1; }
    .sb-logout:hover { background: #F8FAFC; }

    /* ════ MAIN ════ */
    .main { flex: 1; display: flex; flex-direction: column; min-width: 0; overflow: hidden; }

    /* Top bar */
    .topbar {
      height: 56px; background: #fff; border-bottom: 1px solid #E2E8F0;
      display: flex; align-items: center; justify-content: space-between;
      padding: 0 24px; flex-shrink: 0;
    }
    .topbar-left { display: flex; align-items: center; gap: 12px; }
    .tb-title { font-size: 14px; font-weight: 700; color: #1E293B; }
    .tb-badge-green {
      padding: 2px 8px; background: #DCFCE7; color: #16A34A;
      font-size: 10px; font-weight: 700; border-radius: 4px; text-transform: uppercase; letter-spacing: .5px;
    }
    .topbar-right { display: flex; align-items: center; gap: 16px; }
    .tb-login-chip {
      background: #F8FAFC; border: 1px solid #E2E8F0; border-radius: 999px;
      padding: 4px 12px;
    }
    .tb-chip-label { font-size: 10px; font-weight: 700; color: #94A3B8; text-transform: uppercase; letter-spacing: .5px; }
    .tb-divider { width: 1px; height: 16px; background: #E2E8F0; }
    .tb-user { display: flex; align-items: center; gap: 10px; }
    .tb-user-text { text-align: right; }
    .tb-user-name { display: block; font-size: 12px; font-weight: 700; color: #334155; line-height: 1.2; }
    .tb-user-meta { display: block; font-size: 10px; color: #94A3B8; font-weight: 500; margin-top: 2px; }
    .tb-avatar {
      width: 32px; height: 32px; border-radius: 8px;
      background: #FFF3E0; color: #E65100; font-size: 12px; font-weight: 700;
      display: flex; align-items: center; justify-content: center;
      border: 1px solid #FFCCBC;
    }

    /* Scroll area */
    .scroll-area {
      flex: 1; overflow-y: auto; padding: 24px;
      display: flex; flex-direction: column; gap: 24px;
    }
    .scroll-area::-webkit-scrollbar { width: 4px; }
    .scroll-area::-webkit-scrollbar-thumb { background: #CBD5E1; border-radius: 99px; }

    /* Section header */
    .section-hdr { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
    .section-title { font-size: 12px; font-weight: 700; color: #1E293B; text-transform: uppercase; letter-spacing: 1px; }
    .section-updated { font-size: 11px; color: #94A3B8; font-weight: 500; }

    /* Stats grid */
    .stats-grid { display: grid; grid-template-columns: repeat(4,1fr); gap: 16px; }
    .stat-card {
      background: #fff; border: 1px solid #E2E8F0; border-radius: 12px;
      padding: 16px; cursor: pointer;
      box-shadow: 0 1px 3px rgba(0,0,0,.05);
      transition: border-color .15s, box-shadow .15s;
    }
    .stat-card:hover { border-color: #FFCCBC; }
    .stat-warn-ring { box-shadow: 0 0 0 1px #FFF3E0; }
    .stat-selected { border-color: #E65100 !important; box-shadow: 0 0 0 3px rgba(230,81,0,.08) !important; }
    .stat-row { display: flex; align-items: center; justify-content: space-between; }
    .stat-icon {
      padding: 8px; border-radius: 8px; font-size: 18px;
      display: flex; align-items: center; justify-content: center;
    }
    .icon-slate { background: #F1F5F9; color: #64748B; }
    .stat-card:hover .icon-slate { background: #FFF3E0; color: #E65100; }
    .icon-blue  { background: #EFF6FF; color: #2563EB; }
    .icon-orange{ background: #FFF3E0; color: #EA580C; }
    .icon-green { background: #F0FDF4; color: #16A34A; }
    .stat-num { font-size: 26px; font-weight: 800; color: #1E293B; }
    .stat-orange { color: #EA580C; }
    .stat-lbl { font-size: 11px; font-weight: 700; color: #94A3B8; text-transform: uppercase; letter-spacing: .8px; margin-top: 8px; }

    /* Table card */
    .table-card {
      background: #fff; border: 1px solid #E2E8F0; border-radius: 12px;
      box-shadow: 0 1px 3px rgba(0,0,0,.05); overflow: hidden;
    }
    .table-hdr {
      display: flex; align-items: flex-start; justify-content: space-between;
      padding: 20px 24px 16px;
    }
    .table-title { font-size: 14px; font-weight: 700; color: #1E293B; margin-bottom: 3px; }
    .table-sub { font-size: 11px; color: #94A3B8; }
    .btn-create {
      display: flex; align-items: center; gap: 6px;
      padding: 8px 18px; background: #E65100; color: #fff;
      border: none; border-radius: 8px; font-size: 12px; font-weight: 700;
      cursor: pointer; box-shadow: 0 2px 8px rgba(230,81,0,.25);
      transition: background .15s, transform .1s; white-space: nowrap;
    }
    .btn-create:hover { background: #C14400; }
    .btn-create:active { transform: scale(.97); }

    /* Tabs */
    .tabs { display: flex; padding: 0 24px; border-bottom: 1px solid #F1F5F9; gap: 0; }
    .tab {
      padding: 10px 0; margin-right: 24px;
      font-size: 11px; font-weight: 700; color: #94A3B8;
      background: transparent; border: none; border-bottom: 2px solid transparent;
      cursor: pointer; transition: color .15s, border-color .15s;
    }
    .tab:hover { color: #475569; }
    .tab-active { color: #E65100; border-bottom-color: #E65100; }

    /* Table */
    .tbl-msg { padding: 32px; text-align: center; font-size: 13px; color: #94A3B8; }
    .tbl-err { color: #DC2626; }
    .spin { display: inline-block; animation: spin .7s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }
    .tbl-wrap { overflow-x: auto; }
    .tbl { width: 100%; border-collapse: collapse; font-size: 12px; text-align: left; }
    .tbl thead { background: #F8FAFC; border-bottom: 1px solid #F1F5F9; }
    .tbl thead th {
      padding: 12px 24px; font-size: 10px; font-weight: 700;
      color: #94A3B8; text-transform: uppercase; letter-spacing: .8px;
    }
    .th-r { text-align: right; }
    .tbl tbody tr { border-bottom: 1px solid #F8FAFC; transition: background .1s; }
    .tbl tbody tr:hover { background: #F8FAFC; }
    .row-rework { background: rgba(255,237,213,.2); }
    .row-rework:hover { background: rgba(255,237,213,.5) !important; }
    .tbl tbody td { padding: 14px 24px; vertical-align: middle; }
    .td-code { font-weight: 700; color: #1E293B; }
    .td-title { font-weight: 500; color: #334155; }
    .td-sub { font-size: 10px; color: #94A3B8; margin-top: 2px; }
    .td-red { color: #DC2626 !important; }
    .td-amt { font-weight: 700; color: #334155; }
    .td-date { color: #64748B; }
    .td-act { text-align: right; }

    /* Badges */
    .badge {
      display: inline-flex; align-items: center; gap: 5px;
      padding: 3px 9px; border-radius: 999px; font-size: 10px; font-weight: 700;
    }
    .badge-dot { width: 5px; height: 5px; border-radius: 50%; background: currentColor; opacity: .7; }
    .b-rejected { background: #FEF2F2; color: #DC2626; }
    .b-pending  { background: #EFF6FF; color: #2563EB; }
    .b-draft    { background: #F1F5F9; color: #475569; }
    .b-approved { background: #F0FDF4; color: #16A34A; }
    .b-submitted{ background: #FFF3E0; color: #EA580C; }
    .b-default  { background: #F1F5F9; color: #64748B; }

    /* Action btns */
    .act-btn {
      padding: 5px 14px; border-radius: 6px; border: none;
      font-size: 10px; font-weight: 700; cursor: pointer; transition: background .15s;
    }
    .act-primary { background: #E65100; color: #fff; }
    .act-primary:hover { background: #C14400; }
    .act-ghost { background: #F1F5F9; color: #334155; }
    .act-ghost:hover { background: #E2E8F0; }
    .act-icon {
      width: 30px; height: 30px; border: none; border-radius: 6px;
      background: transparent; color: #94A3B8; font-size: 15px;
      display: inline-flex; align-items: center; justify-content: center;
      cursor: pointer; transition: background .15s;
    }
    .act-icon:hover { background: #F1F5F9; }

    /* Pagination */
    .pagination {
      display: flex; align-items: center; justify-content: space-between;
      padding: 14px 24px; border-top: 1px solid #F1F5F9;
    }
    .pg-info { font-size: 11px; color: #94A3B8; font-weight: 500; }
    .pg-btns { display: flex; gap: 6px; }
    .pg-btn {
      width: 32px; height: 32px; border-radius: 6px;
      border: 1px solid #E2E8F0; background: #fff; color: #475569;
      font-size: 11px; font-weight: 700; cursor: pointer;
      display: flex; align-items: center; justify-content: center;
      transition: background .1s;
    }
    .pg-btn:hover:not(:disabled) { background: #F8FAFC; }
    .pg-btn:disabled { opacity: .35; cursor: not-allowed; }
    .pg-active { background: #FFF3E0 !important; border-color: #E65100 !important; color: #E65100 !important; }

    /* Footer */
    .footer {
      display: flex; align-items: center; justify-content: space-between;
      padding: 0 24px; height: 32px; background: #fff;
      border-top: 1px solid #E2E8F0;
      font-size: 10px; font-weight: 500; color: #94A3B8; flex-shrink: 0;
    }
    .footer-r { display: flex; align-items: center; gap: 16px; font-weight: 700; text-transform: uppercase; letter-spacing: .4px; }
    .footer-r span { display: flex; align-items: center; gap: 5px; }
    .dot-green { display: inline-block; width: 6px; height: 6px; border-radius: 50%; background: #22C55E; }
  `]
})
export class MakerWorkspaceComponent implements OnInit {
  sidebarOpen = false;
  tickets: TicketDTO[] = [];
  loading = false;
  error: string | null = null;
  selectedTab = 'ALL';

  currentPage = 0;
  pageSize = 10;
  totalElements = 0;
  totalPages = 0;

  stats = { draft: 0, pending: 0, rejected: 0, approved: 0 };
  lastLoginTime = new Date().toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });

  constructor(
    private ticketService: TicketService,
    public authService: AuthService,
    private router: Router
  ) {}

  get displayName(): string {
    const u = this.authService.userInfo;
    if (u?.firstName && u?.lastName) return `${u.firstName} ${u.lastName}`;
    if (u?.firstName) return u.firstName;
    return u?.username || 'User';
  }

  get makerId(): string {
    return (this.authService.userInfo?.username || '').slice(-4).toUpperCase() || '—';
  }

  get avatarInitials(): string {
    const n = this.displayName.split(' ');
    return n.length >= 2 ? (n[0][0] + n[n.length-1][0]).toUpperCase() : this.displayName.slice(0,2).toUpperCase();
  }

  ngOnInit(): void {
    this.loadStats();
    this.loadTickets();
  }

  loadStats(): void {
    forkJoin({
      draft:    this.ticketService.getTicketsPaginated(0, 1, '', 'DRAFT'),
      pending:  this.ticketService.getTicketsPaginated(0, 1, '', 'PENDING'),
      rejected: this.ticketService.getTicketsPaginated(0, 1, '', 'REJECTED'),
      approved: this.ticketService.getTicketsPaginated(0, 1, '', 'APPROVED'),
    }).subscribe({
      next: r => {
        this.stats.draft    = r.draft.totalElements;
        this.stats.pending  = r.pending.totalElements;
        this.stats.rejected = r.rejected.totalElements;
        this.stats.approved = r.approved.totalElements;
      }
    });
  }

  loadTickets(): void {
    this.loading = true;
    this.error = null;
    this.ticketService.getTicketsPaginated(this.currentPage, this.pageSize, '', this.selectedTab).subscribe({
      next: (r: PagedTicketResponse) => {
        this.tickets = r.content;
        this.totalElements = r.totalElements;
        this.totalPages = r.totalPages;
        this.loading = false;
      },
      error: () => { this.error = 'Unable to load tickets. Please try again.'; this.loading = false; }
    });
  }

  setTab(tab: string): void { this.selectedTab = tab; this.currentPage = 0; this.loadTickets(); }
  goPage(p: number): void { if (p >= 0 && p < this.totalPages) { this.currentPage = p; this.loadTickets(); } }
  nextPage(): void { if (this.currentPage < this.totalPages-1) { this.currentPage++; this.loadTickets(); } }
  prevPage(): void { if (this.currentPage > 0) { this.currentPage--; this.loadTickets(); } }

  pageNums(): number[] {
    const max = 5, pages: number[] = [];
    let s = Math.max(0, this.currentPage - Math.floor(max/2));
    let e = Math.min(this.totalPages, s + max);
    if (e - s < max) s = Math.max(0, e - max);
    for (let i = s; i < e; i++) pages.push(i);
    return pages;
  }

  pgStart(): number { return this.totalElements === 0 ? 0 : this.currentPage * this.pageSize + 1; }
  pgEnd(): number   { return Math.min((this.currentPage + 1) * this.pageSize, this.totalElements); }

  fmtAmt(v?: number): string { return v == null ? '—' : v.toLocaleString('de-DE') + ' VND'; }

  badgeLabel(s: string): string {
    return ({ DRAFT:'DRAFT', PENDING:'AT CHECKER', SUBMITTED:'SUBMITTED', APPROVED:'PROCESSED', REJECTED:'REJECTED' } as any)[s] || s;
  }

  badgeClass(s: string): string {
    return ({ DRAFT:'b-draft', PENDING:'b-pending', SUBMITTED:'b-submitted', APPROVED:'b-approved', REJECTED:'b-rejected' } as any)[s] || 'b-default';
  }

  logout(): void { this.authService.logout(); }
}
