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

  /**
   * Saves a collection of templates
   * This method handles create/update operations for each template
   * @param templates Array of templates to save
   * @returns An Observable that completes when the operation is done
   */
  saveTemplates(templates: Template[]): Observable<Template[]> {
    // Process each template individually with the appropriate create/update operation
    return new Observable<Template[]>(observer => {
      const savedTemplates: Template[] = [];
      let completedCount = 0;
      
      templates.forEach(template => {
        // Make sure the template has a valid UUID if we're creating a new one
        if (!template.id || template.id.trim() === '') {
          // For new templates, use the backend to generate the UUID
          this.createTemplate(template).subscribe({
            next: savedTemplate => {
              savedTemplates.push(savedTemplate);
              completedCount++;
              if (completedCount === templates.length) {
                observer.next(savedTemplates);
                observer.complete();
              }
            },
            error: error => observer.error(error)
          });
        } else {
          // Update existing template - ensure the ID is a valid UUID
          try {
            // Validate UUID format
            if (!this.isValidUuid(template.id)) {
              throw new Error(`Invalid UUID format: ${template.id}`);
            }
            
            this.updateTemplate(template).subscribe({
              next: savedTemplate => {
                savedTemplates.push(savedTemplate);
                completedCount++;
                if (completedCount === templates.length) {
                  observer.next(savedTemplates);
                  observer.complete();
                }
              },
              error: error => observer.error(error)
            });
          } catch (error) {
            observer.error(error);
          }
        }
      });
      
      // Handle empty array case
      if (templates.length === 0) {
        observer.next([]);
        observer.complete();
      }
    });
  }
  
  /**
   * Validates if a string is a valid UUID v4 format
   * @param id The string to validate as UUID
   * @returns True if the string is a valid UUID, false otherwise
   */
  private isValidUuid(id: string): boolean {
    const uuidRegex = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
    return uuidRegex.test(id);
  }
}
