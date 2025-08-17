import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ProviderNodeModalComponent } from './provider-node-modal.component';

describe('ProviderNodeModalComponent', () => {
  let component: ProviderNodeModalComponent;
  let fixture: ComponentFixture<ProviderNodeModalComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProviderNodeModalComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ProviderNodeModalComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
