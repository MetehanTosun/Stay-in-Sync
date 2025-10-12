import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { GraphPanelComponent } from './graph-panel.component';
import { MonitoringGraphService } from '../../core/services/monitoring-graph.service';
import { Router } from '@angular/router';
import { of } from 'rxjs';
import { Component, Input } from '@angular/core';

// --- Mock Legend Panel ---
@Component({selector: 'app-legend-panel', template: '', standalone: true})
class MockLegendPanel {
  @Input() data: any;
}

// --- Mocks ---
const mockGraphService = {
  getMonitoringGraphData: jasmine.createSpy('getMonitoringGraphData')
};

const mockRouter = {
  navigate: jasmine.createSpy('navigate')
};

// --- Sample Nodes & Links ---
const mockNodes = [
  { id: '1', label: 'Node 1', type: 'SyncNode', status: 'active', connections: [] },
  { id: '2', label: 'Node 2', type: 'PollingNode', status: 'inactive', connections: [] }
];

const mockLinks = [
  { source: mockNodes[0], target: mockNodes[1], status: 'active' }
];

describe('GraphPanelComponent', () => {
  let component: GraphPanelComponent;
  let fixture: ComponentFixture<GraphPanelComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GraphPanelComponent, MockLegendPanel],
      providers: [
        { provide: MonitoringGraphService, useValue: mockGraphService },
        { provide: Router, useValue: mockRouter }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(GraphPanelComponent);
    component = fixture.componentInstance;

    // Mock service response
    mockGraphService.getMonitoringGraphData.and.returnValue(of({
      nodes: mockNodes,
      connections: mockLinks
    }));
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load graph data on AfterViewInit', fakeAsync(() => {
    spyOn(component, 'updateGraph').and.callThrough();

    fixture.detectChanges();
    component.ngAfterViewInit();
    tick();

    // @ts-ignore
    expect(component.nodes).toEqual(mockNodes);
    // @ts-ignore
    expect(component.links).toEqual(mockLinks);
    expect(component.filteredNodes.length).toBe(2);
    expect(component.filteredLinks.length).toBe(1);
    expect(component.updateGraph).toHaveBeenCalled();
    expect(component['isInitialized']).toBeTrue();
  }));

  it('should filter nodes by searchTerm', fakeAsync(() => {
    fixture.detectChanges();
    component.ngAfterViewInit();
    tick();

    component.searchTerm = 'node 1';
    expect(component.filteredNodes.length).toBe(1);
    expect(component.filteredNodes[0].id).toBe('1');
  }));

  it('should emit nodeSelected on node click (SyncNode or PollingNode)', fakeAsync(() => {
    fixture.detectChanges();
    component.ngAfterViewInit();
    tick();

    spyOn(component.nodeSelected, 'emit');

    // Manually emit selection (simulate click on SyncNode)
    component.nodeSelected.emit('1');
    expect(component.nodeSelected.emit).toHaveBeenCalledWith('1');

    // Manually emit deselection
    component.nodeSelected.emit(null);
    expect(component.nodeSelected.emit).toHaveBeenCalledWith(null);
  }));

  it('should navigate to replay on error node click', fakeAsync(() => {
    fixture.detectChanges();
    component.ngAfterViewInit();
    tick();

    // Simulate node with error
    const errorNode = { id: '3', label: 'ErrorNode', type: "SyncNode", status: 'error', connections: [] };
    // @ts-ignore
    component.nodes.push(errorNode);

    // Normally D3 handles the click, here we simulate it
    component['router'].navigate(['/replay'], { queryParams: { nodeId: errorNode.id } });
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/replay'], { queryParams: { nodeId: '3' } });
  }));
});
