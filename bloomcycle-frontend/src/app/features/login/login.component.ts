import { Component } from '@angular/core';
import { RouterLink, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [RouterLink, FormsModule],
  template: `
    <div class="max-w-sm mx-auto p-6 text-center" style="margin: 0 auto; padding: 20px;">
      <img src="assets/logo.png" alt="BloomCycle" class="w-20" style="margin: 0 auto;">
      <h1 class="text-4xl" style="margin-bottom: 10px;">BloomCycle</h1>
      <h2 class="text-2xl text-gray-600" style="margin-bottom: 30px;">Login</h2>

      <form (ngSubmit)="onSubmit()" #loginForm="ngForm" class="flex flex-col gap-5">
        <div class="relative">
          <label class="flex items-center gap-2 border-b border-gray-300 py-2" style="padding: 8px 0;">
            <img src="assets/email-icon.png" alt="📧" class="email-icon">
            <input type="text"
                   placeholder="Email"
                   name="email"
                   [(ngModel)]="loginData.email"
                   class="w-full border-none outline-none bg-transparent text-lg" style="border: none;"
                   required>
          </label>
        </div>

        <div class="relative">
          <label class="flex items-center gap-2 border-b border-gray-300 py-2" style="padding: 8px 0;">
            <img src="assets/lock-icon.png" alt="🔒" class="lock-icon">
            <input type="password"
                   placeholder="Password"
                   name="password"
                   [(ngModel)]="loginData.password"
                   class="w-full border-none outline-none bg-transparent text-lg" style="border: none;"
                   required>
          </label>
        </div>

        @if (error) {
          <div class="error-message">{{error}}</div>
        }

        <a routerLink="/forgot-password" class="text-right text-gray-600 text-sm">Forgot Password?</a>

        <button type="submit" [disabled]="!loginForm.form.valid || isLoading" class="bg-[#6B7F94] text-white rounded-full text-xl w-full mt-5" style="padding: 12px; background-color: #6B7F94; margin: 20px 0; border-radius: 25px;">
          {{isLoading ? 'Loading...' : 'Login'}}</button>

        <a routerLink="/register" class="text-gray-600 text-sm">Create new account</a>
      </form>
    </div>
  `,
  styleUrl: './login.component.css'
})
export class LoginComponent {
  loginData = {
    email: '',
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

    this.authService.login(this.loginData.email, this.loginData.password)
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
