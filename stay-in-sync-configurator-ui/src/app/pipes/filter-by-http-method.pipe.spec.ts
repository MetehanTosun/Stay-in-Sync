import { FilterByHttpMethodPipe } from './filter-by-http-method.pipe';

describe('FilterByHttpMethodPipe', () => {
  it('create an instance', () => {
    const pipe = new FilterByHttpMethodPipe();
    expect(pipe).toBeTruthy();
  });
});
