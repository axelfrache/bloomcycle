import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LoginComponent } from './login.component';

@NgModule({
  imports: [
    CommonModule,
    LoginComponent
  ],
  exports: [LoginComponent]
})
export class LoginModule {}
