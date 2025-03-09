import {File} from './file.model';
import {User} from './user.model';

export interface Project {
  id: string;
  name: string;
  status: string;
  owner: User;
  files: File[];
}
