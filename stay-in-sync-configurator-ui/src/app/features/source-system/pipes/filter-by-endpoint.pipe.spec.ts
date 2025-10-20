import { FilterByEndpointPipe } from './filter-by-endpoint.pipe';

describe('FilterByEndpointPipe', () => {
  it('create an instance', () => {
    const pipe = new FilterByEndpointPipe();
    expect(pipe).toBeTruthy();
  });

  it('returns [] when arcs or endpointId is falsy', () => {
    const pipe = new FilterByEndpointPipe();
    expect(pipe.transform(null as any, 1)).toEqual([]);
    expect(pipe.transform(undefined as any, 1)).toEqual([]);
    expect(pipe.transform([], 0 as any)).toEqual([]);
  });

  it('filters only REST arcs matching endpointId', () => {
    const pipe = new FilterByEndpointPipe();
    const arcs: any[] = [
      { arcType: 'REST', endpointId: 10, name: 'ok' },
      { arcType: 'REST', endpointId: 11, name: 'no' },
      { arcType: 'AAS', submodelId: 10 },
      { arcType: 'REST', endpointId: 10, name: 'ok2' }
    ];
    const res = pipe.transform(arcs as any, 10);
    expect(res.length).toBe(2);
    expect(res.every(a => a.arcType === 'REST' && a.endpointId === 10)).toBeTrue();
  });
});
