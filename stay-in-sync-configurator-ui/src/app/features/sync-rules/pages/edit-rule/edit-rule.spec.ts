import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EditRule } from './edit-rule';

describe('EditRule', () => {
  let component: EditRule;
  let fixture: ComponentFixture<EditRule>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EditRule]
    })
    .compileComponents();

    fixture = TestBed.createComponent(EditRule);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
