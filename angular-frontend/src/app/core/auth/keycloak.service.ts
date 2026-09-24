import { Injectable } from '@angular/core';
import Keycloak from 'keycloak-js';
import { keycloakConfig } from './keycloak.config';

@Injectable({
  providedIn: 'root'
})
export class KeycloakService {

  private readonly keycloak: Keycloak;

  constructor() {
    this.keycloak = new Keycloak(keycloakConfig);
  }

  async init(): Promise<boolean> {
    return this.keycloak.init({
      onLoad: 'check-sso',
      pkceMethod: 'S256',
      checkLoginIframe: false
    });
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

  async updateToken(minValidity = 30): Promise<boolean> {
    try {
      return await this.keycloak.updateToken(minValidity);
    } catch (error) {
      console.error('Unable to refresh Keycloak token', error);
      return false;
    }
  }

  getTokenParsed(): Record<string, unknown> | undefined {
    return this.keycloak.tokenParsed;
  }

  getUsername(): string | undefined {
    return this.keycloak.tokenParsed?.['preferred_username'] as string | undefined;
  }

  getEmail(): string | undefined {
    return this.keycloak.tokenParsed?.['email'] as string | undefined;
  }

  getFirstName(): string | undefined {
    return this.keycloak.tokenParsed?.['given_name'] as string | undefined;
  }

  getLastName(): string | undefined {
    return this.keycloak.tokenParsed?.['family_name'] as string | undefined;
  }
}
