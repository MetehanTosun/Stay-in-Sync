import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EditRuleGraph } from './edit-rule-graph';

describe('EditRuleGraph', () => {
  let component: EditRuleGraph;
  let fixture: ComponentFixture<EditRuleGraph>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EditRuleGraph]
    })
    .compileComponents();

    fixture = TestBed.createComponent(EditRuleGraph);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
