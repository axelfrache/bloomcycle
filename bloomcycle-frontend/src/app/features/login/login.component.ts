import { Component } from '@angular/core';
import { RouterLink, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [RouterLink, FormsModule],
  template: `
    <div class="login-container">
      <img src="assets/logo.png" alt="BloomCycle" class="logo">
      <h1>BloomCycle</h1>
      <h2>Login</h2>

      <form class="login-form" (ngSubmit)="onSubmit()" #loginForm="ngForm">
        <div class="input-group">
          <label>
            <img src="assets/user-icon.png" alt="👤" class="user-icon">
            <input
              type="text"
              placeholder="Username"
              name="username"
              [(ngModel)]="loginData.username"
              required>
          </label>
        </div>

        <div class="input-group">
          <label>
            <img src="assets/lock-icon.png" alt="🔒" class="lock-icon">
            <input
              type="password"
              placeholder="Password"
              name="password"
              [(ngModel)]="loginData.password"
              required>
          </label>
        </div>

        @if (error) {
          <div class="error-message">{{error}}</div>
        }

        <a routerLink="/forgot-password" class="forgot-password">Forgot Password ?</a>

        <button
          type="submit"
          class="login-button"
          [disabled]="!loginForm.form.valid || isLoading">
          {{isLoading ? 'Loading...' : 'Login'}}
        </button>

        <a routerLink="/register" class="create-account">Create new account</a>
      </form>
    </div>
  `,
  styleUrl: './login.component.css'
})
export class LoginComponent {
  loginData = {
    username: '',
    password: ''
  };

  isLoading = false;
  error: string | null = null;

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  onSubmit(): void {
    if (this.isLoading) return;

    this.isLoading = true;
    this.error = null;

    this.authService.login(this.loginData.username, this.loginData.password)
      .subscribe({
        next: () => {
          this.router.navigate(['/home']);
        },
        error: (err) => {
          this.error = err.error?.message || 'An error occurred during login';
          this.isLoading = false;
        }
      });
  }
}
