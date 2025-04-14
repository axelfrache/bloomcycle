import { Component } from '@angular/core';
import { ProjectService } from '../../core/services/project.service';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-upload',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="max-w-3xl" style="margin: 0 auto; padding: 40px 20px;">
      <h1 class=" text-2xl font-semibold" style="margin-bottom: 40px;">Upload new application</h1>
      <div class="border-2 border-dashed border-gray-300 rounded-lg flex flex-col items-center gap-5" style="padding: 40px;">
        <i class="ph ph-cloud-arrow-up text-8xl"></i>

        <div class="w-full">
          <p class="text-gray-600 mb-2">Project name: <span class="text-red-500">*</span></p>
          <input
            type="text"
            [(ngModel)]="projectName"
            placeholder="My Project"
            class="w-full p-2 border rounded-md"
            required
          >
        </div>

        <input type="file" class="file-input" (change)="onFileSelected($event)" accept=".zip" [disabled]="gitUrl">
        <div class="text-gray-600" style="margin: 10px 0;">OR</div>
        <div class="w-full">
          <p class="text-gray-600">Enter the Git repository URL:</p>
          <input
            type="text"
            [(ngModel)]="gitUrl"
            [disabled]="!!file"
            placeholder="https://github.com/username/repository.git"
            class="w-full p-2 border rounded-md mt-2"
          >
        </div>
        <button
          class="btn btn-neutral border"
          [disabled]="!isValidSubmission()"
          [ngClass]="{
            'text-black': isValidSubmission(),
            'text-gray-400': !isValidSubmission()
          }"
          (click)="submit()">
          Valider l'upload
        </button>

        <div *ngIf="isLoading" class="flex justify-center items-center h-4">
          <span class="loading loading-spinner loading-lg text-primary"></span>
        </div>

        <div *ngIf="error" class="text-red-500 mt-2">
          {{ error }}
        </div>

        <div *ngIf="success" class="text-green-500 mt-2">
          {{ success }}
        </div>
      </div>
    </div>
  `
})
export class UploadComponent {
  file: File | null = null;
  error: string | null = null;
  success: string | null = null;
  projectName: string = '';
  gitUrl: string = '';
  isLoading = false;

  constructor(private projectService: ProjectService, private router: Router) {}

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.file = input.files[0];
    }
  }

  isValidSubmission(): boolean {
    return ((!!this.file && !this.gitUrl) || (!this.file && !!this.gitUrl)) && !!this.projectName;
  }

  submit(): void {
    if (this.file) {
      this.uploadFile();
    } else if (this.gitUrl) {
      this.uploadGitUrl();
    }
  }

  uploadFile(): void {
    if (!this.file || !this.projectName) return;

    this.isLoading = true;
    const formData = new FormData();
    formData.append('name', this.projectName);
    formData.append('sourceZip', this.file);

    this.projectService.createProject(formData).subscribe({
      next: (project) => {
        this.isLoading = false;
        this.success = 'Upload completed successfully!';
        setTimeout(() => this.router.navigate(['/home']), 1000);
      },
      error: (error: any) => {
        this.isLoading = false;
        this.error = error.error?.message || 'An error occurred during upload';
      }
    });
  }

  uploadGitUrl(): void {
    if (!this.gitUrl) return;

    this.isLoading = true;
    const formData = new FormData();
    formData.append('name', this.projectName);
    formData.append('gitUrl', this.gitUrl);
    this.projectService.createProject(formData).subscribe({
      next: (project) => {
        this.isLoading = false;
        this.success = 'Upload completed successfully!';
        setTimeout(() => this.router.navigate(['/home']), 1000);
      },
      error: (error: any) => {
        this.isLoading = false;
        this.error = error.error?.message || 'An error occurred while processing the Git repository';
      }
    });
  }
}
