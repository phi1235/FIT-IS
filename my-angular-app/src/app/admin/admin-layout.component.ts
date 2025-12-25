import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { KeycloakService } from '../services/keycloak.service';

@Component({
  selector: 'app-admin-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './admin-layout.component.html',
  styleUrl: './admin-layout.component.css'
})
export class AdminLayoutComponent implements OnInit {
  currentUser: any = null;
  isAuthenticated = false;
  isAdmin = false;

  constructor(private keycloakService: KeycloakService) { }

  async ngOnInit() {
    this.isAuthenticated = this.keycloakService.isAuthenticated();
    if (this.isAuthenticated) {
      await this.loadUserProfile();
    }

    this.keycloakService.isAuthenticated$.subscribe(isAuth => {
      this.isAuthenticated = isAuth;
      if (isAuth) {
        this.loadUserProfile();
      } else {
        this.currentUser = null;
      }
    });
  }

  async loadUserProfile() {
    try {
      this.currentUser = await this.keycloakService.getUserProfile();
      this.isAdmin = this.keycloakService.hasRole('admin');
    } catch (error) {
      console.error('Failed to load user profile:', error);
      if (this.keycloakService.isAuthenticated()) {
        const token = this.keycloakService.getToken();
        if (token) {
          try {
            const tokenPayload = JSON.parse(atob(token.split('.')[1]));
            this.currentUser = {
              username: tokenPayload.preferred_username || tokenPayload.sub,
              email: tokenPayload.email
            };
          } catch (e) {
            console.error('Failed to parse token:', e);
          }
        }
      }
    }
  }

  logout() {
    this.keycloakService.logout();
  }


}

