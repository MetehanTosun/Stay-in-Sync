/** Unit tests for `FilterBySubmodelPipe`. */
import { FilterBySubmodelPipe } from './filter-by-submodel.pipe';

/** Verifies filter behavior by submodel id for AAS arcs. */
describe('FilterBySubmodelPipe', () => {
  /** Should instantiate the pipe. */
  it('create an instance', () => {
    const pipe = new FilterBySubmodelPipe();
    expect(pipe).toBeTruthy();
  });

  /** Should return empty array when input list or submodelId is falsy. */
  it('returns [] when arcs or submodelId is falsy', () => {
    const pipe = new FilterBySubmodelPipe();
    expect(pipe.transform(null as any, 1)).toEqual([]);
    expect(pipe.transform(undefined as any, 1)).toEqual([]);
    expect(pipe.transform([], 0 as any)).toEqual([]);
  });

  /** Should filter only AAS arcs that match the given submodel id. */
  it('filters only AAS arcs matching submodelId', () => {
    const pipe = new FilterBySubmodelPipe();
    const arcs: any[] = [
      { arcType: 'AAS', submodelId: 7 },
      { arcType: 'REST', endpointId: 7 },
      { arcType: 'AAS', submodelId: 8 }
    ];
    const res = pipe.transform(arcs as any, 7);
    expect(res.length).toBe(1);
    expect(res[0].submodelId).toBe(7);
  });
});
