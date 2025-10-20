import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, delay } from 'rxjs';
import { Template } from '../models/template.model';
import { MOCK_TEMPLATES } from '../../../mocks/mock-data';

/**
 * Service for managing JSON Templates.
 */
@Injectable({
  providedIn: 'root'
})
export class TemplateService {
  // UI Testing method. To use the real backend, change this to false!
  private mockMode = false;

  private backendUrl = 'http://localhost:8090';
  private apiUrl = `${this.backendUrl}/api/config/templates`;

  constructor(private http: HttpClient) { }

  /**
   * Retrieves all templates from the backend
   * @returns An Observable of Template array
   */
  getTemplates(): Observable<Template[]> {
    if (this.mockMode) {
      console.warn('Mock Mode: Fetching templates.');
      return of(MOCK_TEMPLATES).pipe(delay(300));
    }
    return this.http.get<Template[]>(this.apiUrl);
  }

  /**
   * Retrieves a specific template by its ID
   * @param id The ID of the template to fetch
   * @returns An Observable of the requested Template
   */
  getTemplate(id: string): Observable<Template> {
    if (this.mockMode) {
      console.warn(`Mock Mode: Fetching template ${id}.`);
      const template = MOCK_TEMPLATES.find(t => t.id === id);
      return of(template!).pipe(delay(100));
    }
    return this.http.get<Template>(`${this.apiUrl}/${id}`);
  }

  /**
   * Creates a new template
   * @param template The template to create
   * @returns An Observable of the created Template
   */
  createTemplate(template: Template): Observable<Template> {
    if (this.mockMode) {
      console.warn('Mock Mode: Creating template.');
      const newTemplate = { ...template, id: `template-${Date.now()}` };
      MOCK_TEMPLATES.push(newTemplate);
      return of(newTemplate).pipe(delay(300));
    }
    return this.http.post<Template>(this.apiUrl, template);
  }

  /**
   * Updates an existing template
   * @param template The template to update
   * @returns An Observable of the updated Template
   */
  updateTemplate(template: Template): Observable<Template> {
    if (this.mockMode) {
      console.warn(`Mock Mode: Updating template ${template.id}.`);
      const index = MOCK_TEMPLATES.findIndex(t => t.id === template.id);
      if (index > -1) {
        MOCK_TEMPLATES[index] = { ...template };
        return of(MOCK_TEMPLATES[index]).pipe(delay(300));
      }
      return of(template);
    }
    return this.http.put<Template>(`${this.apiUrl}/${template.id}`, template);
  }

  /**
   * Deletes a template by its ID
   * @param id The ID of the template to delete
   * @returns An Observable of the HTTP response
   */
  deleteTemplate(id: string): Observable<void> {
    if (this.mockMode) {
      console.warn(`Mock Mode: Deleting template ${id}.`);
      const index = MOCK_TEMPLATES.findIndex(t => t.id === id);
      if (index > -1) {
        MOCK_TEMPLATES.splice(index, 1);
      }
      return of(undefined).pipe(delay(300));
    }
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
