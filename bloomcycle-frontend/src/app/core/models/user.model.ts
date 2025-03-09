import {Project} from './project.model';

export interface User {
  id: string;
  username: string;
  email: string;
  fullName: string;
  projects: Project[];
}
