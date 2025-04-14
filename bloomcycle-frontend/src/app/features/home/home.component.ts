import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../core/services/auth.service';
import { ProjectService } from '../../core/services/project.service';
import { Router } from '@angular/router';

interface Application {
  id: string;
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

      <div *ngIf="isLoading" class="flex justify-center items-center h-64">
        <span class="loading loading-spinner loading-lg text-primary"></span>
      </div>

      <div *ngIf="error" class="alert alert-error mb-6 max-w-2xl mx-auto text-center">{{ error }}</div>

      <div *ngIf="!isLoading && applications.length > 0" class="overflow-x-auto">
        <table class="bloom-table border-collapse">
          <thead>
            <tr class="border-b border-gray-200">
              <th class="py-3 px-4 text-left font-medium text-gray-700">Application</th>
              <th class="py-3 px-4 text-left font-medium text-gray-700">Status</th>
              <th class="py-3 px-4 text-left font-medium text-gray-700">Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let app of applications" class="border-b border-gray-200">
              <td class="py-3 px-4 font-medium">{{app.name}}</td>
              <td class="py-3 px-4">
                <div [ngClass]="{
                  'status-running': app.status === 'RUNNING',
                  'status-stopped': app.status === 'STOPPED',
                  'status-crashed': app.status === 'CRASHED'
                }" class="py-1 px-3 rounded-full inline-block text-center font-medium">
                  {{app.status}}
                </div>
              </td>
              <td class="py-3 px-4">
                <div class="flex flex-wrap gap-2">
                  <button (click)="navigateToDetails(app.id)" class="bloom-button">
                    <i class="ph ph-eye"></i>
                    <span>VIEW</span>
                  </button>
                  <ng-container *ngIf="app.status === 'RUNNING'">
                    <button (click)="restartProject(app.id)" class="bloom-button">
                      <i class="ph ph-arrow-clockwise"></i>
                      <span>RESTART</span>
                    </button>
                    <button (click)="stopProject(app.id)" class="bloom-button">
                      <i class="ph ph-stop-circle"></i>
                      <span>STOP</span>
                    </button>
                  </ng-container>
                  <ng-container *ngIf="app.status === 'STOPPED'">
                    <button (click)="startProject(app.id)" class="bloom-button">
                      <i class="ph-fill ph-play"></i>
                      <span>START</span>
                    </button>
                  </ng-container>
                  <ng-container *ngIf="app.status === 'CRASHED'">
                    <button (click)="restartProject(app.id)" class="bloom-button">
                      <i class="ph ph-arrow-clockwise"></i>
                      <span>RESTART</span>
                    </button>
                  </ng-container>
                  <button class="bloom-button">
                    <i class="ph ph-trash"></i>
                    <span>DELETE</span>
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div *ngIf="!isLoading && applications.length === 0" class="flex justify-center items-center h-64 text-gray-500">
        No projects found. Create one to get started.
      </div>
    </div>
  `,
  styleUrl: './home.component.css'
})
export class HomeComponent {
  applications: Application[] = [];
  isLoading = false;
  error: string | null = null;

  constructor(
    private projectService: ProjectService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadProjects();
  }

  loadProjects(): void {
    this.isLoading = true;
    this.error = null;

    this.projectService.getProjects().subscribe({
      next: (projects) => {
        this.applications = projects.map(project => ({
          id: project.id,
          name: project.name,
          status: project.containerStatus as 'RUNNING' | 'STOPPED' | 'CRASHED'
        }));
        this.isLoading = false;
      },
      error: (err) => {
        this.error = err.message || 'Failed to load projects';
        this.isLoading = false;
      }
    });
  }

  startProject(id: string): void {
    this.isLoading = true;
    this.error = null;

    this.projectService.startProject(id).subscribe({
      next: () => {
        const project = this.applications.find(app => app.id === id);
        if (project) {
          project.status = 'RUNNING';
        }
        this.isLoading = false;
      },
      error: (err) => {
        this.error = err.message || `Failed to start project ${id}`;
        this.isLoading = false;
        this.loadProjects();
      }
    });
  }

  stopProject(id: string): void {
    this.isLoading = true;
    this.error = null;

    this.projectService.stopProject(id).subscribe({
      next: () => {
        const project = this.applications.find(app => app.id === id);
        if (project) {
          project.status = 'STOPPED';
        }
        this.isLoading = false;
      },
      error: (err) => {
        this.error = err.message || `Failed to stop project ${id}`;
        this.isLoading = false;
        this.loadProjects();
      }
    });
  }

  restartProject(id: string): void {
    this.isLoading = true;
    this.error = null;

    this.projectService.restartProject(id).subscribe({
      next: () => {
        const project = this.applications.find(app => app.id === id);
        if (project) {
          project.status = 'RUNNING';
        }
        this.isLoading = false;
      },
      error: (err) => {
        this.error = err.message || `Failed to restart project ${id}`;
        this.isLoading = false;
        this.loadProjects();
      }
    });
  }

  navigateToDetails(id: string): void {
    this.router.navigate(['/projects', id]);
  }
}

