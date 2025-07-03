export interface EdcInstance {
  id: string;
  name: string;
  url: string;
  status: 'Active' | 'Inactive';
  apiKey?: string;
}
