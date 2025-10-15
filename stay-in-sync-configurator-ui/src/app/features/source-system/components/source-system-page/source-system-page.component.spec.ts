import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';

import { SourceSystemPageComponent } from './source-system-page.component';

describe('SourceSystemPageComponent', () => {
  let component: SourceSystemPageComponent;
  let fixture: ComponentFixture<SourceSystemPageComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, RouterTestingModule, SourceSystemPageComponent]
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
