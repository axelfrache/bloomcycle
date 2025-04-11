import { Component } from '@angular/core';

@Component({
  selector: 'app-upload',
  standalone: true,
  template: `
    <div class="max-w-3xl" style="margin: 0 auto; padding: 40px 20px;">
      <h1 class=" text-2xl font-semibold" style="margin-bottom: 40px;">Upload new application</h1>

      <div class="border-2 border-dashed border-gray-300 rounded-lg flex flex-col items-center gap-5" style="padding: 40px;">
        <i class="ph ph-cloud-arrow-up text-8xl"></i>
<!--        <button class="bg-white border border-gray-300 rounded-md" style="padding: 8px 16px;">-->
<!--          Choose file-->
<!--          <span class="text-gray-600" style="margin-left: 4px;">.zip</span>-->
<!--        </button>-->
        <input type="file" class="file-input" />
        <div class="text-gray-600" style="margin: 10px 0;">OR</div>
<!--        <div class="text-center">-->
<!--          <p class="text-gray-600">Clone here the Git repository:</p>-->
<!--          <div class="bg-gray-800 text-white rounded-md font-mono" style="padding: 12px 20px; margin-top: 10px;">-->
<!--            git clone https://github.com/username/repository.git-->
<!--          </div>-->
<!--        </div>-->
        <div class="mockup-code w-full rounded-xl" style="padding: 10px;">
          <pre data-prefix="$"><code>git clone https://github.com/username/repository.git</code></pre>
        </div>
      </div>
    </div>
  `,
})
export class UploadComponent {}
