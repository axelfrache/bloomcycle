import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DetailsComponent } from './details.component';
import { RouterModule } from '@angular/router';

@NgModule({
  declarations: [DetailsComponent],
  imports: [
    CommonModule,
    RouterModule
  ],
  exports: [DetailsComponent]
})
export class DetailsModule {}
