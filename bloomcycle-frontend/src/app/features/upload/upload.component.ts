import { Component } from '@angular/core';
import { ProjectService } from '../../core/services/project.service';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-upload',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="max-w-3xl" style="margin: 0 auto; padding: 40px 20px;">
      <h1 class=" text-2xl font-semibold" style="margin-bottom: 40px;">Upload new application</h1>
      <div class="border-2 border-dashed border-gray-300 rounded-lg flex flex-col items-center gap-5" style="padding: 40px;">
        <i class="ph ph-cloud-arrow-up text-8xl"></i>
        <input type="file" class="file-input" (change)="onFileSelected($event)" accept=".zip">
        <div class="text-gray-600" style="margin: 10px 0;">OR</div>
        <div class="text-center">
          <p class="text-gray-600">Clone here the Git repository:</p>
          <div class="bg-gray-800 text-white rounded-md font-mono" style="padding: 12px 20px; margin-top: 10px;">
            git clone https://github.com/username/repository.git
          </div>
        </div>
        <button
          class="btn btn-primary"
          [disabled]="!file"
          (click)="uploadFile()">
          Valider l'upload
        </button>

        <div *ngIf="error" class="text-red-500 mt-2">
          {{ error }}
        </div>

        <div *ngIf="uploadSuccess" class="text-green-500 mt-2">
          Upload completed successfully!
        </div>
      </div>
    </div>
  `
})
export class UploadComponent {
  file: File | null = null;
  error: string | null = null;
  uploadSuccess = false;
  projectName: string = '';

  constructor(private projectService: ProjectService, private router: Router) {}

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.file = input.files[0];
      const fileNameWithoutExtension = this.file.name.replace('.zip', '');
      this.projectName = fileNameWithoutExtension;
    }
  }

  uploadFile(): void {
    if (!this.file || !this.projectName) return;

    const formData = new FormData();
    formData.append('file', this.file, this.file.name);
    formData.append('projectName', this.projectName);  // Ajoute le nom du projet à la requête

    this.projectService.createProjectFromZip(formData).subscribe({
      next: (project) => {
        this.uploadSuccess = true;
        this.router.navigate(['/home']); // Redirection après succès
      },
      error: (error: any) => {
        this.error = error.error?.message || 'An error occurred during upload';
      }
    });
  }
}
