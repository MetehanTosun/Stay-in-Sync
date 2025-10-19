/**
 * Unit tests for AasUtilityService verifying AAS-related utilities like ID extraction, encoding, and path handling.
 */
import { TestBed } from '@angular/core/testing';
import { AasUtilityService } from './aas-utility.service';
import { TargetSystemDTO } from '../models/targetSystemDTO';

describe('AasUtilityService', () => {
  let service: AasUtilityService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(AasUtilityService);
  });

  /**
   * Ensures that the service is created successfully.
   */
  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  /**
   * Ensures that isAasSystem correctly identifies AAS and non-AAS systems.
   */
  it('isAasSystem should detect AAS apiType', () => {
    const sys = { apiType: 'AAS' } as TargetSystemDTO;
    expect(service.isAasSystem(sys)).toBeTrue();
    expect(service.isAasSystem({ apiType: 'REST' } as any)).toBeFalse();
    expect(service.isAasSystem(null)).toBeFalse();
  });

  /**
   * Ensures that getAasId returns the aasId property when present.
   */
  it('getAasId should return aasId when present', () => {
    const sys = { apiType: 'AAS', aasId: 'my-id' } as any;
    expect(service.getAasId(sys)).toBe('my-id');
  });

  /**
   * Ensures that getAasId extracts the AAS ID from localhost URLs using path or query parameters.
   */
  it('getAasId should parse localhost AAS ID from path or query', () => {
    const sys1 = { apiUrl: 'http://localhost:8080/aas/abc123' } as any;
    expect(service.getAasId(sys1)).toBe('abc123');

    const sys2 = { apiUrl: 'http://localhost:8080/some?aasId=xyz' } as any;
    expect(service.getAasId(sys2)).toBe('xyz');

    const sys3 = { apiUrl: 'http://localhost:8080/some?id=qwe' } as any;
    expect(service.getAasId(sys3)).toBe('qwe');
  });

  /**
   * Ensures that getAasId returns the hostname for non-localhost URLs.
   */
  it('getAasId should return hostname for non-localhost', () => {
    const sys = { apiUrl: 'https://aas.example.com/api' } as any;
    expect(service.getAasId(sys)).toBe('aas.example.com');
  });

  /**
   * Ensures that getAasId returns fallback values when an invalid URL or null is provided.
   */
  it('getAasId should return fallback when invalid', () => {
    const sys = { apiUrl: '::::not-a-url' } as any;
    expect(service.getAasId(sys)).toBe('AAS ID not found');
    expect(service.getAasId(null)).toBe('-');
  });

  /**
   * Ensures that getParentPath returns the parent path before the last slash, or empty string for root.
   */
  it('getParentPath should return parent before last slash', () => {
    expect(service.getParentPath('a/b/c')).toBe('a/b');
    expect(service.getParentPath('root')).toBe('');
  });

  /**
   * Ensures that encodeIdToBase64 returns the correct base64-encoded string for a given ID.
   */
  it('encodeIdToBase64 should base64-encode an ID', () => {
    expect(service.encodeIdToBase64('abc')).toBe(btoa('abc'));
  });
});
