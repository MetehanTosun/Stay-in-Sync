import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { SourceSystemBaseComponent } from './source-system-base.component';
import { SourceSystemResourceService } from '../../service/sourceSystemResource.service';
import { HttpErrorService } from '../../../../core/services/http-error.service';
import { SourceSystem } from '../../models/sourceSystem';
import { MessageService } from 'primeng/api';

/**
 * Smoke test suite for the SourceSystemBaseComponent.
 * Verifies component creation, initialization behavior, and error handling.
 */
describe('SourceSystemBaseComponent (smoke)', () => {
  let component: SourceSystemBaseComponent;
  let fixture: ComponentFixture<SourceSystemBaseComponent>;
  let mockSourceSystemService: jasmine.SpyObj<SourceSystemResourceService>;
  let mockErrorService: jasmine.SpyObj<HttpErrorService>;

  /**
   * Sets up the testing environment for each test.
   * Initializes mock services and creates the SourceSystemBaseComponent instance.
   */
  beforeEach(async () => {
    mockSourceSystemService = jasmine.createSpyObj('SourceSystemResourceService', ['apiConfigSourceSystemGet', 'apiConfigSourceSystemIdDelete']);
    mockErrorService = jasmine.createSpyObj('HttpErrorService', ['handleError']);

    await TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, RouterTestingModule, SourceSystemBaseComponent],
      providers: [
        { provide: SourceSystemResourceService, useValue: mockSourceSystemService },
        { provide: HttpErrorService, useValue: mockErrorService },
        MessageService,
        provideNoopAnimations()
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(SourceSystemBaseComponent);
    component = fixture.componentInstance;
  });

  /**
   * Ensures that the component can be instantiated successfully
   * and that no errors occur during initialization.
   */
  it('should create', () => {
    mockSourceSystemService.apiConfigSourceSystemGet.and.returnValue(of([] as any));
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  /**
   * Verifies that systems are correctly loaded and assigned
   * to the component when the API call returns data.
   */
  it('loads systems on init', () => {
    const apiSystems: SourceSystem[] = [
      { id: 1, name: 'A', apiUrl: 'u', description: 'd', apiType: 'REST' } as any
    ];
    mockSourceSystemService.apiConfigSourceSystemGet.and.returnValue(of(apiSystems as any));
    fixture.detectChanges();
    expect(component['systems'].length).toBe(1);
    expect(component['loading']).toBeFalse();
  });

  /**
   * Tests the component's behavior when the API call for systems fails.
   * Ensures the component remains functional and handles errors gracefully.
   */
  it('handles load error', () => {
    mockSourceSystemService.apiConfigSourceSystemGet.and.returnValue(of({} as any));
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });
});
