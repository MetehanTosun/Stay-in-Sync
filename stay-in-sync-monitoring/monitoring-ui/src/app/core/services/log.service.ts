import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { HttpClient, HttpParams } from '@angular/common/http';
import { LogEntry } from '../models/log.model';

/**
 * LogService
 *
 * Provides methods to fetch log entries from the backend API.
 * Supports fetching logs by:
 * - Transformation IDs
 * - Service name
 * - General logs (all entries within a time range)
 *
 * Each method allows optional filtering by log level.
 */
@Injectable({ providedIn: 'root' })
export class LogService {
  /**
   * Base URL for the log API endpoints.
   */
  private readonly baseUrl = '/api/logs';

  constructor(private readonly http: HttpClient) {}

  /**
   * Fetch logs for a list of transformation IDs within a given time range.
   *
   * @param transformationIds Array of transformation IDs.
   * @param startTime Start timestamp in nanoseconds.
   * @param endTime End timestamp in nanoseconds.
   * @param level (Optional) Log level filter (e.g., "info", "error").
   * @returns Observable emitting an array of log entries.
   */
  getLogsByTransformations(
    transformationIds: string[],
    startTime: number,
    endTime: number,
    level?: string
  ): Observable<LogEntry[]> {
    let params = new HttpParams()
      .set('startTime', startTime)
      .set('endTime', endTime);

    if (level) {
      params = params.set('level', level);
    }

    return this.http.post<LogEntry[]>(
      `${this.baseUrl}/transformations`,
      transformationIds,
      { params }
    );
  }

  /**
   * Fetch all logs within a given time range (optionally filtered by level).
   *
   * @param startTime Start timestamp in nanoseconds.
   * @param endTime End timestamp in nanoseconds.
   * @param level (Optional) Log level filter (e.g., "debug", "warn").
   * @returns Observable emitting an array of log entries.
   */
  getLogs(
    startTime: number,
    endTime: number,
    level?: string
  ): Observable<LogEntry[]> {
    let params = new HttpParams()
      .set('startTime', startTime)
      .set('endTime', endTime);

    if (level) {
      params = params.set('level', level);
    }

    return this.http.get<LogEntry[]>(this.baseUrl, { params });
  }

  /**
   * Fetch logs for a specific service within a given time range.
   *
   * @param service Service name (e.g., "core-polling-node").
   * @param startTime Start timestamp in nanoseconds.
   * @param endTime End timestamp in nanoseconds.
   * @param level (Optional) Log level filter.
   * @returns Observable emitting an array of log entries.
   */
  getLogsByService(
    service: string,
    startTime: number,
    endTime: number,
    level?: string
  ): Observable<LogEntry[]> {
    let params = new HttpParams()
      .set('service', service)
      .set('startTime', startTime)
      .set('endTime', endTime);

    if (level) {
      params = params.set('level', level);
    }

    return this.http.get<LogEntry[]>(`${this.baseUrl}/service`, { params });
  }
}
