/** Unit tests for CreateTargetSystemDialogService ensuring correct handling of AASX selection, templates, and submodel management. */
import { TestBed } from '@angular/core/testing';
import { CreateTargetSystemDialogService } from './create-target-system-dialog.service';

describe('CreateTargetSystemDialogService', () => {
  let service: CreateTargetSystemDialogService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(CreateTargetSystemDialogService);
  });

  /**
   * Ensures the service is instantiated successfully.
   */
  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  /**
   * Verifies that getSubmodelTemplates returns templates including minimal, property, and collection types.
   */
  it('getSubmodelTemplates should include minimal/property/collection', () => {
    const t = service.getSubmodelTemplates();
    expect(t['minimal']).toContain('NewSubmodel');
    expect(t['property']).toContain('Property');
    expect(t['collection']).toContain('SubmodelElementCollection');
  });

  /**
   * Checks that getElementTemplates includes common element types such as property, range, mlp, and operation.
   */
  it('getElementTemplates should include common element types', () => {
    const t = service.getElementTemplates();
    expect(t['property']).toContain('Property');
    expect(t['range']).toContain('Range');
    expect(t['mlp']).toContain('MultiLanguageProperty');
    expect(t['operation']).toContain('Operation');
  });

  /**
   * Tests that setTemplate returns the value for a given key or falls back to the default if key is missing.
   */
  it('setTemplate should pick by key and fallback', () => {
    const map = { a: 'A', b: 'B' } as any;
    expect(service.setTemplate(map as any, 'a', 'X')).toBe('A');
    expect(service.setTemplate(map as any, 'z', 'X')).toBe('X');
  });

  /**
   * Validates that getOrInitAasxSelection initializes a new selection or returns an existing one for a submodel.
   */
  it('getOrInitAasxSelection should initialize or reuse', () => {
    const sel = { submodels: [] as Array<{ id: string; full: boolean; elements: string[] }> };
    const sm = { id: 'sm1' } as any;
    const s1 = service.getOrInitAasxSelection(sel, sm);
    expect(s1.id).toBe('sm1');
    const s2 = service.getOrInitAasxSelection(sel, sm);
    expect(s1).toBe(s2);
  });

  /**
   * Confirms that toggleAasxSubmodelFull clears selected elements when the submodel is marked as fully selected.
   */
  it('toggleAasxSubmodelFull should clear elements when checked', () => {
    const sel = { submodels: [] as Array<{ id: string; full: boolean; elements: string[] }> };
    const sm = { id: 'sm1' } as any;
    service.toggleAasxSubmodelFull(sel, sm, true);
    expect(service.getOrInitAasxSelection(sel, sm).elements.length).toBe(0);
  });

  /**
   * Tests that isAasxElementSelected and toggleAasxElement correctly reflect the selected state of elements.
   */
  it('isAasxElementSelected and toggleAasxElement should reflect state', () => {
    const sel = { submodels: [] as Array<{ id: string; full: boolean; elements: string[] }> };
    const sm = { id: 'sm1' } as any;
    expect(service.isAasxElementSelected(sel, sm, 'e1')).toBeFalse();
    service.toggleAasxElement(sel, sm, 'e1', true);
    expect(service.isAasxElementSelected(sel, sm, 'e1')).toBeTrue();
    service.toggleAasxElement(sel, sm, 'e1', false);
    expect(service.isAasxElementSelected(sel, sm, 'e1')).toBeFalse();
  });

  /**
   * Checks that getSelectedSubmodelIds returns the IDs of fully selected submodels and excludes partially selected ones.
   */
  it('getSelectedSubmodelIds should return ids when selection exists', () => {
    const sel = { submodels: [{ id: 'a', full: true, elements: [] }] } as any;
    expect(service.getSelectedSubmodelIds(sel)).toEqual(['a']);
    const sel2 = { submodels: [{ id: 'a', full: false, elements: [] }] } as any;
    expect(service.getSelectedSubmodelIds(sel2)).toEqual([]);
  });
});
