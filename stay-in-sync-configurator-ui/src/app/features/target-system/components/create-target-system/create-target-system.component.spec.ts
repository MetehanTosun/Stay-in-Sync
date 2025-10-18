import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of } from 'rxjs';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { CreateTargetSystemComponent } from './create-target-system.component';
import { TargetSystemResourceService } from '../../service/targetSystemResource.service';
import { ApiHeaderResourceService } from '../../../source-system/service/apiHeaderResource.service';
import { HttpErrorService } from '../../../../core/services/http-error.service';
import { AasClientService } from '../../../source-system/services/aas-client.service';
import { CreateTargetSystemFormService } from '../../services/create-target-system-form.service';
import { CreateTargetSystemAasService } from '../../services/create-target-system-aas.service';
import { CreateTargetSystemDialogService } from '../../services/create-target-system-dialog.service';
import { MessageService } from 'primeng/api';

/**
 * Unit tests for {@link CreateTargetSystemComponent}.
 * Verifies functionality for creating, saving, and managing Target System configurations.
 * Includes tests for form handling, AAS type detection, modal visibility, and step navigation.
 */
describe('CreateTargetSystemComponent', () => {
  let component: CreateTargetSystemComponent;
  let fixture: ComponentFixture<CreateTargetSystemComponent>;
  let apiSpy: jasmine.SpyObj<TargetSystemResourceService>;
  let headersSpy: jasmine.SpyObj<ApiHeaderResourceService>;
  let aasClientSpy: jasmine.SpyObj<AasClientService>;
  let aasServiceSpy: jasmine.SpyObj<CreateTargetSystemAasService>;

  /**
   * Sets up the testing environment before each test case.
   * Initializes spies, dependencies, and the testing module configuration.
   */
  beforeEach(async () => {
    apiSpy = jasmine.createSpyObj('TargetSystemResourceService', ['create', 'getById']);
    headersSpy = jasmine.createSpyObj('ApiHeaderResourceService', ['']);
    aasClientSpy = jasmine.createSpyObj('AasClientService', ['previewAas']);
    aasServiceSpy = jasmine.createSpyObj('CreateTargetSystemAasService', ['testConnection']);

    await TestBed.configureTestingModule({
      imports: [CreateTargetSystemComponent, HttpClientTestingModule, RouterTestingModule],
      providers: [
        provideNoopAnimations(),
        MessageService,
        CreateTargetSystemFormService,
        CreateTargetSystemDialogService,
        HttpErrorService,
        { provide: TargetSystemResourceService, useValue: apiSpy },
        { provide: ApiHeaderResourceService, useValue: headersSpy },
        { provide: AasClientService, useValue: aasClientSpy },
        { provide: CreateTargetSystemAasService, useValue: aasServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CreateTargetSystemComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  /**
   * Verifies that the CreateTargetSystemComponent instance is successfully created.
   */
  it('should create', () => {
    expect(component).toBeTruthy();
  });

  /**
   * Tests the save method to ensure it calls the API service and advances the wizard step
   * when the form contains valid input values.
   */
  it('save should call api.create and advance step', fakeAsync(() => {
    apiSpy.create.and.returnValue(of({ id: 10 } as any));
    component.form.patchValue({ name: 't', apiUrl: 'https://x' });

    component.save();
    tick();

    expect(apiSpy.create).toHaveBeenCalled();
    expect(component['createdTargetSystemId']).toBe(10);
    expect(component['currentStep']).toBe(1);
  }));

  /**
   * Verifies that the cancel method resets component visibility flags
   * and emits a visibleChange event with a false value.
   */
  it('cancel should reset flags and emit visibleChange', () => {
    component.visible = true;
    const spyEmit = spyOn(component.visibleChange, 'emit');
    component.cancel();
    expect(component.visible).toBeFalse();
    expect(spyEmit).toHaveBeenCalledWith(false);
  });

  /**
   * Tests helper methods isAas() and isRest() to confirm that they correctly
   * reflect the API type set in the form.
   */
  it('isAas / isRest reflect form apiType', () => {
    component.form.patchValue({ apiType: 'AAS' });
    expect(component.isAas()).toBeTrue();
    component.form.patchValue({ apiType: 'REST_OPENAPI' });
    expect(component.isRest()).toBeTrue();
  });

  /**
   * Tests that openAasxUpload() correctly toggles the AASX upload dialog visibility
   * and resets the selected file.
   */
  it('openAasxUpload should toggle dialog and reset file', () => {
    component.aasxSelectedFile = {} as any;
    component.openAasxUpload();
    expect(component.showAasxUpload).toBeTrue();
    expect(component.aasxSelectedFile).toBeNull();
  });

  /**
   * Verifies that canProceedFromStep1() correctly enforces AAS connection validation
   * before proceeding to the next step in the wizard.
   */
  it('canProceedFromStep1 requires AAS test ok when apiType is AAS', () => {
    component.form.patchValue({ name: 'N', apiUrl: 'http://x', apiType: 'AAS', aasId: 'aas-1' });
    (component as any).aasTestOk = null;
    expect(component.canProceedFromStep1()).toBeFalse();
    (component as any).aasTestOk = true;
    expect(component.canProceedFromStep1()).toBeTrue();
  });

  /**
   * Tests the goNext() method to confirm that it triggers save() when no Target System
   * has been created yet and advances the wizard accordingly.
   */
  it('goNext at step 0 should call save when not created yet', fakeAsync(() => {
    apiSpy.create.and.returnValue(of({ id: 11 } as any));
    component.form.patchValue({ name: 't', apiUrl: 'https://x' });
    component['currentStep'] = 0;

    component.goNext();
    tick();

    expect(apiSpy.create).toHaveBeenCalled();
    expect(component['createdTargetSystemId']).toBe(11);
  }));

  /**
   * Verifies that finish() emits the created event, resets the component state,
   * and hides the dialog when a Target System creation is completed.
   */
  it('finish should emit created and reset state', () => {
    const emitSpy = spyOn(component.created, 'emit');
    (component as any).createdTargetSystemId = 99;
    component.finish();
    expect(emitSpy).toHaveBeenCalled();
    expect(component.visible).toBeFalse();
    expect(component['currentStep']).toBe(0);
  });
});
