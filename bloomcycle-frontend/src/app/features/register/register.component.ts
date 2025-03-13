import { Component } from '@angular/core';
import { RouterLink, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [RouterLink, FormsModule],
  template: `
    <div class="max-w-sm mx-auto p-6 text-center" style="margin: 0 auto; padding: 20px;">
      <img src="assets/logo.png" alt="BloomCycle" class="w-20" style="margin: 0 auto;">
      <h1 class="text-4xl" style="margin-bottom: 10px;">BloomCycle</h1>
      <h2 class="text-2xl text-gray-600" style="margin-bottom: 30px;">Create a new account</h2>

      <form (ngSubmit)="onSubmit()" #registerForm="ngForm" class="flex flex-col gap-5">
        <div class="relative">
          <label class="flex items-center gap-2 border-b border-gray-300 py-2" style="padding: 8px 0;">
            <img src="assets/user.svg" alt="👤" class="user-icon">
            <input type="text"
                   placeholder="Username"
                   name="username"
                   [(ngModel)]="registerData.username"
                   class="w-full border-none outline-none bg-transparent text-lg" style="border: none;"
                   required>
          </label>
        </div>

        <div class="relative">
          <label class="flex items-center gap-2 border-b border-gray-300 py-2" style="padding: 8px 0;">
            <img src="assets/user-square.svg" alt="📝" class="h-6 w-6">
            <input type="text"
                   placeholder="Full Name"
                   name="fullName"
                   [(ngModel)]="registerData.fullName"
                   class="w-full border-none outline-none bg-transparent text-lg" style="border: none;"
                   required>
          </label>
        </div>

        <div class="relative">
          <label class="flex items-center gap-2 border-b border-gray-300 py-2" style="padding: 8px 0;">
            <img src="assets/envelope-simple.svg" alt="📧" class="email-icon">
            <input type="email"
                   placeholder="Email"
                   name="email"
                   [(ngModel)]="registerData.email"
                   class="w-full border-none outline-none bg-transparent text-lg" style="border: none;"
                   required>
          </label>
        </div>

        <div class="relative">
          <label class="flex items-center gap-2 border-b border-gray-300 py-2" style="padding: 8px 0;">
            <img src="assets/lock.svg" alt="🔒" class="lock-icon">
            <input type="password"
                   placeholder="Password"
                   name="password"
                   [(ngModel)]="registerData.password"
                   class="w-full border-none outline-none bg-transparent text-lg" style="border: none;"
                   required>
          </label>
        </div>

        <div class="relative">
          <label class="flex items-center gap-2 border-b border-gray-300 py-2" style="padding: 8px 0;">
            <img src="assets/lock.svg" alt="🔒" class="lock-icon">
            <input type="password"
                   placeholder="Retype Password"
                   name="confirmPassword"
                   [(ngModel)]="confirmPassword"
                   class="w-full border-none outline-none bg-transparent text-lg" style="border: none;"
                   required>
          </label>
        </div>

        @if (error) {
          <div class="error-message">{{error}}</div>
        }

        <button
          type="submit"
          class="bg-[#6B7F94] text-white rounded-full text-xl w-full mt-5" style="padding: 12px; background-color: #6B7F94; margin: 20px 0; border-radius: 25px;"
          [disabled]="!registerForm.form.valid || isLoading || !passwordsMatch()">
          {{isLoading ? 'Loading...' : 'Sign Up'}}
        </button>

        <a routerLink="/login" class="text-gray-600 text-sm">Already have an account ? Login</a>
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

