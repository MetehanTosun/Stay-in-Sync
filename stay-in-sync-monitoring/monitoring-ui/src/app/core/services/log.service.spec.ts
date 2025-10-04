import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting} from '@angular/common/http/testing';
import { LogService } from './log.service';
import { LogEntry } from '../models/log.model';
import {ConfigService} from './config.service';
import {provideHttpClient} from '@angular/common/http';

describe('LogService', () => {
  let service: LogService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ConfigService, provideHttpClient(),
        provideHttpClientTesting() ],
    });

    service = TestBed.inject(LogService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('getLogsByTransformations', () => {
    it('should fetch logs for transformation IDs with correct params', () => {
      const mockLogs: LogEntry[] = [{ message: 'test', level: 'info', timestamp: '123456' }];
      const transformationIds = ['t1', 't2'];
      const startTime = 1000;
      const endTime = 2000;
      const level = 'info';

      service.getLogsByTransformations(transformationIds, startTime, endTime, level).subscribe(logs => {
        expect(logs).toEqual(mockLogs);
      });

      const req = httpMock.expectOne(r => r.url === '/api/logs/transformations');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(transformationIds);
      expect(req.request.params.get('startTime')).toBe(startTime.toString());
      expect(req.request.params.get('endTime')).toBe(endTime.toString());
      expect(req.request.params.get('level')).toBe(level);

      req.flush(mockLogs);
    });
  });

  describe('getLogs', () => {
    it('should fetch all logs with correct params', () => {
      const mockLogs: LogEntry[] = [{ message: 'general', level: 'warn', timestamp: '123456' }];
      const startTime = 1000;
      const endTime = 2000;
      const level = 'warn';

      service.getLogs(startTime, endTime, level).subscribe(logs => {
        expect(logs).toEqual(mockLogs);
      });

      const req = httpMock.expectOne(r => r.url === '/api/logs');
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('startTime')).toBe(startTime.toString());
      expect(req.request.params.get('endTime')).toBe(endTime.toString());
      expect(req.request.params.get('level')).toBe(level);

      req.flush(mockLogs);
    });
  });

  describe('getLogsByService', () => {
    it('should fetch logs for a specific service with correct params', () => {
      const mockLogs: LogEntry[] = [{message: 'service log', level: 'error', timestamp: '123456' }];
      const serviceName = 'core-polling-node';
      const startTime = 1000;
      const endTime = 2000;
      const level = 'error';

      service.getLogsByService(serviceName, startTime, endTime, level).subscribe(logs => {
        expect(logs).toEqual(mockLogs);
      });

      const req = httpMock.expectOne(r => r.url === '/api/logs/service');
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('service')).toBe(serviceName);
      expect(req.request.params.get('startTime')).toBe(startTime.toString());
      expect(req.request.params.get('endTime')).toBe(endTime.toString());
      expect(req.request.params.get('level')).toBe(level);

      req.flush(mockLogs);
    });
  });
});
