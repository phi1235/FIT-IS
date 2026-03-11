import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { TicketService, TicketDTO, PagedTicketResponse } from '../services/ticket.service';
import { AuthService } from '../services/auth.service';
import { ToastService } from '../services/toast.service';

@Component({
  selector: 'app-checker-hub',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  template: `
<div class="page">
  <!-- ═══ SIDEBAR ═══ -->
  <aside class="sidebar" [class.expanded]="sidebarOpen"
         (mouseenter)="sidebarOpen=true" (mouseleave)="sidebarOpen=false">
    <div class="sb-logo">
      <div class="sb-logo-icon"><i class="bi bi-shield-check"></i></div>
      <span class="sb-logo-text">FIS<span class="accent">BANK</span></span>
    </div>

    <nav class="sb-nav">
      <a class="sb-item sb-active" [routerLink]="['/checker']">
        <i class="bi bi-check2-square"></i>
        <span class="sb-lbl">Approval Hub</span>
      </a>
      <a class="sb-item" [routerLink]="['/tickets']">
        <i class="bi bi-clock-history"></i>
        <span class="sb-lbl">My Decisions</span>
      </a>
      <a class="sb-item" [routerLink]="['/home']">
        <i class="bi bi-bar-chart-line"></i>
        <span class="sb-lbl">SLA Reports</span>
      </a>
      <div class="sb-sep"></div>
      <a class="sb-item" [routerLink]="['/settings']">
        <i class="bi bi-gear"></i>
        <span class="sb-lbl">Settings</span>
      </a>
    </nav>

    <div class="sb-foot">
      <button class="sb-logout" (click)="logout()">
        <i class="bi bi-box-arrow-left"></i>
        <span class="sb-lbl">Sign Out</span>
      </button>
    </div>
  </aside>

  <!-- ═══ MAIN ═══ -->
  <div class="main">
    <!-- Top bar -->
    <header class="topbar">
      <div class="tb-left">
        <h1 class="tb-title">Checker Approval Hub</h1>
        <div class="tb-sep"></div>
        <div class="tb-alive"><span class="dot-green"></span><span class="alive-lbl">Active Session</span></div>
      </div>
      <div class="tb-right">
        <div class="search-wrap">
          <i class="bi bi-search search-ico"></i>
          <input class="search-input" placeholder="Search Ticket ID, Maker or Amount..."
                 [(ngModel)]="searchQuery" (input)="onSearch($event)"/>
        </div>
        <div class="tb-user">
          <div class="tb-utext">
            <span class="tb-uname">{{ displayName }}</span>
            <span class="tb-urole">Senior Checker</span>
          </div>
          <div class="tb-avatar">{{ initials }}</div>
        </div>
      </div>
    </header>

    <div class="scroll-area">

      <!-- Stats row -->
      <div class="stats-row">
        <div class="stat" (click)="setTab('PENDING')">
          <div>
            <p class="stat-lbl">PENDING MY REVIEW</p>
            <p class="stat-num">{{ stats.pending }}</p>
          </div>
          <div class="stat-ico ico-orange"><i class="bi bi-hourglass-split"></i></div>
        </div>
        <div class="stat" (click)="setTab('PENDING')">
          <div>
            <p class="stat-lbl">HIGH PRIORITY</p>
            <p class="stat-num red">{{ stats.highPriority }}</p>
          </div>
          <div class="stat-ico ico-red"><i class="bi bi-exclamation-circle"></i></div>
        </div>
        <div class="stat">
          <div>
            <p class="stat-lbl">OVERDUE SLA</p>
            <p class="stat-num amber">{{ stats.overdue }}</p>
          </div>
          <div class="stat-ico ico-amber"><i class="bi bi-alarm"></i></div>
        </div>
        <div class="stat" (click)="setTab('APPROVED')">
          <div>
            <p class="stat-lbl">TODAY'S DECISIONS</p>
            <p class="stat-num green">{{ stats.approved }}</p>
          </div>
          <div class="stat-ico ico-green"><i class="bi bi-check-circle-fill"></i></div>
        </div>
      </div>

      <!-- Queue table -->
      <div class="queue-card">
        <div class="queue-toolbar">
          <div class="qt-left">
            <div class="qt-title-wrap">
              <span class="qt-bar"></span>
              <h3 class="qt-title">APPROVAL QUEUE</h3>
            </div>
            <div class="qt-filters">
              <button class="f-btn" [class.f-active]="selectedTab==='ALL'"     (click)="setTab('ALL')">All Types</button>
              <button class="f-btn" [class.f-active]="selectedTab==='PENDING'" (click)="setTab('PENDING')">Pending</button>
              <button class="f-btn" [class.f-active]="selectedTab==='APPROVED'"(click)="setTab('APPROVED')">Approved</button>
              <button class="f-btn" [class.f-active]="selectedTab==='REJECTED'"(click)="setTab('REJECTED')">Rejected</button>
            </div>
          </div>
          <div class="qt-right">
            <span class="qt-sort-lbl">Sort by:</span>
            <select class="qt-sort">
              <option>SLA Deadline (Asc)</option>
              <option>Amount (Desc)</option>
              <option>Newest First</option>
            </select>
          </div>
        </div>

        <div *ngIf="loading" class="tbl-msg"><i class="bi bi-arrow-repeat spin"></i> Loading...</div>
        <div *ngIf="error" class="tbl-msg err">{{ error }}</div>

        <div *ngIf="!loading && !error" class="tbl-wrap">
          <table class="tbl">
            <thead>
              <tr>
                <th>REFERENCE ID</th>
                <th>MAKER DETAILS</th>
                <th>TRANSACTION TYPE</th>
                <th>AMOUNT</th>
                <th>STATUS</th>
                <th class="th-c">QUICK DECISION</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let t of tickets" class="tbl-row">
                <td class="td-code">{{ t.code }}</td>
                <td>
                  <div class="maker-cell">
                    <div class="maker-av">{{ mkInit(t.makerName) }}</div>
                    <div>
                      <p class="maker-name">{{ t.makerName }}</p>
                      <p class="maker-date">{{ t.createdAt | date:'MMM dd, hh:mm a' }}</p>
                    </div>
                  </div>
                </td>
                <td class="td-type">{{ t.title }}</td>
                <td class="td-amt">{{ fmtAmt(t.amount) }}</td>
                <td>
                  <span class="badge" [ngClass]="bdgClass(t.status)">{{ bdgLbl(t.status) }}</span>
                </td>
                <td class="td-act">
                  <div class="act-row" *ngIf="t.status==='PENDING'; else viewBtn">
                    <button class="btn-approve" (click)="doApprove(t)" [disabled]="busyId===t.id">
                      <i class="bi bi-check-lg"></i> Approve
                    </button>
                    <button class="btn-return" (click)="openReject(t)" [disabled]="busyId===t.id">
                      <i class="bi bi-arrow-counterclockwise"></i> Return
                    </button>
                  </div>
                  <ng-template #viewBtn>
                    <div class="act-row">
                      <a class="btn-view" [routerLink]="['/tickets', t.id]">
                        <i class="bi bi-eye"></i> View
                      </a>
                    </div>
                  </ng-template>
                </td>
              </tr>
              <tr *ngIf="tickets.length===0">
                <td colspan="6" class="tbl-msg">No tickets in this queue</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="pager">
          <span>Showing {{ pgStart() }}–{{ pgEnd() }} of {{ totalElements }} tickets pending review</span>
          <div class="pg-btns">
            <button class="pg-btn" (click)="prevPage()" [disabled]="currentPage===0">Prev</button>
            <button *ngFor="let p of pageNums()" class="pg-btn" [class.pg-on]="p===currentPage" (click)="goPage(p)">{{ p+1 }}</button>
            <button class="pg-btn" (click)="nextPage()" [disabled]="currentPage>=totalPages-1">Next</button>
          </div>
        </div>
      </div>

      <!-- Bottom: SLA chart + audit card -->
      <div class="bottom-row">
        <div class="sla-card">
          <div class="sla-hdr">
            <h3 class="sla-title"><i class="bi bi-hourglass-split" style="color:#E65100"></i> SLA Performance Distribution</h3>
            <span class="sla-sub">Real-time Tracker</span>
          </div>
          <div class="bar-chart">
            <div class="bar" style="height:92%"></div>
            <div class="bar" style="height:86%"></div>
            <div class="bar warn" style="height:67%"></div>
            <div class="bar" style="height:95%"></div>
            <div class="bar" style="height:81%"></div>
            <div class="bar danger" style="height:38%"></div>
            <div class="bar warn" style="height:62%"></div>
            <div class="bar" style="height:90%"></div>
          </div>
          <div class="bar-xlbl">
            <span>08:00</span><span>10:00</span><span>12:00</span>
            <span>14:00</span><span>16:00</span><span>Current</span>
          </div>
        </div>

        <div class="audit-card">
          <h3 class="audit-ttl">AUDIT SUMMARY</h3>
          <div class="audit-rows">
            <div class="audit-row"><span>Accuracy Rate</span><span class="green fw">99.8%</span></div>
            <div class="audit-row"><span>Avg. Review Time</span><span class="amber fw">2m 14s</span></div>
            <div class="audit-row"><span>Returned Today</span><span class="fw">{{ stats.rejected }}</span></div>
            <div class="audit-row"><span>Total Processed</span><span class="fw">{{ stats.approved }}</span></div>
          </div>
          <button class="dl-btn" (click)="goReports()">
            <i class="bi bi-download"></i> Download Shift Report
          </button>
        </div>
      </div>

    </div><!-- /scroll-area -->

    <footer class="footer">
      <span>Checker Hub v5.2.4 &bull; Secure Terminal Node 042</span>
      <div class="footer-r">
        <span><em class="dot-green"></em> SLA Compliance: 94.2%</span>
        <span><i class="bi bi-shield-check"></i> Encrypted Session</span>
      </div>
    </footer>
  </div><!-- /main -->
</div><!-- /page -->

<!-- Reject modal -->
<div class="overlay" *ngIf="showModal" (click)="closeModal()">
  <div class="modal" (click)="$event.stopPropagation()">
    <div class="modal-hdr">
      <h4 class="modal-title">Return Ticket for Correction</h4>
      <button class="modal-close" (click)="closeModal()"><i class="bi bi-x-lg"></i></button>
    </div>
    <div class="modal-body">
      <p class="modal-sub">Returning <strong>{{ rejectTarget?.code }}</strong> — provide a clear reason for the maker.</p>
      <label class="modal-lbl">Rejection Reason *</label>
      <textarea class="modal-ta" [(ngModel)]="rejectReason" placeholder="Describe the issue that needs correction..." rows="4"></textarea>
    </div>
    <div class="modal-foot">
      <button class="btn-cancel" (click)="closeModal()">Cancel</button>
      <button class="btn-confirm" (click)="confirmReject()" [disabled]="!rejectReason.trim() || busyId!=null">
        <i class="bi bi-arrow-counterclockwise"></i> Return to Maker
      </button>
    </div>
  </div>
</div>
  `,
  styles: [`
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
      background: #fff; border-right: 1px solid #E2E8F0;
      display: flex; flex-direction: column; overflow: hidden; flex-shrink: 0;
      box-shadow: 2px 0 8px rgba(0,0,0,.04); z-index: 50;
    }
    .sidebar.expanded { width: 224px; }

    .sb-logo {
      display: flex; align-items: center; gap: 12px;
      padding: 16px 14px; flex-shrink: 0; overflow: hidden;
    }
    .sb-logo-icon {
      min-width: 32px; width: 32px; height: 32px;
      background: #E65100; border-radius: 8px; color: #fff;
      display: flex; align-items: center; justify-content: center; font-size: 16px; flex-shrink: 0;
    }
    .sb-logo-text {
      font-size: 17px; font-weight: 700; letter-spacing: -.3px;
      white-space: nowrap; opacity: 0; transition: opacity .2s .05s; color: #1E293B;
    }
    .sidebar.expanded .sb-logo-text { opacity: 1; }
    .accent { color: #E65100; }

    .sb-nav { flex: 1; padding: 8px; overflow: hidden; display: flex; flex-direction: column; gap: 2px; }
    .sb-item {
      display: flex; align-items: center; gap: 14px;
      padding: 9px 10px; border-radius: 8px; text-decoration: none;
      font-size: 13px; font-weight: 600; color: #64748B; white-space: nowrap;
      overflow: hidden; transition: background .15s, color .15s;
    }
    .sb-item i { font-size: 18px; min-width: 20px; flex-shrink: 0; }
    .sb-lbl { opacity: 0; transition: opacity .15s .06s; }
    .sidebar.expanded .sb-lbl { opacity: 1; }
    .sb-item:hover { background: #F8FAFC; color: #334155; }
    .sb-active { background: #FFF3E0 !important; color: #E65100 !important; border-right: 2px solid #E65100; }
    .sb-sep { height: 1px; background: #F1F5F9; margin: 8px 10px; }
    .sb-foot { padding: 12px 8px; border-top: 1px solid #F1F5F9; }
    .sb-logout {
      display: flex; align-items: center; gap: 14px; padding: 9px 10px; border-radius: 8px;
      background: transparent; border: none; cursor: pointer;
      font-size: 13px; font-weight: 600; color: #64748B; width: 100%; overflow: hidden; transition: background .15s;
    }
    .sb-logout i { font-size: 18px; min-width: 20px; flex-shrink: 0; }
    .sb-logout .sb-lbl { opacity: 0; transition: opacity .15s .06s; white-space: nowrap; }
    .sidebar.expanded .sb-logout .sb-lbl { opacity: 1; }
    .sb-logout:hover { background: #F8FAFC; }

    /* ════ MAIN ════ */
    .main { flex: 1; display: flex; flex-direction: column; min-width: 0; overflow: hidden; }

    .topbar {
      height: 56px; background: #fff; border-bottom: 1px solid #E2E8F0;
      display: flex; align-items: center; justify-content: space-between;
      padding: 0 24px; flex-shrink: 0;
    }
    .tb-left { display: flex; align-items: center; gap: 14px; }
    .tb-title { font-size: 14px; font-weight: 700; color: #1E293B; letter-spacing: -.2px; }
    .tb-sep { width: 1px; height: 16px; background: #E2E8F0; }
    .tb-alive { display: flex; align-items: center; gap: 6px; }
    .dot-green { display: inline-block; width: 8px; height: 8px; border-radius: 50%; background: #22C55E; }
    .alive-lbl { font-size: 10px; font-weight: 700; color: #64748B; text-transform: uppercase; letter-spacing: .5px; }

    .tb-right { display: flex; align-items: center; gap: 20px; }
    .search-wrap { position: relative; display: flex; align-items: center; }
    .search-ico { position: absolute; left: 11px; color: #94A3B8; font-size: 13px; }
    .search-input {
      padding: 7px 14px 7px 32px; background: #F8FAFC;
      border: 1px solid #E2E8F0; border-radius: 8px; font-size: 12px; width: 280px;
      outline: none; transition: border-color .15s;
    }
    .search-input:focus { border-color: #E65100; }
    .tb-user { display: flex; align-items: center; gap: 10px; }
    .tb-utext { text-align: right; }
    .tb-uname { display: block; font-size: 12px; font-weight: 700; color: #334155; line-height: 1.2; }
    .tb-urole { display: block; font-size: 10px; color: #E65100; font-weight: 600; text-transform: uppercase; letter-spacing: .4px; }
    .tb-avatar {
      width: 32px; height: 32px; border-radius: 8px;
      background: #FFF3E0; color: #E65100; font-size: 12px; font-weight: 700;
      display: flex; align-items: center; justify-content: center; border: 1px solid #FFCCBC;
    }

    /* Scroll */
    .scroll-area {
      flex: 1; overflow-y: auto; padding: 16px;
      display: flex; flex-direction: column; gap: 16px;
    }
    .scroll-area::-webkit-scrollbar { width: 4px; }
    .scroll-area::-webkit-scrollbar-thumb { background: #CBD5E1; border-radius: 99px; }

    /* Stats */
    .stats-row { display: grid; grid-template-columns: repeat(4,1fr); gap: 14px; }
    .stat {
      background: #fff; padding: 14px 16px; border-radius: 12px;
      border: 1px solid #E2E8F0; box-shadow: 0 1px 3px rgba(0,0,0,.05);
      display: flex; align-items: center; justify-content: space-between;
      cursor: pointer; transition: border-color .15s;
    }
    .stat:hover { border-color: #FFCCBC; }
    .stat-lbl { font-size: 10px; font-weight: 700; color: #94A3B8; text-transform: uppercase; letter-spacing: .7px; margin-bottom: 4px; }
    .stat-num { font-size: 22px; font-weight: 800; color: #1E293B; }
    .red   { color: #DC2626 !important; }
    .amber { color: #EA580C !important; }
    .green { color: #16A34A !important; }
    .fw    { font-weight: 700; }
    .stat-ico { padding: 8px; border-radius: 8px; font-size: 18px; }
    .ico-orange { background: #FFF3E0; color: #E65100; }
    .ico-red    { background: #FEF2F2; color: #DC2626; }
    .ico-amber  { background: #FFF7ED; color: #EA580C; }
    .ico-green  { background: #F0FDF4; color: #16A34A; }

    /* Queue card */
    .queue-card {
      background: #fff; border-radius: 12px;
      border: 1px solid #E2E8F0; box-shadow: 0 1px 3px rgba(0,0,0,.05); overflow: hidden;
    }
    .queue-toolbar {
      padding: 12px 16px; border-bottom: 1px solid #F8FAFC;
      display: flex; align-items: center; justify-content: space-between;
      background: rgba(248,250,252,.5);
    }
    .qt-left { display: flex; align-items: center; gap: 16px; }
    .qt-title-wrap { display: flex; align-items: center; gap: 8px; }
    .qt-bar { width: 4px; height: 16px; background: #E65100; border-radius: 2px; }
    .qt-title { font-size: 12px; font-weight: 700; color: #1E293B; text-transform: uppercase; letter-spacing: .8px; }
    .qt-filters { display: flex; gap: 6px; }
    .f-btn {
      padding: 4px 10px; font-size: 10px; font-weight: 700;
      background: #fff; border: 1px solid #E2E8F0; border-radius: 4px;
      color: #475569; cursor: pointer; transition: border-color .15s;
    }
    .f-btn:hover { border-color: #E65100; }
    .f-active { background: #FFF3E0 !important; border-color: #E65100 !important; color: #E65100; }
    .qt-right { display: flex; align-items: center; gap: 6px; }
    .qt-sort-lbl { font-size: 10px; font-weight: 700; color: #94A3B8; }
    .qt-sort { font-size: 10px; font-weight: 700; border: none; background: transparent; color: #475569; cursor: pointer; outline: none; }

    .tbl-msg { padding: 28px; text-align: center; font-size: 13px; color: #94A3B8; }
    .err { color: #DC2626; }
    .spin { display: inline-block; animation: spin .7s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }

    .tbl-wrap { overflow-x: auto; }
    .tbl { width: 100%; border-collapse: collapse; font-size: 11px; text-align: left; }
    .tbl thead { background: #F8FAFC; border-bottom: 1px solid #F1F5F9; }
    .tbl thead th {
      padding: 10px 16px; font-size: 10px; font-weight: 700;
      color: #94A3B8; text-transform: uppercase; letter-spacing: .7px;
    }
    .th-c { text-align: center; }
    .tbl-row { border-bottom: 1px solid #F1F5F9; transition: background .1s; }
    .tbl-row:hover { background: rgba(248,250,252,.8); }
    .tbl-row td { padding: 12px 16px; vertical-align: middle; }
    .td-code { font-weight: 700; color: #1E293B; }
    .td-type { font-weight: 500; color: #475569; max-width: 180px; }
    .td-amt { font-weight: 700; color: #0F172A; }
    .td-act { text-align: center; }

    .maker-cell { display: flex; align-items: center; gap: 8px; }
    .maker-av {
      width: 28px; height: 28px; border-radius: 6px; background: #F1F5F9; color: #475569;
      font-size: 10px; font-weight: 700; display: flex; align-items: center; justify-content: center; flex-shrink: 0;
    }
    .maker-name { font-weight: 700; color: #334155; font-size: 11px; margin-bottom: 1px; }
    .maker-date { font-size: 9px; color: #94A3B8; }

    .badge {
      padding: 2px 8px; border-radius: 4px; font-size: 9px; font-weight: 800; text-transform: uppercase; letter-spacing: .4px;
    }
    .b-pending  { background: #FFF3E0; color: #EA580C; }
    .b-approved { background: #F0FDF4; color: #16A34A; }
    .b-rejected { background: #FEF2F2; color: #DC2626; }
    .b-draft    { background: #F1F5F9; color: #475569; }
    .b-default  { background: #F1F5F9; color: #64748B; }

    .act-row { display: flex; justify-content: center; gap: 6px; }
    .btn-approve {
      display: flex; align-items: center; gap: 4px;
      padding: 5px 10px; background: #22C55E; color: #fff;
      border: none; border-radius: 5px; font-size: 10px; font-weight: 700;
      cursor: pointer; box-shadow: 0 1px 3px rgba(34,197,94,.25); transition: background .15s, transform .1s;
    }
    .btn-approve:hover { background: #16A34A; }
    .btn-approve:active { transform: scale(.96); }
    .btn-approve:disabled { opacity: .5; cursor: not-allowed; }
    .btn-return {
      display: flex; align-items: center; gap: 4px;
      padding: 5px 10px; background: #F1F5F9; color: #475569;
      border: 1px solid #E2E8F0; border-radius: 5px; font-size: 10px; font-weight: 700;
      cursor: pointer; transition: all .15s;
    }
    .btn-return:hover { background: #FEF2F2; color: #DC2626; border-color: #FECACA; }
    .btn-return:disabled { opacity: .5; cursor: not-allowed; }
    .btn-view {
      display: inline-flex; align-items: center; gap: 4px;
      padding: 5px 10px; background: #F8FAFC; color: #64748B;
      border: 1px solid #E2E8F0; border-radius: 5px; font-size: 10px; font-weight: 700;
      cursor: pointer; text-decoration: none; transition: background .15s;
    }
    .btn-view:hover { background: #F1F5F9; }

    .pager {
      padding: 10px 16px; border-top: 1px solid #F1F5F9;
      display: flex; align-items: center; justify-content: space-between;
      font-size: 10px; font-weight: 700; color: #64748B;
      background: rgba(248,250,252,.4);
    }
    .pg-btns { display: flex; gap: 6px; }
    .pg-btn {
      padding: 4px 10px; background: #fff; border: 1px solid #E2E8F0; border-radius: 4px;
      font-size: 10px; font-weight: 700; color: #475569; cursor: pointer; transition: background .1s;
    }
    .pg-btn:hover:not(:disabled) { background: #F8FAFC; }
    .pg-btn:disabled { opacity: .4; cursor: not-allowed; }
    .pg-on { background: #E65100 !important; color: #fff !important; border-color: #E65100 !important; }

    /* Bottom row */
    .bottom-row { display: grid; grid-template-columns: 2fr 1fr; gap: 14px; }

    .sla-card {
      background: #fff; padding: 16px; border-radius: 12px;
      border: 1px solid #E2E8F0; box-shadow: 0 1px 3px rgba(0,0,0,.05);
    }
    .sla-hdr { display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px; }
    .sla-title { font-size: 12px; font-weight: 700; color: #334155; display: flex; align-items: center; gap: 6px; }
    .sla-sub { font-size: 10px; color: #94A3B8; }
    .bar-chart { display: flex; align-items: flex-end; height: 72px; gap: 5px; }
    .bar { flex: 1; background: #DCFCE7; border-radius: 3px 3px 0 0; transition: background .15s; cursor: pointer; }
    .bar:hover { background: #BBF7D0; }
    .bar.warn { background: #FED7AA; }
    .bar.warn:hover { background: #FDBA74; }
    .bar.danger { background: #FECACA; }
    .bar.danger:hover { background: #FCA5A5; }
    .bar-xlbl {
      display: flex; justify-content: space-between;
      margin-top: 6px; font-size: 9px; font-weight: 700; color: #94A3B8; text-transform: uppercase;
    }

    .audit-card {
      background: #0F172A; color: #fff; padding: 16px; border-radius: 12px;
      box-shadow: 0 4px 16px rgba(15,23,42,.2); display: flex; flex-direction: column;
    }
    .audit-ttl { font-size: 10px; font-weight: 700; color: rgba(255,255,255,.45); text-transform: uppercase; letter-spacing: 1px; margin-bottom: 12px; }
    .audit-rows { flex: 1; display: flex; flex-direction: column; gap: 10px; }
    .audit-row { display: flex; justify-content: space-between; align-items: center; }
    .audit-row span:first-child { font-size: 12px; color: rgba(255,255,255,.65); }
    .audit-row span:last-child { font-size: 13px; }
    .dl-btn {
      margin-top: 14px; width: 100%; padding: 8px;
      background: rgba(230,81,0,.15); border: 1px solid rgba(255,255,255,.1);
      border-radius: 8px; color: #fff; font-size: 10px; font-weight: 700;
      text-transform: uppercase; letter-spacing: .8px; cursor: pointer;
      display: flex; align-items: center; justify-content: center; gap: 6px;
      transition: background .15s;
    }
    .dl-btn:hover { background: rgba(230,81,0,.28); }

    /* Footer */
    .footer {
      display: flex; align-items: center; justify-content: space-between;
      padding: 0 24px; height: 32px; background: #fff;
      border-top: 1px solid #E2E8F0; font-size: 10px; color: #94A3B8; font-weight: 500; flex-shrink: 0;
    }
    .footer-r { display: flex; align-items: center; gap: 20px; font-weight: 700; text-transform: uppercase; letter-spacing: .4px; }
    .footer-r span { display: flex; align-items: center; gap: 5px; }

    /* Modal */
    .overlay {
      position: fixed; inset: 0; background: rgba(15,23,42,.45);
      display: flex; align-items: center; justify-content: center; z-index: 1000;
      backdrop-filter: blur(3px);
    }
    .modal {
      background: #fff; border-radius: 16px; width: 480px;
      max-width: calc(100vw - 32px); box-shadow: 0 20px 60px rgba(0,0,0,.2); overflow: hidden;
    }
    .modal-hdr {
      display: flex; align-items: center; justify-content: space-between;
      padding: 16px 20px; border-bottom: 1px solid #F1F5F9;
    }
    .modal-title { font-size: 14px; font-weight: 700; color: #1E293B; }
    .modal-close {
      width: 28px; height: 28px; border-radius: 6px; border: none;
      background: transparent; color: #94A3B8; cursor: pointer; font-size: 14px;
      display: flex; align-items: center; justify-content: center; transition: background .1s;
    }
    .modal-close:hover { background: #F1F5F9; }
    .modal-body { padding: 16px 20px; }
    .modal-sub { font-size: 13px; color: #64748B; margin-bottom: 14px; }
    .modal-lbl { display: block; font-size: 11px; font-weight: 700; color: #334155; margin-bottom: 6px; }
    .modal-ta {
      width: 100%; padding: 10px 12px; border: 1px solid #E2E8F0; border-radius: 8px;
      font-size: 13px; font-family: inherit; resize: vertical;
      outline: none; transition: border-color .15s; box-sizing: border-box;
    }
    .modal-ta:focus { border-color: #E65100; }
    .modal-foot {
      display: flex; justify-content: flex-end; gap: 10px;
      padding: 14px 20px; border-top: 1px solid #F1F5F9; background: #F8FAFC;
    }
    .btn-cancel {
      padding: 8px 16px; background: #fff; border: 1px solid #E2E8F0; border-radius: 8px;
      font-size: 12px; font-weight: 600; color: #475569; cursor: pointer; transition: background .15s;
    }
    .btn-cancel:hover { background: #F1F5F9; }
    .btn-confirm {
      display: flex; align-items: center; gap: 6px;
      padding: 8px 18px; background: #DC2626; color: #fff;
      border: none; border-radius: 8px; font-size: 12px; font-weight: 700; cursor: pointer; transition: background .15s;
    }
    .btn-confirm:hover { background: #B91C1C; }
    .btn-confirm:disabled { opacity: .5; cursor: not-allowed; }
  `]
})
export class CheckerHubComponent implements OnInit {
  sidebarOpen = false;
  tickets: TicketDTO[] = [];
  loading = false;
  error: string | null = null;
  selectedTab = 'PENDING';
  searchQuery = '';
  private searchTimeout: any;

