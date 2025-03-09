import { Component } from '@angular/core';

@Component({
  selector: 'app-upload',
  standalone: true,
  template: `
    <div class="upload-container">
      <h1>Upload new application</h1>
      
      <div class="upload-area">
        <div class="upload-icon">⬆️</div>
        <button class="choose-file">Choose file<span class="file-type">.zip</span></button>
        <div class="separator">OR</div>
        <div class="git-clone">
          <p>clone here the Git repository :</p>
          <div class="git-command">
            git clone https://github.com/username/repository.git
          </div>
        </div>
      </div>
    </div>
  `,
  styleUrl: './upload.component.css'
})
export class UploadComponent {}
