export interface EdcInstance {
  id: number; // Or string, if your IDs are not numbers
  name: string;
  url: string;
  status: 'Active' | 'Inactive' | 'Pending' | string; // Allow for more statuses if needed
  // Add any other relevant properties for your EDC instances
}
