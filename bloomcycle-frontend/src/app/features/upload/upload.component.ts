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
    <div class="flex flex-col items-center py-10 px-6 min-h-[80vh]">
      <h1 class="text-3xl font-bold text-gray-800 mb-8">Upload new application</h1>

      <div class="flex justify-center mb-8">
        <svg xmlns="http://www.w3.org/2000/svg" class="w-20 h-20 text-[#6B7F94]" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
          <polyline points="17 8 12 3 7 8"></polyline>
          <line x1="12" y1="3" x2="12" y2="15"></line>
        </svg>
      </div>

      <div class="w-full max-w-2xl bg-white rounded-lg shadow-md border border-gray-200 mb-8">
        <div class="p-8">
          <div class="mb-8">
            <label class="block text-lg text-gray-700 font-semibold mb-3">
              Project name: <span class="text-red-500">*</span>
            </label>
            <input
              type="text"
              [(ngModel)]="projectName"
              placeholder="My Project"
              class="bloom-input w-full"
              required
            >
          </div>

          <div class="mb-8">
            <label class="block text-lg text-gray-700 font-semibold mb-3">Upload project file:</label>
            <div class="flex items-center">
              <label class="bloom-button">
                <i class="ph ph-file"></i>
                <span>Choisir un fichier</span>
                <input
                  type="file"
                  class="hidden"
                  (change)="onFileSelected($event)"
                  accept=".zip"
                  [disabled]="gitUrl"
                />
              </label>
              <span class="ml-4 text-gray-500 truncate flex-1">{{ file ? file.name : 'Aucun fichier choisi' }}</span>
            </div>
          </div>

          <div class="flex items-center py-4 my-8">
            <div class="flex-grow h-px bg-gray-200"></div>
            <div class="px-8 text-lg text-gray-500 font-medium">OR</div>
            <div class="flex-grow h-px bg-gray-200"></div>
          </div>

          <div class="mb-8">
            <label class="block text-lg text-gray-700 font-semibold mb-3">Enter the Git repository URL:</label>
            <input
              type="text"
              [(ngModel)]="gitUrl"
              [disabled]="!!file"
              placeholder="https://github.com/username/repository.git"
              class="bloom-input w-full"
            >
          </div>
        </div>

        <div class="flex justify-center py-6 border-t border-gray-200">
          <button
            class="bloom-button"
            [disabled]="!isValidSubmission()"
            [ngClass]="{
              'opacity-100': isValidSubmission(),
              'opacity-50 cursor-not-allowed': !isValidSubmission()
            }"
            (click)="submit()">
            <i class="ph ph-check"></i>
            <span>Valider l'upload</span>
          </button>
        </div>
      </div>

      <div *ngIf="isLoading" class="flex justify-center items-center h-12 mt-6">
        <span class="loading loading-spinner loading-lg text-[#6B7F94]"></span>
      </div>

      <div *ngIf="error" class="mt-6 p-4 bg-red-50 text-red-600 text-center rounded-md max-w-2xl w-full border border-red-100">
        {{ error }}
      </div>

      <div *ngIf="uploadSuccess" class="mt-6 p-4 bg-green-50 text-green-600 text-center rounded-md max-w-2xl w-full border border-green-100">
        Upload completed successfully!
      </div>
    </div>
  `
})
export class UploadComponent {
  file: File | null = null;
  error: string | null = null;
  uploadSuccess = false;
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
        this.uploadSuccess = true;
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
        this.uploadSuccess = true;
        setTimeout(() => this.router.navigate(['/home']), 1000);
      },
      error: (error: any) => {
        this.isLoading = false;
        this.error = error.error?.message || 'An error occurred while processing the Git repository';
      }
    });
  }
}
