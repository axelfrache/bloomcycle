import { Injectable } from '@angular/core';
import { ApiService } from './api.service';
import { Observable } from 'rxjs';
import { Project } from '../models/project.model';

@Injectable({
  providedIn: 'root'
})
export class ProjectService {
  constructor(private apiService: ApiService) {}

  getAllProjects(): Observable<Project[]> {
    return this.apiService.get<Project[]>('projects');
  }

  getProjectById(id: string): Observable<Project> {
    return this.apiService.get<Project>(`projects/${id}`);
  }

  createProject(project: Partial<Project>): Observable<Project> {
    return this.apiService.post<Project>('projects', project);
  }

  deleteProject(id: string): Observable<void> {
    return this.apiService.delete<void>(`projects/${id}`);
  }
}
