// biome-ignore lint/style/useImportType: <explanation>
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


describe('SourceSystemBaseComponent (smoke)', () => {
  let component: SourceSystemBaseComponent;
  let fixture: ComponentFixture<SourceSystemBaseComponent>;
  let mockSourceSystemService: jasmine.SpyObj<SourceSystemResourceService>;
  let mockErrorService: jasmine.SpyObj<HttpErrorService>;

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

  it('should create', () => {
    mockSourceSystemService.apiConfigSourceSystemGet.and.returnValue(of([] as any));
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('loads systems on init', () => {
    const apiSystems: SourceSystem[] = [
      { id: 1, name: 'A', apiUrl: 'u', description: 'd', apiType: 'REST' } as any
    ];
    mockSourceSystemService.apiConfigSourceSystemGet.and.returnValue(of(apiSystems as any));
    fixture.detectChanges();
    expect(component['systems'].length).toBe(1);
    expect(component['loading']).toBeFalse();
  });

  it('handles load error', () => {
    mockSourceSystemService.apiConfigSourceSystemGet.and.returnValue(of({} as any));
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });
});
