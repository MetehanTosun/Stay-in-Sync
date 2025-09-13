import { ComponentFixture, TestBed } from '@angular/core/testing';

import { JobStatusTagComponent } from './job-status-tag.component';

describe('JobStatusTagComponent', () => {
  let component: JobStatusTagComponent;
  let fixture: ComponentFixture<JobStatusTagComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [JobStatusTagComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(JobStatusTagComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
