import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TransformationScriptSelectionComponent } from './transformation-script-selection.component';

describe('TransformationScriptSelectionComponent', () => {
  let component: TransformationScriptSelectionComponent;
  let fixture: ComponentFixture<TransformationScriptSelectionComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TransformationScriptSelectionComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(TransformationScriptSelectionComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
