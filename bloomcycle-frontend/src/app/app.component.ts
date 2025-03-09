import { Component } from '@angular/core';
import { RouterOutlet, RouterLink } from '@angular/router';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink],
  template: `
    <header>
      <nav>
        <div class="logo-container">
          <img src="assets/logo.png" alt="BloomCycle" class="logo">
          <span class="logo-text">BloomCycle</span>
        </div>
        <div class="nav-links">
          <a routerLink="/home" class="nav-link">
            <img src="assets/eye-icon.png" alt="eye"> YOUR APPS
          </a>
          <a routerLink="/upload" class="nav-link">
            <img src="assets/upload-icon.png" alt="upload"> UPLOAD NEW APP
          </a>
        </div>
      </nav>
    </header>
    <main>
      <router-outlet></router-outlet>
    </main>
  `,
  styleUrl: './app.component.css'
})
export class AppComponent {
  title = 'bloomcycle';
}
