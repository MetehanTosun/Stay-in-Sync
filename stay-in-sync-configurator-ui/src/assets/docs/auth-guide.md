# Authentifizierung und Autorisierung in Angular

In diesem Guide lernst du, wie man Authentifizierungs- und Autorisierungsmechanismen in einer Angular-Anwendung implementiert.

---

## Voraussetzungen

- Angular CLI installiert (`npm install -g @angular/cli`)
- Grundverständnis von Angular (Services, Routing, Guards)
- Backend-API mit JWT-Authentifizierung oder Auth-Anbieter wie Auth0 oder Keycloak

---

## Ziel des Guides

Am Ende dieses Guides wirst du:

- Benutzer authentifizieren (Login/Logout)
- Zugriffe auf Routen beschränken
- Benutzerrollen verwalten
- Token lokal speichern und automatisch mitsenden

---

## 1. Projekt vorbereiten

```bash
ng new angular-auth-demo --standalone --routing --style=scss
cd angular-auth-demo

ng generate service auth/auth
ng generate component login
ng generate component dashboard
ng generate guard auth/auth
```
## 2. AuthService implementieren

```typescript
// src/app/auth/auth.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = 'https://your-api.com/auth'; // Ersetze durch deine API-URL

  constructor(private http: HttpClient) {}

  login(username: string, password: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/login`, { username, password });
  }

  logout(): void {
    localStorage.removeItem('token');
  }

  isAuthenticated(): boolean {
    return !!localStorage.getItem('token');
  }

  getToken(): string | null {
    return localStorage.getItem('token');
  }
}
```
## 3. Login-Komponente erstellen

```typescript
// src/app/login/login.component.ts
import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../auth/auth.service';
@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent {
  username: string = '';
  password: string = '';
  errorMessage: string = '';

  constructor(private authService: AuthService, private router: Router) {}

  onLogin(): void {
    this.authService.login(this.username, this.password).subscribe({
      next: (response) => {
        localStorage.setItem('token', response.token);
        this.router.navigate(['/dashboard']);
      },
      error: (error) => {
        this.errorMessage = 'Login fehlgeschlagen. Bitte versuche es erneut.';
      }
    });
  }
}
```
