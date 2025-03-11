import { Component } from '@angular/core';
import { RouterLink, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [RouterLink, FormsModule],
  template: `
    <div class="register-container">
      <img src="assets/logo.png" alt="BloomCycle" class="logo">
      <h1>BloomCycle</h1>
      <h2>Create a new account</h2>

      <form class="register-form" (ngSubmit)="onSubmit()" #registerForm="ngForm">
        <div class="input-group">
          <label>
            <img src="assets/user-icon.png" alt="👤" class="user-icon">
            <input
              type="text"
              placeholder="Username"
              name="username"
              [(ngModel)]="registerData.username"
              required>
          </label>
        </div>

        <div class="input-group">
          <label>
            <img src="assets/name-icon.png" alt="📝" class="name-icon">
            <input
              type="text"
              placeholder="Full Name"
              name="fullName"
              [(ngModel)]="registerData.fullName"
              required>
          </label>
        </div>

        <div class="input-group">
          <label>
            <img src="assets/email-icon.png" alt="📧" class="email-icon">
            <input
              type="email"
              placeholder="Email"
              name="email"
              [(ngModel)]="registerData.email"
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
              [(ngModel)]="registerData.password"
              required>
          </label>
        </div>

        <div class="input-group">
          <label>
            <img src="assets/lock-icon.png" alt="🔒" class="lock-icon">
            <input
              type="password"
              placeholder="Retype Password"
              name="confirmPassword"
              [(ngModel)]="confirmPassword"
              required>
          </label>
        </div>

        @if (error) {
          <div class="error-message">{{error}}</div>
        }

        <button
          type="submit"
          class="register-button"
          [disabled]="!registerForm.form.valid || isLoading || !passwordsMatch()">
          {{isLoading ? 'Loading...' : 'Sign Up'}}
        </button>

        <a routerLink="/login" class="already-have-account">Already have an account? Login</a>
      </form>
    </div>
  `,
  styleUrl: './register.component.css'
})
export class RegisterComponent {
  registerData = {
    username: '',
    fullName: '',
    email: '',
    password: ''
  };

  confirmPassword = '';
  isLoading = false;
  error: string | null = null;

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  passwordsMatch(): boolean {
    return this.registerData.password === this.confirmPassword;
  }

  onSubmit(): void {
    if (this.isLoading || !this.passwordsMatch()) return;

    this.isLoading = true;
    this.error = null;

    this.authService.register(this.registerData)
      .subscribe({
        next: () => {
          this.router.navigate(['/login']);
        },
        error: (err) => {
          this.error = err.error?.message || 'An error occurred during registration';
          this.isLoading = false;
        }
      });
  }
}

