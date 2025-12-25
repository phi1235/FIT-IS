import { Routes } from '@angular/router';
import { HomeComponent } from './home/home.component';
import { SignPageComponent } from './sign-page/sign-page.component';
import { LoginComponent } from './login/login.component';
import { AdminComponent } from './admin/admin.component';
import { AdminLayoutComponent } from './admin/admin-layout.component';
import { portalGuard } from './guards/portal.guard';

export const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'home', component: HomeComponent },
  { path: 'login', component: LoginComponent },
  { path: 'sign', component: SignPageComponent },

  // Top-level tickets for regular users (no sidebar)
  {
    path: 'tickets',
    children: [
      { path: '', loadComponent: () => import('./tickets/ticket-list.component').then(m => m.TicketListComponent) },
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
        path: 'tickets',
        children: [
          { path: '', loadComponent: () => import('./tickets/ticket-list.component').then(m => m.TicketListComponent) },
          { path: 'create', loadComponent: () => import('./tickets/ticket-create.component').then(m => m.TicketCreateComponent) },
          { path: ':id', loadComponent: () => import('./tickets/ticket-detail.component').then(m => m.TicketDetailComponent) }
        ]
      },
      { path: '**', redirectTo: '' }
    ]
  },
  { path: '**', redirectTo: 'home' }
];
