import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { UploadComponent } from './upload.component';

@NgModule({
  imports: [
    CommonModule,
    UploadComponent
  ],
  exports: [UploadComponent]
})
export class UploadModule {}
