import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ConfigBaseComponent } from './config-base.component';

describe('ConfigBaseComponent', () => {
  let component: ConfigBaseComponent;
  let fixture: ComponentFixture<ConfigBaseComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ConfigBaseComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ConfigBaseComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
