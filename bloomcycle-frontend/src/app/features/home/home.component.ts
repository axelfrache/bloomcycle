import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../core/services/auth.service';

interface Application {
  name: string;
  status: 'RUNNING' | 'STOPPED' | 'CRASHED';
}

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule],

  template: `
    <div class="home-container">

      <button class="logout-btn" (click)="logout()">Logout</button>

      <h1>Your Projects</h1>

      <table class="applications-table">
        <thead>
          <tr>
            <th>Application</th>
            <th>Status</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          @for (app of applications; track app.name) {
            <tr>
              <td>{{app.name}}</td>
              <td [class]="'status-' + app.status.toLowerCase()">{{app.status}}</td>
              <td class="actions">
                <button class="action-btn" title="View Details">
                  <img src="assets/view-icon.png" alt="view">
                  <span class="action-label">VIEW DETAILS</span>
                </button>
                @if (app.status === 'RUNNING') {
                  <button class="action-btn" title="Restart">
                    <img src="assets/restart-icon.png" alt="restart">
                    <span class="action-label">RESTART</span>
                  </button>
                  <button class="action-btn" title="Stop">
                    <img src="assets/stop-icon.png" alt="stop">
                    <span class="action-label">STOP</span>
                  </button>
                } @else if (app.status === 'STOPPED') {
                  <button class="action-btn" title="Start">
                    <img src="assets/start-icon.png" alt="start">
                    <span class="action-label">START</span>
                  </button>
                } @else if (app.status === 'CRASHED') {
                  <button class="action-btn" title="Restart">
                    <img src="assets/restart-icon.png" alt="restart">
                    <span class="action-label">RESTART</span>
                  </button>
                }
                <button class="action-btn" title="Delete">
                  <img src="assets/delete-icon.png" alt="delete">
                  <span class="action-label">DELETE</span>
                </button>
              </td>
            </tr>
          }
        </tbody>
      </table>
    </div>
  `,
  styleUrl: './home.component.css'
})
export class HomeComponent {
  applications: Application[] = [
    { name: 'APP1', status: 'RUNNING' },
    { name: 'APP2', status: 'STOPPED' },
    { name: 'APP3', status: 'CRASHED' },
    { name: 'APP4', status: 'RUNNING' },
    { name: 'APP5', status: 'RUNNING' },
    { name: 'APP6', status: 'STOPPED' },
    { name: 'APP7', status: 'RUNNING' }
  ];

  constructor(private authService: AuthService) {}

  logout(): void {
    this.authService.logout();
  }
}