  currentPage = 0;
  pageSize = 10;
  totalElements = 0;
  totalPages = 0;

  stats = { pending: 0, highPriority: 0, overdue: 0, approved: 0, rejected: 0 };
  busyId: string | null = null;

  showModal = false;
  rejectTarget: TicketDTO | null = null;
  rejectReason = '';

  constructor(
    private ticketService: TicketService,
    public authService: AuthService,
    private router: Router,
    private toastService: ToastService
  ) {}

  get displayName(): string {
    const u = this.authService.userInfo;
    if (u?.firstName && u?.lastName) return `${u.firstName} ${u.lastName}`;
    if (u?.firstName) return u.firstName;
    return u?.username || 'Checker';
  }

  get initials(): string {
    const n = this.displayName.split(' ');
    return n.length >= 2 ? (n[0][0] + n[n.length-1][0]).toUpperCase() : this.displayName.slice(0,2).toUpperCase();
  }

  ngOnInit(): void {
    this.loadStats();
    this.loadTickets();
  }

  loadStats(): void {
    forkJoin({
      pending:  this.ticketService.getTicketsPaginated(0, 1, '', 'PENDING'),
      approved: this.ticketService.getTicketsPaginated(0, 1, '', 'APPROVED'),
      rejected: this.ticketService.getTicketsPaginated(0, 1, '', 'REJECTED'),
    }).subscribe({
      next: r => {
        this.stats.pending  = r.pending.totalElements;
        this.stats.approved = r.approved.totalElements;
        this.stats.rejected = r.rejected.totalElements;
        this.stats.highPriority = Math.max(1, Math.floor(r.pending.totalElements * 0.3));
        this.stats.overdue      = Math.max(0, Math.floor(r.pending.totalElements * 0.15));
      }
    });
  }

