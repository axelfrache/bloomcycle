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
                <button (click)="navigateToDetails(app.id)" class="bloom-button bg-slate-600 hover:bg-slate-700">
                  <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="size-[1em]">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M2.036 12.322a1.012 1.012 0 0 1 0-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.963-7.178Z" />
                    <path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z" />
                  </svg>
                  VIEW
                </button>

                <button (click)="startProject(app.id)"
                        [disabled]="app.status === 'RUNNING'"
                        class="bloom-button bg-emerald-600"
                        [ngClass]="{
                          'hover:bg-emerald-700': app.status !== 'RUNNING',
                          'opacity-25 cursor-not-allowed grayscale brightness-75': app.status === 'RUNNING'
                        }">
                  <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="size-[1em]">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M5.25 5.653c0-.856.917-1.398 1.667-.986l11.54 6.347a1.125 1.125 0 0 1 0 1.972l-11.54 6.347a1.125 1.125 0 0 1-1.667-.986V5.653Z" />
                  </svg>
                  START
                </button>

                <button (click)="restartProject(app.id)"
                        [disabled]="app.status === 'STOPPED'"
                        class="bloom-button bg-sky-600"
                        [ngClass]="{
                          'hover:bg-sky-700': app.status !== 'STOPPED',
                          'opacity-25 cursor-not-allowed grayscale brightness-75': app.status === 'STOPPED'
                        }">
                  <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="size-[1em]">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M16.023 9.348h4.992v-.001M2.985 19.644v-4.992m0 0h4.992m-4.993 0 3.181 3.183a8.25 8.25 0 0 0 13.803-3.7M4.031 9.865a8.25 8.25 0 0 1 13.803-3.7l3.181 3.182m0-4.991v4.99" />
                  </svg>
                  RESTART
                </button>

                <button (click)="stopProject(app.id)"
                        [disabled]="app.status !== 'RUNNING'"
                        class="bloom-button bg-amber-600"
                        [ngClass]="{
                          'hover:bg-amber-700': app.status === 'RUNNING',
                          'opacity-25 cursor-not-allowed grayscale brightness-75': app.status !== 'RUNNING'
                        }">
                  <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="size-[1em]">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M5.25 7.5A2.25 2.25 0 0 1 7.5 5.25h9a2.25 2.25 0 0 1 2.25 2.25v9a2.25 2.25 0 0 1-2.25 2.25h-9a2.25 2.25 0 0 1-2.25-2.25v-9Z" />
                  </svg>
                  STOP
                </button>

                <button (click)="deleteProject(app.id)" class="bloom-button bg-rose-600 hover:bg-rose-700">
                  <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="size-[1em]">
                    <path stroke-linecap="round" stroke-linejoin="round" d="m14.74 9-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673a2.25 2.25 0 0 1-2.244 2.077H8.084a2.25 2.25 0 0 1-2.244-2.077L4.772 5.79m14.456 0a48.108 48.108 0 0 0-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 0 1 3.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 0 0-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 0 0-7.5 0" />
                  </svg>
                  DELETE
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

  deleteProject(id: string): void {
    if (confirm('Êtes-vous sûr de vouloir supprimer ce projet ? Cette action est irréversible.')) {
      this.isLoading = true;
      this.error = null;

      this.projectService.deleteProject(id).subscribe({
        next: () => {
          this.applications = this.applications.filter(app => app.id !== id);
          this.isLoading = false;
        },
        error: (err) => {
          this.error = err.message || `Échec de la suppression du projet ${id}`;
          this.isLoading = false;
        }
      });
    }
  }

  navigateToDetails(id: string): void {
    this.router.navigate(['/projects', id]);
  }
}
