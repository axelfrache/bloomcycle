import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { Router } from '@angular/router';
import { JwtHelperService } from '@auth0/angular-jwt';

interface LoginResponse {
  token: string;
  email: string;
  username: string;
}

interface RegisterData {
  username: string;
  fullName: string;
  email: string;
  password: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = 'https://api-bloomcycle.axelfrache.me/api/v1';
  private tokenKey = 'auth_token';
  private isAuthenticatedSubject = new BehaviorSubject<boolean>(false);
  private jwtHelper = new JwtHelperService();
  private tokenCheckInterval: any;

  constructor(
    private http: HttpClient,
    private router: Router
  ) {
    this.validateAndSetAuthState();
    this.tokenCheckInterval = setInterval(() => this.validateAndSetAuthState(), 600000);
  }

  login(email: string, password: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/auth/login`, {
      email,
      password
    }).pipe(
      tap(response => {
        localStorage.setItem(this.tokenKey, response.token);
        this.isAuthenticatedSubject.next(true);
      })
    );
  }

  register(data: RegisterData): Observable<any> {
    return this.http.post(`${this.apiUrl}/auth/register`, data);
  }

  logout(): void {
    localStorage.removeItem(this.tokenKey);
    this.isAuthenticatedSubject.next(false);
    this.router.navigate(['/login']);
  }

  isAuthenticated(): Observable<boolean> {
    this.validateAndSetAuthState();
    return this.isAuthenticatedSubject.asObservable();
  }

  getToken(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  private validateAndSetAuthState(): void {
    try {
      const token = this.getToken();

      if (!token) {
        this.isAuthenticatedSubject.next(false);
        return;
      }

      const isExpired = this.jwtHelper.isTokenExpired(token);

      if (isExpired) {
        this.logout();
        return;
      }

      this.isAuthenticatedSubject.next(true);
    } catch (error) {
      this.isAuthenticatedSubject.next(false);
      this.logout();
    }
  }
}
