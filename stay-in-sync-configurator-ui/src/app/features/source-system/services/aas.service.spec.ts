import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AasService } from './aas.service';

describe('AasService', () => {
  let service: AasService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [AasService]
    });
    service = TestBed.inject(AasService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('createElement', () => {
    it('should create element with correct URL and parameters', () => {
      const sourceSystemId = 1;
      const submodelId = 'test-submodel-id';
      const element = { idShort: 'test-element', modelType: 'Property' };
      const parentPath = 'parent/path';

      service.createElement(sourceSystemId, submodelId, element, parentPath).subscribe();

      const req = httpMock.expectOne(`/api/config/source-system/${sourceSystemId}/aas/submodels/${service.encodeIdToBase64Url(submodelId)}/elements`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(element);
      expect(req.request.params.get('parentPath')).toBe('parent.path');
    });

    it('should create element without parentPath', () => {
      const sourceSystemId = 1;
      const submodelId = 'test-submodel-id';
      const element = { idShort: 'test-element', modelType: 'Property' };

      service.createElement(sourceSystemId, submodelId, element).subscribe();

      const req = httpMock.expectOne(`/api/config/source-system/${sourceSystemId}/aas/submodels/${service.encodeIdToBase64Url(submodelId)}/elements`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(element);
      expect(req.request.params.get('parentPath')).toBeNull();
    });
  });

  describe('deleteElement', () => {
    it('should delete element with correct URL', () => {
      const sourceSystemId = 1;
      const submodelId = 'test-submodel-id';
      const elementPath = 'test/element/path';

      service.deleteElement(sourceSystemId, submodelId, elementPath).subscribe();

      const req = httpMock.expectOne(`/api/config/source-system/${sourceSystemId}/aas/submodels/${service.encodeIdToBase64Url(submodelId)}/elements/test.element.path`);
      expect(req.request.method).toBe('DELETE');
    });

    it('should handle long paths correctly', () => {
      const sourceSystemId = 1;
      const submodelId = 'test-submodel-id';
      const elementPath = 'very/long/path/with/many/segments/that/might/cause/issues';

      service.deleteElement(sourceSystemId, submodelId, elementPath).subscribe();

      const req = httpMock.expectOne(`/api/config/source-system/${sourceSystemId}/aas/submodels/${service.encodeIdToBase64Url(submodelId)}/elements/very.long.path.with.many.segments.that.might.cause.issues`);
      expect(req.request.method).toBe('DELETE');
    });
  });

  describe('getElement', () => {
    it('should get element with correct URL and parameters', () => {
      const sourceSystemId = 1;
      const submodelId = 'test-submodel-id';
      const idShortPath = 'test/element/path';
      const source = 'LIVE';

      service.getElement(sourceSystemId, submodelId, idShortPath, source).subscribe();

      const req = httpMock.expectOne(`/api/config/source-system/${sourceSystemId}/aas/submodels/${service.encodeIdToBase64Url(submodelId)}/elements/test.element.path`);
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('source')).toBe(source);
    });
  });

  describe('listElements', () => {
    it('should list elements with correct parameters', () => {
      const sourceSystemId = 1;
      const submodelId = 'test-submodel-id';
      const options = { depth: 'shallow' as const, parentPath: 'parent', source: 'LIVE' as const };

      service.listElements(sourceSystemId, submodelId, options).subscribe();

      const req = httpMock.expectOne(`/api/config/source-system/${sourceSystemId}/aas/submodels/${service.encodeIdToBase64Url(submodelId)}/elements`);
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('depth')).toBe('shallow');
      expect(req.request.params.get('parentPath')).toBe('parent');
      expect(req.request.params.get('source')).toBe('LIVE');
    });
  });

  describe('encodeIdToBase64Url', () => {
    it('should encode ID correctly (padding allowed)', () => {
      const id = 'https://admin-shell.io/idta/SubmodelTemplate/CarbonFootprint/0/9';
      const encoded = service.encodeIdToBase64Url(id);
      expect(encoded).toBeDefined();
      expect(encoded).not.toBe(id);
      expect(encoded).not.toContain('+');
      expect(encoded).not.toContain('/');
      // Padding '=' may be present; do not assert its absence
    });
  });
});
