import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-upload',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="max-w-3xl" style="margin: 0 auto; padding: 40px 20px;">
      <h1 class=" text-2xl font-semibold" style="margin-bottom: 40px;">Upload new application</h1>

      <div *ngIf="isLoading" class="flex justify-center items-center h-64">
        <span class="loading loading-spinner loading-lg text-primary"></span>
      </div>

      <div *ngIf="error" class="alert alert-error mb-6 max-w-2xl mx-auto text-center">{{ error }}</div>

      <div *ngIf="!isLoading" class="border-2 border-dashed border-gray-300 rounded-lg flex flex-col items-center gap-5" style="padding: 40px;">
        <i class="ph ph-cloud-arrow-up text-8xl"></i>
        <input type="file" class="file-input" (change)="onFileSelected($event)" />
        <div class="text-gray-600" style="margin: 10px 0;">OR</div>
        <div class="mockup-code w-full rounded-xl" style="padding: 10px;">
          <pre data-prefix="$"><code>git clone https://github.com/username/repository.git</code></pre>
        </div>

        <button
          *ngIf="selectedFile"
          (click)="uploadFile()"
          class="btn btn-primary mt-4"
          [disabled]="isUploading">
          <span *ngIf="isUploading" class="loading loading-spinner loading-sm mr-2"></span>
          {{ isUploading ? 'Uploading...' : 'Upload Application' }}
        </button>
      </div>
    </div>
  `,
})
export class UploadComponent {
  isLoading = false;
  isUploading = false;
  error: string | null = null;
  selectedFile: File | null = null;

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.selectedFile = input.files[0];
    }
  }

  uploadFile(): void {
    if (!this.selectedFile) return;

    this.isUploading = true;
    this.error = null;

    setTimeout(() => {
      this.isUploading = false;
      if (this.selectedFile) {
        console.log('File uploaded:', this.selectedFile.name);
      }
      this.selectedFile = null;
    }, 2000);
  }
}

