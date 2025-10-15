import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Template } from '../models/template.model';

/**
 * Service for managing JSON Templates.
 */
@Injectable({
  providedIn: 'root'
})
export class TemplateService {
  private apiUrl = 'http://localhost:8090/api/config/templates';

  constructor(private http: HttpClient) { }

  /**
   * Retrieves all templates from the backend
   * @returns An Observable of Template array
   */
  getTemplates(): Observable<Template[]> {
    return this.http.get<Template[]>(this.apiUrl);
  }

  /**
   * Retrieves a specific template by its ID
   * @param id The ID of the template to fetch
   * @returns An Observable of the requested Template
   */
  getTemplate(id: string): Observable<Template> {
    return this.http.get<Template>(`${this.apiUrl}/${id}`);
  }

  /**
   * Creates a new template
   * @param template The template to create
   * @returns An Observable of the created Template
   */
  createTemplate(template: Template): Observable<Template> {
    return this.http.post<Template>(this.apiUrl, template);
  }

  /**
   * Updates an existing template
   * @param template The template to update
   * @returns An Observable of the updated Template
   */
  updateTemplate(template: Template): Observable<Template> {
    return this.http.put<Template>(`${this.apiUrl}/${template.id}`, template);
  }

  /**
   * Deletes a template by its ID
   * @param id The ID of the template to delete
   * @returns An Observable of the HTTP response
   */
  deleteTemplate(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
