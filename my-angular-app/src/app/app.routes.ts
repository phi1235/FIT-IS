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
      { path: 'users', component: UserManagementComponent, canActivate: [adminGuard] }, // User Management
      { path: 'password-reset', component: AdminPasswordResetComponent, canActivate: [adminGuard] }, // Admin Password Reset
      {
        path: 'tickets',
        children: [
          { path: '', loadComponent: () => import('./tickets/ticket-list.component').then(m => m.TicketListMainComponent) },
          { path: 'create', loadComponent: () => import('./tickets/ticket-create.component').then(m => m.TicketCreateComponent) },
          { path: ':id', loadComponent: () => import('./tickets/ticket-detail.component').then(m => m.TicketDetailComponent) }
        ]
      },
      {
        path: 'email-templates',
        canActivate: [adminGuard],
        children: [
          { path: '', component: EmailTemplateListComponent },
          { path: 'new', component: EmailTemplateDetailComponent },
          { path: ':id', component: EmailTemplateDetailComponent }
        ]
      },
      { path: '**', redirectTo: '' }
    ]
  },
  { path: '**', redirectTo: 'home' }
];

