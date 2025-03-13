import { Component } from '@angular/core';
import {RouterOutlet, RouterLink, RouterLinkActive} from '@angular/router';
import {AuthService} from './core/services/auth.service';
import {AsyncPipe, NgIf} from '@angular/common';
import { Observable } from 'rxjs';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, NgIf, AsyncPipe],
  template: `
    <header *ngIf="hasToken$ | async">
      <nav>
        <div class="logo-container">
          <a routerLink="/home" class="nav-link">
            <img src="assets/logo.png" alt="BloomCycle" class="logo">
            <span class="logo-text">BloomCycle</span>
          </a>
        </div>
        <div class="nav-links">
          <a routerLink="/home" routerLinkActive="active" class="nav-link">
            <img src="assets/eye-icon.png" alt="eye"> YOUR APPS
          </a>
          <a routerLink="/upload" routerLinkActive="active" class="nav-link">
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
  hasToken$: Observable<boolean>;

  constructor(private authService: AuthService) {
    this.hasToken$ = this.authService.isAuthenticated();
  }
}
