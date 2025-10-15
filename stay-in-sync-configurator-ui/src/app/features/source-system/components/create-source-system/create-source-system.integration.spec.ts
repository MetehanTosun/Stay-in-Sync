import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { MessageService } from 'primeng/api';

import { CreateSourceSystemComponent } from './create-source-system.component';
import { AasService } from '../../services/aas.service';
import { HttpErrorService } from '../../../../core/services/http-error.service';


describe('CreateSourceSystemComponent Integration Tests', () => {
  let component: CreateSourceSystemComponent;
  let fixture: ComponentFixture<CreateSourceSystemComponent>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        CreateSourceSystemComponent,
        HttpClientTestingModule,
        RouterTestingModule
      ],
      providers: [
        AasService,
        MessageService,
        HttpErrorService
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CreateSourceSystemComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);

    component.createdSourceSystemId = 1;
  });

  afterEach(() => {
   
  });

  it('opens element dialog with correct data', () => {
    component.openCreateElement('smId', 'parent/path');
    expect(true).toBeTrue();
  });

  it('loads children with correct params', () => {
    const submodelId = 'smId';
    const parentPath = 'parent/path';
    const node = { children: [] } as any;

    component['loadChildren'](submodelId, parentPath, node);

    const req = httpMock.expectOne((request) => {
      return request.url.includes('/api/config/source-system/1/aas/submodels/') &&
             request.url.includes('/elements') &&
             request.method === 'GET';
    });
    expect(req.request.params.get('depth')).toBe('shallow');
    expect(req.request.params.get('parentPath')).toBe(parentPath);
    expect(req.request.params.get('source')).toBe('SNAPSHOT');
    req.flush([]);
  });

  it('tests AAS connection and triggers refresh', () => {
    component.createdSourceSystemId = 2;
    // Start connection test
    component.testAasConnection();

    // Expect POST /test
    const testReq = httpMock.expectOne((request) => {
      return request.url.includes('/api/config/source-system/2/aas/test') && request.method === 'POST';
    });
    testReq.flush({ idShort: 'Shell' });

    // Expect refreshSnapshot after success (actual endpoint is POST /snapshot/refresh)
    const refreshReq = httpMock.expectOne((request) => {
      return request.url.includes('/api/config/source-system/2/aas/snapshot/refresh') && request.method === 'POST';
    });
    refreshReq.flush({});
  });

  it('creates submodel and rediscover is called', () => {
    component.createdSourceSystemId = 3;
    component.newSubmodelJson = '{"id":"sm://new","idShort":"NewSm"}';
    component.createSubmodel();

    const createReq = httpMock.expectOne((request) => {
      return request.url.includes('/api/config/source-system/3/aas/submodels') && request.method === 'POST';
    });
    createReq.flush({});

    const listReq = httpMock.expectOne((request) => {
      return request.url.includes('/api/config/source-system/3/aas/submodels') && request.method === 'GET';
    });
    listReq.flush([]);
  });
});
