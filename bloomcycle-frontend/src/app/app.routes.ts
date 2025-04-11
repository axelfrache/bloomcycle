import { Routes } from '@angular/router';
import { LoginComponent } from './features/login/login.component';
import { UploadComponent } from './features/upload/upload.component';
import { HomeComponent } from './features/home/home.component';
import {RegisterComponent} from './features/register/register.component';
import {DetailsComponent} from './features/details/details.component';
import {AuthGuard} from './core/guards/auth.guard';
import {GuestGuard} from './core/guards/guest.guard';

export const routes: Routes = [
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent, canActivate: [GuestGuard] },
  { path: 'home', component: HomeComponent, canActivate: [AuthGuard] },
  { path: 'upload', component: UploadComponent, canActivate: [AuthGuard] },
  { path: 'register', component: RegisterComponent, canActivate: [GuestGuard] },
  { path: 'projects/:id', component: DetailsComponent, canActivate: [AuthGuard] },
  { path: '**', redirectTo: '/login' }
];
