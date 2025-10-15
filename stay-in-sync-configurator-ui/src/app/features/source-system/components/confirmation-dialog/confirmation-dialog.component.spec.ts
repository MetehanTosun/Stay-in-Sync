/** Unit tests for `ConfirmationDialogComponent`. */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { ConfirmationDialogComponent } from './confirmation-dialog.component';


/** Verifies dialog behavior, events, and bindings. */
describe('ConfirmationDialogComponent', () => {
  let component: ConfirmationDialogComponent;
  let fixture: ComponentFixture<ConfirmationDialogComponent>;

  /** Configure testing module with standalone component and no animations. */
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ConfirmationDialogComponent],
      providers: [provideNoopAnimations()]
    }).compileComponents();

    fixture = TestBed.createComponent(ConfirmationDialogComponent);
    component = fixture.componentInstance;
    component.visible = true;
    fixture.detectChanges();
  });

  /** Should instantiate the component. */
  it('should create', () => {
    expect(component).toBeTruthy();
  });

  /** Emits confirmed and closes on confirm. */
  it('emits confirmed and closes on confirm', () => {
    spyOn(component.confirmed, 'emit');
    spyOn(component.visibleChange, 'emit');
    component.onConfirm();
    expect(component.confirmed.emit).toHaveBeenCalled();
    expect(component.visible).toBeFalse();
    expect(component.visibleChange.emit).toHaveBeenCalledWith(false);
  });

  /** Emits cancelled and closes on cancel. */
  it('emits cancelled and closes on cancel', () => {
    spyOn(component.cancelled, 'emit');
    spyOn(component.visibleChange, 'emit');
    component.onCancel();
    expect(component.cancelled.emit).toHaveBeenCalled();
    expect(component.visible).toBeFalse();
    expect(component.visibleChange.emit).toHaveBeenCalledWith(false);
  });

  /** Reflects severity via button and icon classes. */
  it('returns proper classes for severity', () => {
    component.data = { ...component.data, severity: 'danger', title: '', message: '' };
    fixture.detectChanges();
    expect(component.getSeverityClass()).toContain('danger');
    expect(component.getIconClass()).toContain('exclamation');
  });

  /** Binds header and message text from `data`. */
  it('binds header and message from data', () => {
    component.data = { ...component.data, title: 'Delete item', message: 'Are you sure?' };
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    // PrimeNG dialog header text
    const headerEl = compiled.querySelector('.p-dialog-header') || compiled.querySelector('.p-dialog .p-dialog-header');
    expect(headerEl?.textContent || '').toContain('Delete item');
    // Message text
    expect(compiled.querySelector('.confirmation-message')?.textContent || '').toContain('Are you sure?');
  });

  /** Falls back to default button labels when not provided. */
  it('uses default button labels when none provided', () => {
    component.data = { ...component.data, confirmLabel: undefined, cancelLabel: undefined };
    fixture.detectChanges();
    const buttons = fixture.nativeElement.querySelectorAll('button');
    const labels = Array.from(buttons).map((b: any) => (b.textContent || '').trim());
    expect(labels.join(' ')).toMatch(/Cancel/i);
    expect(labels.join(' ')).toMatch(/Confirm/i);
  });

  /** Displays custom button labels when provided. */
  it('uses custom button labels when provided', () => {
    component.data = { ...component.data, confirmLabel: 'Yes, delete', cancelLabel: 'Nope' };
    fixture.detectChanges();
    const buttons = fixture.nativeElement.querySelectorAll('button');
    const labels = Array.from(buttons).map((b: any) => (b.textContent || '').trim());
    expect(labels).toContain('Nope');
    expect(labels).toContain('Yes, delete');
  });
});
