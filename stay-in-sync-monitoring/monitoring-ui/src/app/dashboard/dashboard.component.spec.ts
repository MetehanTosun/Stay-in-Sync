import { Component, EventEmitter, Input, Output, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { provideRouter } from '@angular/router';
import { DashboardComponent } from './dashboard.component';

@Component({
  selector: 'app-graph-panel',
  template: '',
  standalone: true
})
class MockGraphPanelComponent {
  @Input() searchTerm!: string;
  @Output() nodeSelected = new EventEmitter<string | null>();
}

@Component({
  selector: 'app-search-bar',
  template: '',
  standalone: true
})
class MockSearchBarComponent {
  @Output() search = new EventEmitter<string>();
}

describe('DashboardComponent (with provideRouter + shallow mocks)', () => {
  let fixture: ComponentFixture<DashboardComponent>;
  let component: DashboardComponent;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    await TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [
        { provide: Router, useValue: routerSpy },
        provideRouter([])
      ]
    })
      .overrideComponent(DashboardComponent, {
        set: {
          imports: [
            CommonModule,
            MockGraphPanelComponent,
            MockSearchBarComponent
          ],
          // 👇 Das ist der entscheidende Fix!
          schemas: [CUSTOM_ELEMENTS_SCHEMA]
        }
      })
      .compileComponents();

    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('onSearch should update searchTerm', () => {
    component.onSearch('test');
    expect(component.searchTerm).toBe('test');
  });

  it('onNodeSelected should navigate correctly', () => {
    component.onNodeSelected('abc');
    expect(routerSpy.navigate).toHaveBeenCalledWith([], {
      queryParams: { input: 'abc' },
      queryParamsHandling: 'merge'
    });
    expect(component.selectedNodeId).toBe('abc');
  });
});
