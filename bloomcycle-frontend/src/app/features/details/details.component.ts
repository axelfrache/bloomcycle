import { Component, OnInit } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { ProjectService } from '../../core/services/project.service';
import { catchError, finalize, tap } from 'rxjs/operators';
import { Observable, of } from 'rxjs';

@Component({
  selector: 'app-details',
  standalone: true,
  imports: [CommonModule, RouterModule, DatePipe],
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
                  <button (click)="restartProject()" class="btn btn-success btn-sm gap-2">
                    <i class="ph ph-arrow-clockwise"></i>
                    Restart
                  </button>
                  <button (click)="stopProject()" class="btn btn-error btn-sm gap-2">
                    <i class="ph ph-stop-circle"></i>
                    Stop
                  </button>
                </ng-container>
                <ng-container *ngIf="project.containerStatus === 'STOPPED'">
                  <button (click)="startProject()" class="btn btn-success btn-sm gap-2">
                    <i class="ph-fill ph-play"></i>
                    Start
                  </button>
                </ng-container>
                <ng-container *ngIf="project.containerStatus === 'CRASHED'">
                  <button (click)="restartProject()" class="btn btn-success btn-sm gap-2">
                    <i class="ph ph-arrow-clockwise"></i>
                    Restart
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
                    <div class="flex justify-between">
                      <span class="text-gray-600">Created:</span>
                      <span>{{ project.createdAt | date:'medium' }}</span>
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

            <div class="card bg-base-500 shadow-sm mb-8 w-full max-w-4xl">
              <div class="card-body">
                <h2 class="card-title">Logs</h2>
                <pre class="bg-neutral text-neutral-content p-4 rounded-lg overflow-auto max-h-[300px]">{{ project.logs || 'No logs available' }}</pre>
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
      tap(project => this.project = project),
      catchError(err => {
        this.error = err.message || `Failed to load project details for ${id}`;
        return of(null);
      }),
      finalize(() => this.isLoading = false)
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
        }
      }),
      catchError(err => {
        this.error = err.message || 'Failed to start project';
        if (this.projectId) this.loadProjectDetails(this.projectId);
        return of(null);
      }),
      finalize(() => this.isLoading = false)
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
}
