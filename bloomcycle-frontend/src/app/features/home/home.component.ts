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
    <div class="max-w-screen-xl" style="margin: 0 auto; padding: 40px 20px;">
      <h1 class="text-2xl font-semibold" style="margin-bottom: 40px;">Your Projects</h1>

      <table class="w-full bg-white rounded-lg overflow-hidden">
        <thead>
        <tr>
          <th class="text-left font-bold bg-gray-100 border-2 border-solid border-gray-200" style="padding: 16px;">Application</th>
          <th class="text-left font-bold bg-gray-100 border-2 border-solid border-gray-200" style="padding: 16px;">Status</th>
          <th class="text-left font-bold bg-gray-100 border-2 border-solid border-gray-200" style="padding: 16px;">Actions</th>
        </tr>
        </thead>
        <tbody>
        <tr *ngFor="let app of applications">
          <td class="border-2 border-solid border-gray-200" style="padding: 16px;">{{app.name}}</td>
          <td [ngClass]="{'bg-green-200': app.status === 'RUNNING', 'bg-yellow-200': app.status === 'STOPPED', 'bg-red-200': app.status === 'CRASHED'}"
              class="text-left rounded-md border-2 border-solid border-gray-200" style="padding: 8px;">
            {{app.status}}
          </td>
          <td class="flex gap-3 border-2 border-solid border-gray-200" style="padding: 16px;">
            <button class="flex items-center gap-2 text-gray-600 hover:bg-gray-200 p-2 rounded-md" style="padding: 6px 12px;">
              <i class="ph ph-eye"></i>
              <span class="text-xs">VIEW DETAILS</span>
            </button>
            <ng-container *ngIf="app.status === 'RUNNING'">
              <button class="flex items-center gap-2 text-gray-600 hover:bg-gray-200 p-2 rounded-md" style="padding: 6px 12px;">
                <i class="ph ph-arrow-clockwise text-green-600"></i>
                <span class="text-xs">RESTART</span>
              </button>
              <button class="flex items-center gap-2 text-gray-600 hover:bg-gray-200 p-2 rounded-md" style="padding: 6px 12px;">
                <i class="ph ph-stop-circle text-red-500"></i>
                <span class="text-xs">STOP</span>
              </button>
            </ng-container>
            <ng-container *ngIf="app.status === 'STOPPED'">
              <button class="flex items-center gap-2 text-gray-600 hover:bg-gray-200 p-2 rounded-md" style="padding: 6px 12px;">
                <i class="ph-fill ph-play text-blue-800"></i>
                <span class="text-xs">START</span>
              </button>
            </ng-container>
            <ng-container *ngIf="app.status === 'CRASHED'">
              <button class="flex items-center gap-2 text-gray-600 hover:bg-gray-200 p-2 rounded-md" style="padding: 6px 12px;">
                <i class="ph ph-arrow-clockwise text-green-600"></i>
                <span class="text-xs">RESTART</span>
              </button>
            </ng-container>
            <button class="flex items-center gap-2 text-gray-600 hover:bg-gray-200 p-2 rounded-md" style="padding: 6px 12px;">
              <i class="ph ph-trash"></i>
              <span class="text-xs">DELETE</span>
            </button>
          </td>
        </tr>
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
}
