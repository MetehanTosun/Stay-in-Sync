import { TestBed } from '@angular/core/testing';
import { AasUtilityService } from './aas-utility.service';
import { TargetSystemDTO } from '../models/targetSystemDTO';

describe('AasUtilityService', () => {
  let service: AasUtilityService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(AasUtilityService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('isAasSystem should detect AAS apiType', () => {
    const sys = { apiType: 'AAS' } as TargetSystemDTO;
    expect(service.isAasSystem(sys)).toBeTrue();
    expect(service.isAasSystem({ apiType: 'REST' } as any)).toBeFalse();
    expect(service.isAasSystem(null)).toBeFalse();
  });

  it('getAasId should return aasId when present', () => {
    const sys = { apiType: 'AAS', aasId: 'my-id' } as any;
    expect(service.getAasId(sys)).toBe('my-id');
  });

  it('getAasId should parse localhost AAS ID from path or query', () => {
    const sys1 = { apiUrl: 'http://localhost:8080/aas/abc123' } as any;
    expect(service.getAasId(sys1)).toBe('abc123');

    const sys2 = { apiUrl: 'http://localhost:8080/some?aasId=xyz' } as any;
    expect(service.getAasId(sys2)).toBe('xyz');

    const sys3 = { apiUrl: 'http://localhost:8080/some?id=qwe' } as any;
    expect(service.getAasId(sys3)).toBe('qwe');
  });

  it('getAasId should return hostname for non-localhost', () => {
    const sys = { apiUrl: 'https://aas.example.com/api' } as any;
    expect(service.getAasId(sys)).toBe('aas.example.com');
  });

  it('getAasId should return fallback when invalid', () => {
    const sys = { apiUrl: '::::not-a-url' } as any;
    expect(service.getAasId(sys)).toBe('AAS ID not found');
    expect(service.getAasId(null)).toBe('-');
  });

  it('getParentPath should return parent before last slash', () => {
    expect(service.getParentPath('a/b/c')).toBe('a/b');
    expect(service.getParentPath('root')).toBe('');
  });

  it('encodeIdToBase64 should base64-encode an ID', () => {
    expect(service.encodeIdToBase64('abc')).toBe(btoa('abc'));
  });
});
