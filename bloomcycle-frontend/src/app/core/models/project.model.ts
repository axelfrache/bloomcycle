import {User} from './user.model';

export interface Project {
  id: string;
  name: string;
  containerStatus: string;
  owner: User;
}
