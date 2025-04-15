import {User} from './user.model';

export interface Project {
  id: string;
  name: string;
  containerStatus: string;
  owner: User;
  cpuUsage: number;
  memoryUsage: number;
  serverUrl: string;
  technology: string;
  logs: string;
}
