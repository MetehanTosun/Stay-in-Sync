/**
 * Interface representing a JSON Template in the frontend.
 */
export interface Template {
  id: string;
  name: string;
  description?: string;
  content: any;
}
