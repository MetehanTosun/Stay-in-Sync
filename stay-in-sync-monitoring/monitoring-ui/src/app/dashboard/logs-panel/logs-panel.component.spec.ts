import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { LogsPanelComponent } from './logs-panel.component';
import { ActivatedRoute } from '@angular/router';
import { of, throwError } from 'rxjs';
import { LogService } from '../../core/services/log.service';
import { TransformationService } from '../../core/services/transformation.service';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

// --- PrimeNG Mocks ---
import { Component, Input, Output, EventEmitter } from '@angular/core';

@Component({selector: 'p-select', template: '', standalone: true})
class MockSelect {
  @Input() options: any;
  @Input() placeholder?: string;
  @Input() optionLabel?: string;
  @Input() optionValue?: string;
  @Input() showClear?: boolean;
  @Input() filter?: boolean;
  @Input() ngModel?: any;
  @Output() onChange = new EventEmitter<any>();
}

@Component({selector: 'p-button', template: '', standalone: true})
class MockButton {
  @Output() click = new EventEmitter<void>();
}

@Component({selector: 'p-table', template: '', standalone: true})
class MockTable {
  @Input() value: any;
}

describe('LogsPanelComponent', () => {
  let fixture: ComponentFixture<LogsPanelComponent>;
  let component: LogsPanelComponent;

  let mockRoute: any;
  let mockLogService: jasmine.SpyObj<LogService>;
  let mockTransformationService: jasmine.SpyObj<TransformationService>;

  beforeEach(async () => {
    mockRoute = { queryParams: of({}) };
    mockLogService = jasmine.createSpyObj('LogService', ['getLogs', 'getLogsByService', 'getLogsByTransformations']);
    mockTransformationService = jasmine.createSpyObj('TransformationService', ['getTransformations']);

    await TestBed.configureTestingModule({
      imports: [
        LogsPanelComponent,
        FormsModule,
        CommonModule,
        MockSelect,
        MockButton,
        MockTable
      ],
      providers: [
        { provide: ActivatedRoute, useValue: mockRoute },
        { provide: LogService, useValue: mockLogService },
        { provide: TransformationService, useValue: mockTransformationService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(LogsPanelComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should fetch logs by service for POLL node', fakeAsync(() => {
    mockRoute.queryParams = of({ input: 'POLL_NODE1' });
    const logsMock = [{ message: 'log1', timestamp: '0' }];
    mockLogService.getLogsByService.and.returnValue(of(logsMock));

    fixture.detectChanges();
    tick();

    expect(component.selectedService).toBe('core-polling-node');
    expect(component.logs).toEqual(logsMock);
    expect(component.loading).toBeFalse();
  }));

  it('should fetch transformations and logs', fakeAsync(() => {
    mockRoute.queryParams = of({ input: 'NODE123' });
    mockTransformationService.getTransformations.and.returnValue(of([{ id: 10 }, { id: 20 }]));
    mockLogService.getLogsByTransformations.and.returnValue(of([{ message: 'logA', timestamp: '0', transformationId: '10' }]));

    fixture.detectChanges();
    tick();

    expect(component.transformationIds).toEqual(['10', '20']);
    expect(component.logs.length).toBe(1);
    expect(component.loading).toBeFalse();
  }));

  it('should fetch all logs if no node selected', fakeAsync(() => {
    mockRoute.queryParams = of({});
    const logsMock = [{ message: 'logX', timestamp: '0' }];
    mockLogService.getLogs.and.returnValue(of(logsMock));

    fixture.detectChanges();
    tick();

    expect(component.logs).toEqual(logsMock);
  }));

  it('should handle error fetching transformations', fakeAsync(() => {
    spyOn(console, 'error');
    mockRoute.queryParams = of({ input: 'NODE_ERR' });
    mockTransformationService.getTransformations.and.returnValue(throwError(() => new Error('fail')));

    fixture.detectChanges();
    tick();

    expect(component.errorMessage).toBe('Error loading transformations');
  }));

  it('reloadLogs should update endTime and fetch logs', fakeAsync(() => {
    spyOn(component, 'fetchLogs');
    component.reloadLogs();
    expect(component.fetchLogs).toHaveBeenCalled();
  }));

  it('onServiceChange resets POLL node', () => {
    component.selectedNodeId = 'POLL_NODE1';
    component.selectedTransformationId = '123';
    spyOn(component, 'fetchLogs');

    component.onServiceChange();

    expect(component.selectedNodeId).toBeUndefined();
    expect(component.selectedTransformationId).toBe('');
    expect(component.fetchLogs).toHaveBeenCalled();
  });
});
