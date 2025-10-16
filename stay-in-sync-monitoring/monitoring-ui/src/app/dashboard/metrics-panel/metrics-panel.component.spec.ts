import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { MetricsPanelComponent } from './metrics-panel.component';
import { ActivatedRoute } from '@angular/router';
import { of, throwError } from 'rxjs';
import { SafeUrlPipe } from './safe.url.pipe';
import { TransformationService } from '../../core/services/transformation.service';
import { ConfigService } from '../../core/services/config.service';

describe('MetricsPanelComponent', () => {
  let fixture: ComponentFixture<MetricsPanelComponent>;
  let component: MetricsPanelComponent;

  let mockRoute: any;
  let mockTransformationService: jasmine.SpyObj<TransformationService>;
  let mockConfigService: jasmine.SpyObj<ConfigService>;

  beforeEach(async () => {
    mockRoute = {
      queryParams: of({})
    };

    mockTransformationService = jasmine.createSpyObj('TransformationService', ['getTransformations']);
    mockConfigService = jasmine.createSpyObj('ConfigService', ['getGrafanaBaseUrl']);
    mockConfigService.getGrafanaBaseUrl.and.returnValue(Promise.resolve('https://grafana.local/dashboard'));

    await TestBed.configureTestingModule({
      imports: [MetricsPanelComponent, SafeUrlPipe],
      providers: [
        { provide: ActivatedRoute, useValue: mockRoute },
        { provide: TransformationService, useValue: mockTransformationService },
        { provide: ConfigService, useValue: mockConfigService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(MetricsPanelComponent);
    component = fixture.componentInstance;
  });

  // --- ✅ 1. Smoke test
  it('should create', () => {
    expect(component).toBeTruthy();
  });

  // --- ✅ 2. should build grafana URL without selected node
  it('should build grafanaUrl when no node selected', fakeAsync(async () => {
    mockRoute.queryParams = of({});
    fixture.detectChanges();
    await mockConfigService.getGrafanaBaseUrl.calls.mostRecent().returnValue;
    tick();

    expect(component.grafanaUrl).toContain('https://grafana.local/dashboard?orgId=');
  }));

  // --- ✅ 3. should handle POLL_ node
  it('should set isPollingNode true and build url accordingly', fakeAsync(async () => {
    mockRoute.queryParams = of({ input: 'POLL_NODE1' });
    fixture.detectChanges();
    await mockConfigService.getGrafanaBaseUrl.calls.mostRecent().returnValue;
    tick();

    expect(component.isPollingNode).toBeTrue();
    expect(component.pollingNodeName).toBe('NODE1');
    expect(component.grafanaUrl).toContain('var-WorkerpodName=NODE1');
  }));

  // --- ✅ 4. should load transformations and build url for normal node
  it('should load transformations for a normal node', fakeAsync(async () => {
    const mockTransformations = [{ id: 10 }, { id: 20 }];
    mockTransformationService.getTransformations.and.returnValue(of(mockTransformations));
    mockRoute.queryParams = of({ input: '123' });

    fixture.detectChanges();
    await mockConfigService.getGrafanaBaseUrl.calls.mostRecent().returnValue;
    tick();

    expect(mockTransformationService.getTransformations).toHaveBeenCalledWith('123');
    expect(component.transformationIds).toEqual([10, 20]);
    expect(component.grafanaUrl).toContain('var-transformationId=10');
    expect(component.grafanaUrl).toContain('var-transformationId=20');
  }));

  // --- ✅ 5. should handle error when transformationService fails
  it('should build grafanaUrl even if transformations fail', fakeAsync(async () => {
    spyOn(console, 'error');
    mockTransformationService.getTransformations.and.returnValue(throwError(() => new Error('fail')));
    mockRoute.queryParams = of({ input: '999' });

    fixture.detectChanges();
    await mockConfigService.getGrafanaBaseUrl.calls.mostRecent().returnValue;
    tick();

    expect(console.error).toHaveBeenCalled();
    expect(component.grafanaUrl).toContain('https://grafana.local/dashboard?orgId=');
  }));
});
