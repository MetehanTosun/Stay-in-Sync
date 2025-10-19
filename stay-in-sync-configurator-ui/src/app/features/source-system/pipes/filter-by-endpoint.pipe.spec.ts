/**
 * Test suite for FilterByEndpointPipe.
 * Verifies the pipe's behavior in filtering arcs by endpointId and arcType.
 */
import { FilterByEndpointPipe } from './filter-by-endpoint.pipe';

describe('FilterByEndpointPipe', () => {
  /**
   * Tests that an instance of the pipe can be created successfully.
   */
  it('create an instance', () => {
    const pipe = new FilterByEndpointPipe();
    expect(pipe).toBeTruthy();
  });

  /**
   * Tests that the pipe returns an empty array when the input arcs or endpointId are falsy values.
   */
  it('returns [] when arcs or endpointId is falsy', () => {
    const pipe = new FilterByEndpointPipe();
    expect(pipe.transform(null as any, 1)).toEqual([]);
    expect(pipe.transform(undefined as any, 1)).toEqual([]);
    expect(pipe.transform([], 0 as any)).toEqual([]);
  });

  /**
   * Tests that the pipe correctly filters arcs to include only those of type 'REST' 
   * that match the specified endpointId.
   */
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
