import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { Router } from '@angular/router';

interface LoginResponse {
  token: string;
  user: {
    id: number;
    username: string;
    email: string;
  };
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
  private apiUrl = 'http://api-bloomcycle.axelfrache.me/api/v1';
  private tokenKey = 'auth_token';
  private isAuthenticatedSubject = new BehaviorSubject<boolean>(this.hasToken());

  constructor(
    private http: HttpClient,
    private router: Router
  ) {}

  login(username: string, password: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/auth/login`, {
      username,
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

  private hasToken(): boolean {
    return !!localStorage.getItem(this.tokenKey);
  }

  isAuthenticated(): Observable<boolean> {
    return this.isAuthenticatedSubject.asObservable();
  }

  getToken(): string | null {
    return localStorage.getItem(this.tokenKey);
  }
}
