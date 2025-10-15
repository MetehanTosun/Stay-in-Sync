import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { MessageService } from 'primeng/api';

import { ManageApiHeadersComponent } from './manage-api-headers.component';

describe('ManageApiHeadersComponent', () => {
  let component: ManageApiHeadersComponent;
  let fixture: ComponentFixture<ManageApiHeadersComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, ManageApiHeadersComponent],
      providers: [MessageService]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ManageApiHeadersComponent);
    component = fixture.componentInstance;
    component.syncSystemId = 1;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should filter Accept/Content-Type when isAas=true', () => {
    component.isAas = true;
    fixture.detectChanges();
    const types = component.allowedHeaderTypes.map(t => String(t.value));
    expect(types.some(v => v.includes('Accept'))).toBeFalse();
    expect(types.some(v => v.toUpperCase().includes('CONTENT'))).toBeFalse();
  });
});
