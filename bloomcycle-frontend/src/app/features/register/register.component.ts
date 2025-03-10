import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="register-container">
      <img src="assets/logo.png" alt="BloomCycle" class="logo">
      <h1>BloomCycle</h1>
      <h2>Create a new account</h2>

      <form class="register-form">
        <div class="input-group">
          <label>
            <i class="user-icon">👤</i>
            <input type="text" placeholder="Username" required>
          </label>
        </div>

        <div class="input-group">
          <label>
            <i class="name-icon">📝</i>
            <input type="text" placeholder="Full Name" required>
          </label>
        </div>

        <div class="input-group">
          <label>
            <i class="email-icon">📧</i>
            <input type="email" placeholder="Email" required>
          </label>
        </div>

        <div class="input-group">
          <label>
            <i class="lock-icon">🔒</i>
            <input type="password" placeholder="Password" required>
          </label>
        </div>

        <div class="input-group">
          <label>
            <i class="lock-icon">🔒</i>
            <input type="password" placeholder="Retype Password" required>
          </label>
        </div>

        <button type="submit" class="register-button">Sign Up</button>

        <a routerLink="/login" class="already-have-account">Already have an account? Login</a>
      </form>
    </div>
  `,
  styleUrl: './register.component.css'
})
export class RegisterComponent {}

