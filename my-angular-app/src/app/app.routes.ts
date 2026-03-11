import { Routes } from '@angular/router';
import { HomeComponent } from './home/home.component';
import { SignPageComponent } from './sign-page/sign-page.component';
import { LoginComponent } from './login/login.component';
import { AdminComponent } from './admin/admin.component';
import { AdminLayoutComponent } from './admin/admin-layout.component';
import { UserManagementComponent } from './admin/user-management.component';
import { portalGuard } from './guards/portal.guard';
import { adminGuard } from './guards/admin.guard';
import { authGuard } from './guards/auth.guard';
import { ForgotPasswordComponent } from './password-management/forgot-password.component';
import { AdminPasswordResetComponent } from './password-management/admin-password-reset.component';
import { EmailTemplateListComponent } from './admin/email-templates/email-template-list.component';
import { EmailTemplateDetailComponent } from './admin/email-templates/email-template-detail.component';
import { permissionGuard } from './guards/permission.guard';
import { RoleManagementComponent } from './admin/role-management.component';
import { AuditLogListComponent } from './admin/audit-logs/audit-log-list.component';
import { WorkflowListComponent } from './admin/workflow/workflow-list.component';
import { WorkflowDetailComponent } from './admin/workflow/workflow-detail.component';
import { ProfileComponent } from './profile/profile.component';
import { SettingsComponent } from './settings/settings.component';
import { NotificationManagementComponent } from './admin/notifications/notification-management.component';

export const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'home', component: HomeComponent },
  { path: 'login', component: LoginComponent },
  { path: 'sign', component: SignPageComponent },
  { path: 'forgot-password', component: ForgotPasswordComponent },

  // Maker Workspace — full-page standalone layout for MAKER role
  {
    path: 'workspace',
    canActivate: [authGuard],
    loadComponent: () => import('./tickets/maker-workspace.component').then(m => m.MakerWorkspaceComponent)
  },

  // Checker Hub — full-page standalone layout for CHECKER role
  {
    path: 'checker',
    canActivate: [authGuard],
    loadComponent: () => import('./tickets/checker-hub.component').then(m => m.CheckerHubComponent)
  },

  // Top-level tickets for regular users (no sidebar)
  {
    path: 'tickets',
    children: [
      { path: '', loadComponent: () => import('./tickets/ticket-list.component').then(m => m.TicketListMainComponent) },
      { path: 'create', loadComponent: () => import('./tickets/ticket-create.component').then(m => m.TicketCreateComponent) },
      { path: ':id', loadComponent: () => import('./tickets/ticket-detail.component').then(m => m.TicketDetailComponent) }
    ]
  },

  {
    path: 'admin',
    component: AdminLayoutComponent,
    canActivate: [portalGuard], // Cho phép Admin, Maker, Checker vào portal
    children: [
      { path: '', component: AdminComponent }, // Dashboard
      { 
        path: 'users', 
        component: UserManagementComponent, 
        canActivate: [permissionGuard], 
        data: { permission: 'USER_VIEW' } 
      }, // User Management
      { 
        path: 'password-reset', 
        component: AdminPasswordResetComponent, 
        canActivate: [permissionGuard], 
        data: { permission: 'USER_MANAGE' } 
      }, // Admin Password Reset
      {
        path: 'roles',
        component: RoleManagementComponent,
        canActivate: [permissionGuard],
        data: { permission: 'ROLE_VIEW' }
      },
      {
        path: 'tickets',
        canActivate: [permissionGuard],
        data: { permission: 'TICKET_VIEW' },
        children: [
          { path: '', loadComponent: () => import('./tickets/ticket-list.component').then(m => m.TicketListMainComponent) },
          { path: 'create', loadComponent: () => import('./tickets/ticket-create.component').then(m => m.TicketCreateComponent) },
          { path: ':id', loadComponent: () => import('./tickets/ticket-detail.component').then(m => m.TicketDetailComponent) }
        ]
      },
      {
        path: 'email-templates',
        canActivate: [permissionGuard],
        data: { permission: 'EMAIL_TEMPLATE_VIEW' },
        children: [
          { path: '', component: EmailTemplateListComponent },
          { path: 'new', component: EmailTemplateDetailComponent, data: { permission: 'EMAIL_TEMPLATE_MANAGE' } },
          { path: ':id', component: EmailTemplateDetailComponent, data: { permission: 'EMAIL_TEMPLATE_MANAGE' } }
        ]
      },
      {
        path: 'audit-logs',
        component: AuditLogListComponent,
        canActivate: [permissionGuard],
        data: { permission: 'AUDIT_VIEW' }
      },
      {
        path: 'notifications',
        component: NotificationManagementComponent,
        canActivate: [permissionGuard],
        data: { permission: 'AUDIT_VIEW' }
      },
      {
        path: 'workflow',
        canActivate: [permissionGuard],
        data: { permission: 'WORKFLOW_VIEW' },
        children: [
          { path: '', component: WorkflowListComponent },
          { path: ':id', component: WorkflowDetailComponent }
        ]
      },
      { path: '**', redirectTo: '' }
    ]
  },
  { path: 'profile', component: ProfileComponent, canActivate: [authGuard] },
  { path: 'settings', component: SettingsComponent, canActivate: [authGuard] },
  { path: '**', redirectTo: 'home' }
];

