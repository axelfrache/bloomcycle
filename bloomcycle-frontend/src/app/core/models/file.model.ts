export interface File {
  id: string;
  name: string;
  type: string;
  content: Uint8Array;
  project_id: string;
}
