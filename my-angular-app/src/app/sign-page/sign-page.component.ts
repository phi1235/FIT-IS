import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { KeycloakService } from '../services/keycloak.service';

@Component({
  selector: 'app-sign-page',
  imports: [RouterLink],
  templateUrl: './sign-page.component.html',
  styleUrl: './sign-page.component.css'
})
export class SignPageComponent {

  constructor(private keycloakService: KeycloakService) {}

  login(): void {
    this.keycloakService.login();
  }

  register(): void {
    this.keycloakService.register();
  }
}
