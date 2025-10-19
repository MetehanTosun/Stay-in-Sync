import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EdcInstancesComponent } from './edc-instances.component';

describe('EdcInstancesComponent', () => {
  let component: EdcInstancesComponent;
  let fixture: ComponentFixture<EdcInstancesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EdcInstancesComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(EdcInstancesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
