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
      <nav class="flex flex-col md:flex-row justify-between items-center max-w-screen-xl" style="margin: 0 auto;">
        <div class="flex items-center justify-between w-full md:w-auto">
          <a routerLink="/home" class="flex items-center gap-2">
            <img src="assets/logo.png" alt="BloomCycle" class="h-10 md:h-12">
            <span class="font-serif text-xl font-bold text-gray-800">BloomCycle</span>
          </a>
          <button (click)="toggleMobileMenu()" class="md:hidden text-gray-800">
            <i class="ph" [ngClass]="{'ph-list': !isMobileMenuOpen, 'ph-x': isMobileMenuOpen}"></i>
          </button>
        </div>

        <div [ngClass]="{'hidden md:flex': !isMobileMenuOpen, 'flex': isMobileMenuOpen}"
             class="flex-col md:flex-row w-full md:w-auto gap-4 md:gap-8 mt-4 md:mt-0 transition-all duration-300">
          <a routerLink="/home" routerLinkActive="active"
             [ngClass]="{'bg-[#f5f0e6] border border-gray-800 border-b-0 font-bold': isActive('/home') && !isMobileMenuOpen}"
             class="flex items-center gap-2 text-gray-800 rounded-t-xl hover:bg-gray-100 px-4 py-2 md:px-5 md:py-2.5">
            <i class="ph ph-eye text-3xl md:text-5xl"></i>
            <span class="text-sm md:text-base">YOUR APPS</span>
          </a>
          <a routerLink="/upload" routerLinkActive="active"
             [ngClass]="{'bg-[#f5f0e6] border border-gray-800 border-b-0 font-bold': isActive('/upload') && !isMobileMenuOpen}"
             class="flex items-center gap-2 text-gray-800 rounded-t-xl hover:bg-gray-100 px-4 py-2 md:px-5 md:py-2.5">
            <i class="ph ph-cloud-arrow-up text-3xl md:text-5xl"></i>
            <span class="text-sm md:text-base">UPLOAD NEW APP</span>
          </a>
          <a (click)="logout()" class="cursor-pointer flex items-center gap-2 text-gray-800 rounded-t-xl hover:bg-gray-100 px-4 py-2 md:px-5 md:py-2.5">
            <i class="ph ph-sign-out text-3xl md:text-5xl"></i>
            <span class="text-sm md:text-base">LOGOUT</span>
          </a>
        </div>
      </nav>
    </header>
    <main class="min-h-screen bg-white p-4 md:p-10" style="padding-top: 20px; padding-bottom: 40px;">
      <router-outlet></router-outlet>
    </main>
  `
})
export class AppComponent {
  title = 'bloomcycle';
  hasToken$: Observable<boolean>;
  isMobileMenuOpen = false;

  constructor(private router: Router, private authService: AuthService) {
    this.hasToken$ = this.authService.isAuthenticated();
  }

  logout(): void {
    this.authService.logout();
    this.isMobileMenuOpen = false;
    this.router.navigate(['/login']);
  }

  toggleMobileMenu(): void {
    this.isMobileMenuOpen = !this.isMobileMenuOpen;
  }

  isActive(route: string): boolean {
    return this.router.isActive(route, true);
  }
}
