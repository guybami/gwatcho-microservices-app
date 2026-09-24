import { Injectable } from '@angular/core';
import Keycloak from 'keycloak-js';
import { keycloakConfig } from './keycloak.config';

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private readonly keycloak = new Keycloak(keycloakConfig);

  private initialized = false;

  async init(): Promise<boolean> {

    if (this.initialized) {
      return this.keycloak.authenticated ?? false;
    }

    const authenticated = await this.keycloak.init({
      onLoad: 'check-sso',
      pkceMethod: 'S256',
      checkLoginIframe: false
    });

    this.initialized = true;

    return authenticated;
  }

  async login(): Promise<void> {
    await this.keycloak.login({
      redirectUri: window.location.origin
    });
  }

  async logout(): Promise<void> {
    await this.keycloak.logout({
      redirectUri: window.location.origin
    });
  }

  isAuthenticated(): boolean {
    return this.keycloak.authenticated ?? false;
  }

  getToken(): string | undefined {
    return this.keycloak.token;
  }

  getUsername(): string | undefined {
    return this.keycloak.tokenParsed?.['preferred_username'];
  }

  getEmail(): string | undefined {
    return this.keycloak.tokenParsed?.['email'];
  }

  getFirstName(): string | undefined {
    return this.keycloak.tokenParsed?.['given_name'];
  }

  getLastName(): string | undefined {
    return this.keycloak.tokenParsed?.['family_name'];
  }

  async updateToken(): Promise<boolean> {
    try {
      return await this.keycloak.updateToken(30);
    } catch (error) {
      console.error('Failed to refresh Keycloak token', error);
      return false;
    }
  }
}