  loadTickets(): void {
    this.loading = true;
    this.error = null;
    this.ticketService.getTicketsPaginated(this.currentPage, this.pageSize, this.searchQuery, this.selectedTab).subscribe({
      next: (r: PagedTicketResponse) => {
        this.tickets = r.content;
        this.totalElements = r.totalElements;
        this.totalPages = r.totalPages;
        this.loading = false;
      },
      error: () => { this.error = 'Unable to load approval queue.'; this.loading = false; }
    });
  }

  onSearch(e: Event): void {
    this.searchQuery = (e.target as HTMLInputElement).value;
    clearTimeout(this.searchTimeout);
    this.searchTimeout = setTimeout(() => { this.currentPage = 0; this.loadTickets(); }, 300);
  }

  setTab(t: string): void { this.selectedTab = t; this.currentPage = 0; this.loadTickets(); }
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
  mkInit(name: string): string {
    if (!name) return '?';
    const p = name.trim().split(' ');
    return p.length >= 2 ? (p[0][0] + p[p.length-1][0]).toUpperCase() : name.slice(0,2).toUpperCase();
  }
  bdgLbl(s: string): string {
    return ({ DRAFT:'DRAFT', PENDING:'PENDING', APPROVED:'APPROVED', REJECTED:'RETURNED', SUBMITTED:'SUBMITTED' } as any)[s] || s;
  }
  bdgClass(s: string): string {
    return ({ DRAFT:'b-draft', PENDING:'b-pending', APPROVED:'b-approved', REJECTED:'b-rejected' } as any)[s] || 'b-default';
  }

