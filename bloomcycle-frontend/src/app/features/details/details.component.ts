import { Component, OnInit } from '@angular/core';
import { CommonModule} from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { ProjectService } from '../../core/services/project.service';
import { catchError, finalize, tap } from 'rxjs/operators';
import { of } from 'rxjs';
import {Project} from '../../core/models/project.model';

@Component({
  selector: 'app-details',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="flex flex-col items-center w-full bg-base-500">
      <div class="w-full max-w-screen-xl mx-auto p-6">
        <div class="flex items-center gap-4 mb-6">
          <a routerLink="/home" class="flex items-center gap-2">
            <i class="ph ph-arrow-left"></i>
            <span>Back to projects</span>
          </a>
        </div>

        <div *ngIf="isLoading" class="flex justify-center items-center h-64">
          <span class="loading loading-spinner loading-lg text-primary"></span>
        </div>

        <div *ngIf="error" class="alert alert-error mb-6 max-w-2xl mx-auto text-center">{{ error }}</div>

        <ng-container *ngIf="!isLoading && !error && project">
          <div class="flex flex-col items-center mb-6">
            <div class="flex justify-between items-center w-full max-w-4xl mb-6">
              <h1 class="text-2xl font-semibold">{{ project.name }}</h1>
              <div class="flex gap-3">
                <ng-container *ngIf="project.containerStatus === 'RUNNING'">
                  <button (click)="toggleAutoRestart()" class="btn btn-sm gap-2"
                          [class.btn-info]="project.autoRestartEnabled"
                          [class.btn-outline-primary]="!project.autoRestartEnabled">
                    <i class="ph ph-arrows-clockwise"></i>
                    <span [class.text-info]="!project.autoRestartEnabled">
                      {{ project.autoRestartEnabled ? 'Désactiver auto-restart' : 'Activer auto-restart' }}
                    </span>
                  </button>
                  <button (click)="restartProject()"
                          class="bloom-button bg-sky-600"
                          [ngClass]="{'hover:bg-sky-700': true}">
                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="size-[1em]">
                      <path stroke-linecap="round" stroke-linejoin="round" d="M16.023 9.348h4.992v-.001M2.985 19.644v-4.992m0 0h4.992m-4.993 0 3.181 3.183a8.25 8.25 0 0 0 13.803-3.7M4.031 9.865a8.25 8.25 0 0 1 13.803-3.7l3.181 3.182m0-4.991v4.99" />
                    </svg>
                    RESTART
                  </button>
                  <button (click)="stopProject()"
                          class="bloom-button bg-amber-600"
                          [ngClass]="{'hover:bg-amber-700': true}">
                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="size-[1em]">
                      <path stroke-linecap="round" stroke-linejoin="round" d="M5.25 7.5A2.25 2.25 0 0 1 7.5 5.25h9a2.25 2.25 0 0 1 2.25 2.25v9a2.25 2.25 0 0 1-2.25 2.25h-9a2.25 2.25 0 0 1-2.25-2.25v-9Z" />
                    </svg>
                    STOP
                  </button>
                </ng-container>
                <ng-container *ngIf="project.containerStatus === 'STOPPED'">
                  <button (click)="startProject()"
                          class="bloom-button bg-emerald-600"
                          [ngClass]="{'hover:bg-emerald-700': true}">
                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="size-[1em]">
                      <path stroke-linecap="round" stroke-linejoin="round" d="M5.25 5.653c0-.856.917-1.398 1.667-.986l11.54 6.347a1.125 1.125 0 0 1 0 1.972l-11.54 6.347a1.125 1.125 0 0 1-1.667-.986V5.653Z" />
                    </svg>
                    START
                  </button>
                </ng-container>
                <ng-container *ngIf="project.containerStatus === 'CRASHED'">
                  <button (click)="restartProject()"
                          class="bloom-button bg-sky-600"
                          [ngClass]="{'hover:bg-sky-700': true}">
                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="size-[1em]">
                      <path stroke-linecap="round" stroke-linejoin="round" d="M16.023 9.348h4.992v-.001M2.985 19.644v-4.992m0 0h4.992m-4.993 0 3.181 3.183a8.25 8.25 0 0 0 13.803-3.7M4.031 9.865a8.25 8.25 0 0 1 13.803-3.7l3.181 3.182m0-4.991v4.99" />
                    </svg>
                    RESTART
                  </button>
                </ng-container>
              </div>
            </div>

            <div class="grid grid-cols-1 md:grid-cols-2 gap-6 mb-8 w-full max-w-4xl">
              <div class="card bg-base-500 shadow-sm">
                <div class="card-body">
                  <h2 class="card-title">Project Information</h2>
                  <div class="space-y-3">
                    <div class="flex justify-between">
                      <span class="text-gray-600">ID:</span>
                      <span>{{ project.id }}</span>
                    </div>
                    <div class="flex justify-between">
                      <span class="text-gray-600">Status:</span>
                      <span [ngClass]="{
                    'text-success': project.containerStatus === 'RUNNING',
                    'text-warning': project.containerStatus === 'STOPPED',
                    'text-error': project.containerStatus === 'CRASHED'
                  }">{{ project.containerStatus }}</span>
                    </div>
                  </div>
                </div>
              </div>

              <div class="card bg-base-500 shadow-sm">
                <div class="card-body">
                  <h2 class="card-title">Resource Usage</h2>
                  <div class="space-y-3">
                    <div class="flex justify-between">
                      <span class="text-gray-600">CPU:</span>
                      <span>{{ project.cpuUsage || 'N/A' }}</span>
                    </div>
                    <div class="flex justify-between">
                      <span class="text-gray-600">Memory:</span>
                      <span>{{ project.memoryUsage || 'N/A' }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div *ngIf="serverUrl" class="card bg-base-500 shadow-sm mb-8 w-full max-w-4xl">
              <div class="card-body">
                <h2 class="card-title">Server URL</h2>
                <div class="flex items-center justify-between">
                  <a [href]="serverUrl" target="_blank" class="link link-primary break-all">{{ serverUrl }}</a>
                  <button class="btn btn-sm btn-outline" (click)="copyToClipboard(serverUrl)">
                    <i class="ph ph-copy"></i>
                  </button>
                </div>
              </div>
            </div>

            <div class="card bg-base-500 shadow-sm mb-8 w-full max-w-4xl">
              <div class="card-body">
                <div class="flex justify-between items-center mb-4">
                  <h2 class="card-title">Logs</h2>
                  <button (click)="loadProjectDetails(project.id)"
                          class="bloom-button bg-slate-600"
                          [ngClass]="{'hover:bg-slate-700': true}">
                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="size-[1em]">
                      <path stroke-linecap="round" stroke-linejoin="round" d="M16.023 9.348h4.992v-.001M2.985 19.644v-4.992m0 0h4.992m-4.993 0 3.181 3.183a8.25 8.25 0 0 0 13.803-3.7M4.031 9.865a8.25 8.25 0 0 1 13.803-3.7l3.181 3.182m0-4.991v4.99" />
                    </svg>
                    REFRESH
                  </button>
                </div>
                <pre class="bg-neutral text-neutral-content p-4 rounded-lg overflow-auto max-h-[500px] min-h-[400px]">{{ project.logs || 'No logs available' }}</pre>
              </div>
            </div>
          </div>
        </ng-container>
      </div>
    </div>
  `,
  styleUrl: './details.component.css'
})
export class DetailsComponent implements OnInit {
  project: any = null;
  isLoading = false;
  error: string | null = null;
  projectId: string | null = null;
  serverUrl: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private projectService: ProjectService
  ) {}

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      this.projectId = params.get('id');
      if (this.projectId) {
        this.loadProjectDetails(this.projectId);
      } else {
        this.error = 'Project ID not found';
      }
    });
  }

  loadProjectDetails(id: string): void {
    this.isLoading = true;
    this.error = null;

    this.projectService.getProjectDetails(id).pipe(
      tap(project => {
        this.project = project;
        this.serverUrl = project?.serverUrl || null;

        if (project.containerStatus === 'RUNNING') {
          this.loadProjectLogs(id);
        }
      }),
      catchError(err => {
        this.error = err.message || `Failed to load project details for ${id}`;
        return of(null);
      }),
      finalize(() => this.isLoading = false)
    ).subscribe();
  }

  loadProjectLogs(id: string): void {
    this.projectService.getProjectLogs(id).pipe(
      tap((response: Project | null) => {
        if (this.project) {
          this.project.logs = response?.logs || 'No logs available';
        }
      }),
      catchError(err => {
        console.error('Failed to load logs:', err);
        return of(null);
      })
    ).subscribe();
  }

  startProject(): void {
    if (!this.projectId) return;

    this.isLoading = true;
    this.error = null;

    this.projectService.startProject(this.projectId).pipe(
      tap(() => {
        if (this.project) {
          this.project.containerStatus = 'RUNNING';
          setTimeout(() => this.loadProjectLogs(this.projectId!), 2000);
        }
      }),
      catchError(err => {
        this.error = err.message || 'Failed to start project';
        if (this.projectId) this.loadProjectDetails(this.projectId);
        return of(null);
      }),
      finalize(() => {
        this.isLoading = false;
        if (this.projectId) this.loadProjectDetails(this.projectId);
      })
    ).subscribe();
  }

  stopProject(): void {
    if (!this.projectId) return;

    this.isLoading = true;
    this.error = null;

    this.projectService.stopProject(this.projectId).pipe(
      tap(() => {
        if (this.project) {
          this.project.containerStatus = 'STOPPED';
        }
      }),
      catchError(err => {
        this.error = err.message || 'Failed to stop project';
        if (this.projectId) this.loadProjectDetails(this.projectId);
        return of(null);
      }),
      finalize(() => this.isLoading = false)
    ).subscribe();
  }

  restartProject(): void {
    if (!this.projectId) return;

    this.isLoading = true;
    this.error = null;

    this.projectService.restartProject(this.projectId).pipe(
      tap(() => {
        if (this.project) {
          this.project.containerStatus = 'RUNNING';
          setTimeout(() => this.loadProjectLogs(this.projectId!), 2000);
        }
      }),
      catchError(err => {
        this.error = err.message || 'Failed to restart project';
        if (this.projectId) this.loadProjectDetails(this.projectId);
        return of(null);
      }),
      finalize(() => this.isLoading = false)
    ).subscribe();
  }

  toggleAutoRestart(): void {
    if (!this.projectId) return;

    this.isLoading = true;
    this.error = null;

    // Inverser l'état actuel
    const newState = !this.project.autoRestartEnabled;

    this.projectService.autoRestartProject(this.projectId, newState).pipe(
      tap((response) => {
        if (this.project) {
          this.project.autoRestartEnabled = response.autoRestartEnabled;
        }
      }),
      catchError(err => {
        this.error = err.message || 'Échec de configuration auto-restart';
        if (this.projectId) this.loadProjectDetails(this.projectId);
        return of(null);
      }),
      finalize(() => {
        this.isLoading = false;
        if (this.projectId) this.loadProjectDetails(this.projectId);
      })
    ).subscribe();
  }

  copyToClipboard(text: string | null): void {
    if (!text) return;
    navigator.clipboard.writeText(text)
      .then(() => {
        console.log('URL copiée dans le presse-papiers');
      })
      .catch(err => {
        console.error('Erreur lors de la copie :', err);
      });
  }
}
