import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { TargetSystemDTO } from '../models/targetSystemDTO';

/**
 * Service responsible for performing CRUD operations on Target Systems.
 * Interacts with the backend REST API to manage Target System configurations.
 */
@Injectable({ providedIn: 'root' })
export class TargetSystemResourceService {
  /**
   * Creates a new instance of TargetSystemResourceService.
   * @param http Angular HttpClient instance used for sending HTTP requests.
   */
  constructor(private http: HttpClient) {}

  /**
   * Retrieves all Target Systems from the backend.
   * @returns Observable emitting an array of TargetSystemDTO objects.
   */
  getAll(): Observable<TargetSystemDTO[]> {
    return this.http.get<TargetSystemDTO[]>(`/api/config/target-systems`);
  }

  /**
   * Retrieves details for a specific Target System by its ID.
   * @param id ID of the Target System to retrieve.
   * @returns Observable emitting the TargetSystemDTO of the requested system.
   */
  getById(id: number): Observable<TargetSystemDTO> {
    return this.http.get<TargetSystemDTO>(`/api/config/target-systems/${id}`);
  }

  /**
   * Creates a new Target System in the backend.
   * @param dto The TargetSystemDTO containing system data to create.
   * @returns Observable emitting the created TargetSystemDTO.
   */
  create(dto: TargetSystemDTO): Observable<TargetSystemDTO> {
    return this.http.post<TargetSystemDTO>(`/api/config/target-systems`, dto);
  }

  /**
   * Updates an existing Target System with new data.
   * @param id ID of the Target System to update.
   * @param dto The updated TargetSystemDTO object.
   * @returns Observable emitting the updated TargetSystemDTO.
   */
  update(id: number, dto: TargetSystemDTO): Observable<TargetSystemDTO> {
    return this.http.put<TargetSystemDTO>(`/api/config/target-systems/${id}`, dto);
  }

  /**
   * Deletes a Target System from the backend.
   * @param id ID of the Target System to delete.
   * @returns Observable that completes once deletion is successful.
   */
  delete(id: number): Observable<void> {
    return this.http.delete<void>(`/api/config/target-systems/${id}`);
  }
}