  doApprove(t: TicketDTO): void {
    this.busyId = t.id;
    this.ticketService.approveTicket(t.id).subscribe({
      next: () => {
        this.toastService.success(`Ticket ${t.code} approved.`);
        this.busyId = null;
        this.loadStats();
        this.loadTickets();
      },
      error: () => { this.toastService.error('Failed to approve. Please try again.'); this.busyId = null; }
    });
  }

  openReject(t: TicketDTO): void { this.rejectTarget = t; this.rejectReason = ''; this.showModal = true; }
  closeModal(): void { this.showModal = false; this.rejectTarget = null; this.rejectReason = ''; }

  confirmReject(): void {
    if (!this.rejectTarget || !this.rejectReason.trim()) return;
    this.busyId = this.rejectTarget.id;
    this.ticketService.rejectTicket(this.rejectTarget.id, this.rejectReason.trim()).subscribe({
      next: () => {
        this.toastService.success(`Ticket ${this.rejectTarget!.code} returned to maker.`);
        this.busyId = null;
        this.closeModal();
        this.loadStats();
        this.loadTickets();
      },
      error: () => { this.toastService.error('Failed to return ticket.'); this.busyId = null; }
    });
  }

  goReports(): void { this.router.navigate(['/tickets']); }
  logout(): void { this.authService.logout(); }
}
