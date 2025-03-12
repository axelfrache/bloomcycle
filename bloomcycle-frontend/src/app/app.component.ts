import { Component } from '@angular/core';
import {RouterOutlet, RouterLink, RouterLinkActive, Router} from '@angular/router';
import {NgClass, AsyncPipe, NgIf} from '@angular/common';
import {AuthService} from './core/services/auth.service';
import { Observable } from 'rxjs';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, NgClass, NgIf, AsyncPipe],
  template: `
    <header *ngIf="hasToken$ | async" class="bg-white border-b border-gray-300" style="padding: 15px 30px 0 30px;">
      <nav class="flex justify-between items-center max-w-screen-xl" style="margin: 0 auto;">
        <div class="flex items-center gap-2">
          <a routerLink="/home" class="flex items-center gap-2">
            <img src="assets/logo.png" alt="BloomCycle" class="h-10">
            <span class="font-serif text-xl text-gray-800">BloomCycle</span>
          </a>
        </div>
        <div class="flex gap-8">
          <a routerLink="/home" routerLinkActive="active" [ngClass]="{'bg-[#f5f0e6] border border-gray-800 border-b-0 font-bold': isActive('/home')}"
             class="flex items-center gap-2 text-gray-800 rounded-t-xl hover:bg-gray-100 border-gr" style="padding: 10px 20px;">
            <img src="assets/eye-icon.png" alt="eye" class="h-10">
            YOUR APPS
          </a>
          <a routerLink="/upload" routerLinkActive="active" [ngClass]="{'bg-[#f5f0e6] border border-gray-800 border-b-0 font-bold': isActive('/upload')}"
             class="flex items-center gap-2 text-gray-800 py-2 px-4 rounded-t-xl hover:bg-gray-100">
            <img src="assets/upload-icon.png" alt="upload" class="h-10">
            UPLOAD NEW APP
          </a>
        </div>
      </nav>
    </header>
    <main class="min-h-screen bg-white p-10" style="padding: 40px;">
      <router-outlet></router-outlet>
    </main>
  `,
  styleUrl: './app.component.css'
})
export class AppComponent {
  title = 'bloomcycle';
  hasToken$: Observable<boolean>;

  constructor(private router: Router, private authService: AuthService) {
    this.hasToken$ = this.authService.isAuthenticated();
  }

  // Méthode pour vérifier si la route est active
  isActive(route: string): boolean {
    return this.router.isActive(route, true);  // 'true' inclut les sous-routes
  }
}
