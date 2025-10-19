/** Unit tests for `FilterByTypePipe`. */
import { FilterByTypePipe } from './filter-by-type.pipe';

/** Verifies filtering behavior by arc type. */
describe('FilterByTypePipe', () => {
  /** Should instantiate the pipe. */
  it('create an instance', () => {
    const pipe = new FilterByTypePipe();
    expect(pipe).toBeTruthy();
  });

  /** Should return empty array when input list is falsy. */
  it('returns [] when arcs is falsy', () => {
    const pipe = new FilterByTypePipe();
    expect(pipe.transform(null as any, 'REST')).toEqual([]);
    expect(pipe.transform(undefined as any, 'AAS')).toEqual([]);
  });

  /** Should filter items by `arcType`. */
  it('filters by arcType', () => {
    const pipe = new FilterByTypePipe();
    const arcs: any[] = [
      { arcType: 'REST', endpointId: 1 },
      { arcType: 'AAS', submodelId: 1 },
      { arcType: 'REST', endpointId: 2 }
    ];
    expect(pipe.transform(arcs as any, 'REST').length).toBe(2);
    expect(pipe.transform(arcs as any, 'AAS').length).toBe(1);
  });
});
