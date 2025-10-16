import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { ErrorSnapshotPanelComponent } from './error-snapshot-panel.component';
import { TransformationService } from '../../core/services/transformation.service';
import { SnapshotService } from '../../core/services/snapshot.service';
import { ActivatedRoute, Router } from '@angular/router';
import { of } from 'rxjs';
import { TableModule } from 'primeng/table';
import { Button } from 'primeng/button';
import { NgIf, NgForOf } from '@angular/common';
import { Panel } from 'primeng/panel';
import { Message } from 'primeng/message';
import { PrimeTemplate } from 'primeng/api';
import { DatePipe } from '@angular/common';
import {NoopAnimationsModule} from '@angular/platform-browser/animations';

describe('ErrorSnapshotPanelComponent', () => {
  let component: ErrorSnapshotPanelComponent;
  let fixture: ComponentFixture<ErrorSnapshotPanelComponent>;
  let transformationService: jasmine.SpyObj<TransformationService>;
  let snapshotService: jasmine.SpyObj<SnapshotService>;
  let router: jasmine.SpyObj<Router>;

  beforeEach(waitForAsync(() => {
    const transformationMock = jasmine.createSpyObj('TransformationService', ['getTransformations']);
    const snapshotMock = jasmine.createSpyObj('SnapshotService', ['getLastFiveSnapshots']);
    const routerMock = jasmine.createSpyObj('Router', ['navigate']);

    TestBed.configureTestingModule({
      imports: [
        ErrorSnapshotPanelComponent,
        TableModule,
        Button,
        NgIf,
        NgForOf,
        Panel,
        Message,
        PrimeTemplate,
        DatePipe,
        NoopAnimationsModule
      ],
      providers: [
        { provide: TransformationService, useValue: transformationMock },
        { provide: SnapshotService, useValue: snapshotMock },
        { provide: Router, useValue: routerMock },
        { provide: ActivatedRoute, useValue: { queryParams: of({ input: 'NODE1' }) } }
      ]
    }).compileComponents();

    transformationService = TestBed.inject(TransformationService) as jasmine.SpyObj<TransformationService>;
    snapshotService = TestBed.inject(SnapshotService) as jasmine.SpyObj<SnapshotService>;
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;

    class MockEventSource {
      addEventListener = jasmine.createSpy();
      close = jasmine.createSpy();
      onerror: any = null;
    }
    Object.defineProperty(window, 'EventSource', { value: MockEventSource });

    fixture = TestBed.createComponent(ErrorSnapshotPanelComponent);
    component = fixture.componentInstance;
  }));

  it('should create the component', () => {
    expect(component).toBeTruthy();
  });

  it('should load transformations and snapshots on init', () => {
    const transformations = [
      { id: 1, name: 'Transform 1' },
      { id: 2, name: 'Transform 2' }
    ];
    const snapshots = [
      { snapshotId: '101', createdAt: new Date().toString(), transformationId: 1 },
      { snapshotId: '102', createdAt: new Date().toString(), transformationId: 2 }
    ];

    transformationService.getTransformations.and.returnValue(of(transformations));
    snapshotService.getLastFiveSnapshots.and.returnValue(of(snapshots));

    fixture.detectChanges();

    expect(component.transformations.length).toBe(2);
    expect(component.transformationSnapshots.get(1)?.length).toBe(2);
    expect(component.transformationSnapshots.get(2)?.length).toBe(2);
  });

  it('should navigate to replay snapshot', () => {
    const snapshotId = 101;
    component.replaySnapshot(snapshotId);
    expect(router.navigate).toHaveBeenCalledWith(['/replay'], { queryParams: { snapshotId } });
  });

  it('should mark transformations as error on SSE update', () => {
    const transformations = [
      { id: 1, name: 'Transform 1', error: false },
      { id: 2, name: 'Transform 2', error: false }
    ];
    transformationService.getTransformations.and.returnValue(of(transformations));
    snapshotService.getLastFiveSnapshots.and.returnValue(of([]));

    fixture.detectChanges();

    const sseInstance = (component as any).sse;
    const mockData = JSON.stringify([1]);
    const event = { data: mockData };

    sseInstance.addEventListener.calls.allArgs().forEach(([eventName, callback]: any) => {
      if (eventName === 'transformation-update') {
        callback(event);
      }
    });

    expect(component.transformations[0].error).toBeTrue();
    expect(component.transformations[1].error).toBeFalse();
  });

  it('should handle no transformations gracefully', () => {
    transformationService.getTransformations.and.returnValue(of([]));
    snapshotService.getLastFiveSnapshots.and.returnValue(of([]));

    fixture.detectChanges();

    expect(component.transformations.length).toBe(0);
    expect(component.transformationSnapshots.size).toBe(0);
  });
});
