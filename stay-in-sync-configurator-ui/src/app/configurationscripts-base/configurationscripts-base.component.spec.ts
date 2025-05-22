import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ConfigurationscriptsBaseComponent } from './configurationscripts-base.component';

describe('ConfigurationscriptsBaseComponent', () => {
  let component: ConfigurationscriptsBaseComponent;
  let fixture: ComponentFixture<ConfigurationscriptsBaseComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ConfigurationscriptsBaseComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ConfigurationscriptsBaseComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
