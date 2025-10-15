import { TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';

import { MessageService, ConfirmationService } from 'primeng/api';

import { SourceSystemBaseComponent } from './source-system-base/source-system-base.component';
import { SourceSystemPageComponent } from './source-system-page/source-system-page.component';
import { SourceSystemAasManagementComponent } from './source-system-aas-management/source-system-aas-management.component';
import { ManageApiHeadersComponent } from './manage-api-headers/manage-api-headers.component';
import { ManageEndpointsComponent } from './manage-endpoints/manage-endpoints.component';
import { ManageEndpointParamsComponent } from './manage-endpoint-params/manage-endpoint-params.component';
import { CreateSourceSystemComponent } from './create-source-system/create-source-system.component';

import { SourceSystemResourceService } from '../../service/sourceSystemResource.service';
import { SourceSystemEndpointResourceService } from '../../service/sourceSystemEndpointResource.service';
import { ApiHeaderResourceService } from '../../service/apiHeaderResource.service';
import { ApiHeaderValueResourceService } from '../../service/apiHeaderValueResource.service';
import { RequestConfigurationResourceService } from '../../service/requestConfigurationResource.service';
import { AasService } from '../../services/aas.service';
import { SourceSystemAasManagementService } from '../../services/source-system-aas-management.service';
import { CreateSourceSystemDialogService } from '../../services/create-source-system-dialog.service';
import { CreateSourceSystemFormService } from '../../services/create-source-system-form.service';

const mockSourceSystem = {
  id: 1,
  name: 'Demo Source',
  apiUrl: 'http://localhost:8081',
  apiType: 'AAS',
  description: ''
} as any;

const mockProviders = [
  { provide: MessageService, useValue: { add: jasmine.createSpy('add') } },
  { provide: ConfirmationService, useValue: { confirm: () => {} } },
  {
    provide: SourceSystemResourceService,
    useValue: {
      apiConfigSourceSystemGet: () => of([mockSourceSystem]),
      apiConfigSourceSystemIdGet: (_id: number) => of(mockSourceSystem),
      apiConfigSourceSystemIdPut: () => of(mockSourceSystem),
      apiConfigSourceSystemPost: () => of(mockSourceSystem),
      apiConfigSourceSystemIdDelete: () => of({})
    }
  },
  {
    provide: SourceSystemEndpointResourceService,
    useValue: {
      apiConfigSourceSystemEndpointGet: () => of([]),
      apiConfigSourceSystemEndpointPost: () => of({}),
      apiConfigSourceSystemEndpointIdPut: () => of({}),
      apiConfigSourceSystemEndpointIdDelete: () => of({})
    }
  },
  { provide: ApiHeaderResourceService, useValue: { apiConfigApiHeaderGet: () => of([]), apiConfigApiHeaderPost: () => of({}), apiConfigApiHeaderIdDelete: () => of({}) } },
  { provide: ApiHeaderValueResourceService, useValue: { apiConfigApiHeaderValueGet: () => of([]), apiConfigApiHeaderValuePost: () => of({}), apiConfigApiHeaderValueIdDelete: () => of({}) } },
  { provide: RequestConfigurationResourceService, useValue: { apiConfigRequestConfigurationGet: () => of([]) } },
  {
    provide: AasService,
    useValue: {
      listSubmodels: () => of([]),
      listElements: () => of([]),
      getElement: () => of({}),
      previewAasx: () => of({ submodels: [] }),
      uploadAasx: () => of({}),
      attachSelectedAasx: () => of({}),
      createSubmodel: () => of({}),
      createElement: () => of({}),
      deleteElement: () => of({}),
      deleteSubmodel: () => of({}),
      encodeIdToBase64Url: (s: string) => btoa(s)
    }
  },
  {
    provide: SourceSystemAasManagementService,
    useValue: {
      createSubmodel: () => of({}),
      createElement: () => of({}),
      deleteSubmodel: () => of({}),
      deleteElement: () => of({}),
      setPropertyValue: () => of({}),
      encodeIdToBase64Url: (s: string) => btoa(s),
      listElements: () => of({ result: [] }),
      parseValueForType: (_v: string) => _v,
      findNodeByKey: () => null
    }
  },
  { provide: CreateSourceSystemDialogService, useValue: { uploadAasx: () => Promise.resolve({}) } },
  { provide: CreateSourceSystemFormService, useValue: { createForm: () => ({ get: () => ({ value: null }), valueChanges: of(null) }) } }
];

describe('Source System components smoke test', () => {
  const components = [
    SourceSystemBaseComponent,
    SourceSystemPageComponent,
    SourceSystemAasManagementComponent,
    ManageApiHeadersComponent,
    ManageEndpointsComponent,
    ManageEndpointParamsComponent,
    CreateSourceSystemComponent
  ];

  for (const Cmp of components) {
    it(`should create ${Cmp.name}`, async () => {
      await TestBed.configureTestingModule({
        imports: [
          HttpClientTestingModule,
          RouterTestingModule,
          Cmp
        ],
        providers: [
          ...mockProviders,
          { provide: (window as any).ActivatedRoute || 'ActivatedRoute', useValue: { snapshot: { paramMap: { get: () => '1' } } } }
        ],
        schemas: [NO_ERRORS_SCHEMA]
      }).compileComponents();

      const fixture = TestBed.createComponent(Cmp as any);
      const instance = fixture.componentInstance as any;
      if ('system' in instance && !instance.system) {
        instance.system = mockSourceSystem;
      }
      if ('sourceSystem' in instance && !instance.sourceSystem) {
        instance.sourceSystem = mockSourceSystem;
      }
      if ('selectedSystem' in instance && !instance.selectedSystem) {
        instance.selectedSystem = mockSourceSystem;
      }
      fixture.detectChanges();
      expect(instance).toBeTruthy();
    });
  }
});



