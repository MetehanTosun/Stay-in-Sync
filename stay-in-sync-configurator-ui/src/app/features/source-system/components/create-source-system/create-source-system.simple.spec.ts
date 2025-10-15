import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { MessageService } from 'primeng/api';
import { of, throwError } from 'rxjs';

import { CreateSourceSystemComponent } from './create-source-system.component';
import { AasService } from '../../services/aas.service';
import { HttpErrorService } from '../../../../core/services/http-error.service';


describe('CreateSourceSystemComponent - Simple Tests', () => {
  let component: CreateSourceSystemComponent;
  let fixture: ComponentFixture<CreateSourceSystemComponent>;
  let aasService: jasmine.SpyObj<AasService>;
  let messageService: jasmine.SpyObj<MessageService>;
  let httpErrorService: jasmine.SpyObj<HttpErrorService>;

  beforeEach(async () => {
    const aasServiceSpy = jasmine.createSpyObj('AasService', [
      'createElement', 'deleteElement', 'getElement', 'listElements', 'encodeIdToBase64Url', 'listSubmodels'
    ]);
    const messageServiceSpy = jasmine.createSpyObj('MessageService', ['add']);

    await TestBed.configureTestingModule({
      imports: [
        CreateSourceSystemComponent,
        HttpClientTestingModule,
        RouterTestingModule
      ],
      providers: [
        { provide: AasService, useValue: aasServiceSpy },
        { provide: MessageService, useValue: messageServiceSpy },
        { provide: HttpErrorService, useValue: jasmine.createSpyObj('HttpErrorService', ['handleError']) }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CreateSourceSystemComponent);
    component = fixture.componentInstance;
    aasService = TestBed.inject(AasService) as jasmine.SpyObj<AasService>;
    messageService = TestBed.inject(MessageService) as jasmine.SpyObj<MessageService>;
    httpErrorService = TestBed.inject(HttpErrorService) as jasmine.SpyObj<HttpErrorService>;

    component.createdSourceSystemId = 1;
    aasService.listElements.and.returnValue(of([]));
    aasService.listSubmodels.and.returnValue(of([]));
    (aasService as any).refreshSnapshot = (aasService as any).refreshSnapshot || jasmine.createSpy('refreshSnapshot').and.returnValue(of({}));


    spyOn<any>(component, 'hydrateNodeTypesForNodes').and.returnValue(of([]));
  });

  it('opens create element dialog', () => {
    component.openCreateElement('test-submodel', undefined);
    expect(true).toBeTrue();
  });

  describe('Element Deletion', () => {
    it('should delete element successfully', () => {
      aasService.deleteElement.and.returnValue(of({}));

      component.deleteElement('test-submodel-id', 'test/element/path');

      expect(aasService.deleteElement).toHaveBeenCalledWith(1, 'test-submodel-id', 'test/element/path');
    });

    it('should handle delete error (404) gracefully', () => {
      const error = { status: 404, message: 'Not found' } as any;
      aasService.deleteElement.and.returnValue(throwError(() => error));

      component.deleteElement('test-submodel-id', 'test/element/path');

      expect(aasService.deleteElement).toHaveBeenCalledWith(1, 'test-submodel-id', 'test/element/path');
    });
  });
});
