import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="login-container">
      <img src="assets/logo.png" alt="BloomCycle" class="logo">
      <h1>BloomCycle</h1>
      <h2>Login</h2>
      
      <form class="login-form">
        <div class="input-group">
          <label>
            <i class="user-icon">👤</i>
            <input type="text" placeholder="Username">
          </label>
        </div>
        
        <div class="input-group">
          <label>
            <i class="lock-icon">🔒</i>
            <input type="password" placeholder="Password">
          </label>
        </div>

        <a routerLink="/forgot-password" class="forgot-password">Forgot Password ?</a>
        
        <button type="submit" class="login-button">Login</button>
        
        <a routerLink="/register" class="create-account">Create new account</a>
      </form>
    </div>
  `,
  styleUrl: './login.component.css'
})
export class LoginComponent {}
