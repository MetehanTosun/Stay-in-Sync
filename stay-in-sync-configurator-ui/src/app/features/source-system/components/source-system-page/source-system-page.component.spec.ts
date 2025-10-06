import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SourceSystemPageComponent } from './source-system-page.component';

describe('SourceSystemPageComponent', () => {
  let component: SourceSystemPageComponent;
  let fixture: ComponentFixture<SourceSystemPageComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SourceSystemPageComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SourceSystemPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
