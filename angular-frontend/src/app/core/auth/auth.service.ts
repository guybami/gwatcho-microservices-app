import { Injectable, inject } from '@angular/core';
import { KeycloakService } from './keycloak.service';

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private readonly keycloak = inject(KeycloakService);

  init(): Promise<boolean> {
    return this.keycloak.init();
  }

  login(): Promise<void> {
    return this.keycloak.login();
  }

  logout(): Promise<void> {
    return this.keycloak.logout();
  }

  isAuthenticated(): boolean {
    return this.keycloak.isAuthenticated();
  }

  getToken(): string | undefined {
    return this.keycloak.getToken();
  }

  updateToken(minValidity = 30): Promise<boolean> {
    return this.keycloak.updateToken(minValidity);
  }

  getUsername(): string | undefined {
    return this.keycloak.getUsername();
  }

  getEmail(): string | undefined {
    return this.keycloak.getEmail();
  }

  getFirstName(): string | undefined {
    return this.keycloak.getFirstName();
  }

  getLastName(): string | undefined {
    return this.keycloak.getLastName();
  }
}
