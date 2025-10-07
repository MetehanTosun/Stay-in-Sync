import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ManageApiHeadersComponent } from './manage-api-headers.component';

describe('ManageApiHeadersComponent', () => {
  let component: ManageApiHeadersComponent;
  let fixture: ComponentFixture<ManageApiHeadersComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ManageApiHeadersComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ManageApiHeadersComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should filter Accept/Content-Type when isAas=true', () => {
    component.isAas = true;
    fixture.detectChanges();
    const types = component.allowedHeaderTypes.map(t => t.value);
    expect(types).not.toContain('Accept');
    expect(types).not.toContain('ContentType');
  });
});
