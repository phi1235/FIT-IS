import { Routes } from '@angular/router';
import { HomeComponent } from './home/home.component';
import { SignPageComponent } from './sign-page/sign-page.component';
import { LoginComponent } from './login/login.component';
import { AdminComponent } from './admin/admin.component';
import { AdminLayoutComponent } from './admin/admin-layout.component';
import { UserManagementComponent } from './admin/user-management.component';
import { portalGuard } from './guards/portal.guard';
import { adminGuard } from './guards/admin.guard';
import { ForgotPasswordComponent } from './password-management/forgot-password.component';
import { AdminPasswordResetComponent } from './password-management/admin-password-reset.component';
import { EmailTemplateListComponent } from './admin/email-templates/email-template-list.component';
import { EmailTemplateDetailComponent } from './admin/email-templates/email-template-detail.component';
import { permissionGuard } from './guards/permission.guard';
import { RoleManagementComponent } from './admin/role-management.component';

export const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'home', component: HomeComponent },
  { path: 'login', component: LoginComponent },
  { path: 'sign', component: SignPageComponent },
  { path: 'forgot-password', component: ForgotPasswordComponent },

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
      { path: '**', redirectTo: '' }
    ]
  },
  { path: '**', redirectTo: 'home' }
];

