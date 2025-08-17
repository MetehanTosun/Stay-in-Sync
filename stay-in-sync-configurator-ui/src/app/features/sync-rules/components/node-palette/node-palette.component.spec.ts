import { ComponentFixture, TestBed } from '@angular/core/testing';

import { NodePaletteComponent } from './node-palette.component';

describe('NodePaletteComponent', () => {
  let component: NodePaletteComponent;
  let fixture: ComponentFixture<NodePaletteComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NodePaletteComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(NodePaletteComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
