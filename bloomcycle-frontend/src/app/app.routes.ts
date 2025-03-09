import { Routes } from '@angular/router';
import { LoginComponent } from './features/login/login.component';
import { UploadComponent } from './features/upload/upload.component';
import { HomeComponent } from './features/home/home.component';

export const routes: Routes = [
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'home', component: HomeComponent },
  { path: 'upload', component: UploadComponent }
];
