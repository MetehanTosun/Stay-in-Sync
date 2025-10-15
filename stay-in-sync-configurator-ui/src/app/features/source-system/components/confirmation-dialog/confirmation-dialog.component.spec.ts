import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ConfirmationDialogComponent } from './confirmation-dialog.component';


describe('ConfirmationDialogComponent', () => {
  let component: ConfirmationDialogComponent;
  let fixture: ComponentFixture<ConfirmationDialogComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ConfirmationDialogComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(ConfirmationDialogComponent);
    component = fixture.componentInstance;
    component.visible = true;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('emits confirmed and closes on confirm', () => {
    spyOn(component.confirmed, 'emit');
    spyOn(component.visibleChange, 'emit');
    component.onConfirm();
    expect(component.confirmed.emit).toHaveBeenCalled();
    expect(component.visible).toBeFalse();
    expect(component.visibleChange.emit).toHaveBeenCalledWith(false);
  });

  it('emits cancelled and closes on cancel', () => {
    spyOn(component.cancelled, 'emit');
    spyOn(component.visibleChange, 'emit');
    component.onCancel();
    expect(component.cancelled.emit).toHaveBeenCalled();
    expect(component.visible).toBeFalse();
    expect(component.visibleChange.emit).toHaveBeenCalledWith(false);
  });

  it('returns proper classes for severity', () => {
    component.data = { ...component.data, severity: 'danger', title: '', message: '' };
    expect(component.getSeverityClass()).toContain('danger');
    expect(component.getIconClass()).toContain('exclamation');
  });
});
