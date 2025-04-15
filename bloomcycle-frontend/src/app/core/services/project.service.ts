import { Injectable } from '@angular/core';
import { ApiService } from './api.service';
import { Observable } from 'rxjs';
import { Project } from '../models/project.model';

@Injectable({
  providedIn: 'root'
})
export class ProjectService {
  constructor(private apiService: ApiService) {}

  getProjects(): Observable<Project[]> {
    return this.apiService.get<Project[]>('projects');
  }

  getProjectById(id: string): Observable<Project> {
    return this.apiService.get<Project>(`projects/${id}`);
  }

  getProjectDetails(id: string): Observable<Project> {
    return this.apiService.get<Project>(`projects/${id}/details`);
  }

  createProject(project: FormData): Observable<Project> {
    return this.apiService.post<Project>(`projects`, project);
  }

  deleteProject(id: string): Observable<void> {
    return this.apiService.delete<void>(`projects/${id}`);
  }

  startProject(id: string): Observable<{serverUrl: string}> {
    return this.apiService.post<{serverUrl: string}>(`projects/${id}/start`, {});
  }

  restartProject(id: string): Observable<void> {
    return this.apiService.post<void>(`projects/${id}/restart`, {});
  }

  stopProject(id: string): Observable<void> {
    return this.apiService.post<void>(`projects/${id}/stop`, {});
  }

  autoRestartProject(id: string, enabled: boolean): Observable<any> {
    return this.apiService.post<any>(`projects/${id}/auto-restart`, {
      enabled: enabled
    });
    
  getProjectLogs(id: string): Observable<Project> {
    return this.apiService.get<Project>(`projects/${id}/logs`);
  }
}
