import {Component, OnInit, OnDestroy} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {NgClass, NgForOf, NgIf, NgStyle} from '@angular/common';
import {SnapshotModel} from '../../core/models/snapshot.model';
import {TransformationService} from '../../core/services/transformation.service';
import {Panel} from 'primeng/panel';
import {Message} from 'primeng/message';
import {PrimeTemplate} from 'primeng/api';
import {Subscription} from 'rxjs';
import {TransformationModelForSnapshotPanel} from '../../core/models/transformation.model';
import {TableModule} from 'primeng/table';
import {Button} from 'primeng/button';

@Component({
  selector: 'app-error-snapshot-panel',
  templateUrl: './error-snapshot-panel.component.html',
  imports: [
    NgIf,
    NgForOf,
    Panel,
    Message,
    NgClass,
    PrimeTemplate,
    NgStyle,
    TableModule,
    Button
  ],
  styleUrl: './error-snapshot-panel.component.css'
})
export class ErrorSnapshotPanelComponent implements OnInit, OnDestroy {
  selectedNodeId?: string;
  filteredSnapshots: SnapshotModel[] = [];
  transformations: TransformationModelForSnapshotPanel[] = [];

  private routeSub?: Subscription;

  constructor(
    private route: ActivatedRoute,
    private transformationService: TransformationService,
    private router: Router
  ) {}

  ngOnInit() {
    // Auf Änderungen der Query-Parameter reagieren
    this.routeSub = this.route.queryParams.subscribe(params => {
      this.selectedNodeId = params['input'];

      // Nur wenn sich etwas geändert hat, neu laden
      if (this.selectedNodeId) {
        this.filterSnapshots();
        this.getTransformations();
      }
    });

    // Beispiel-Daten initialisieren
    this.filteredSnapshots = [
      { message: 'Error1', transformationId: 1},
      { message: 'Error2', transformationId: 1}
    ];
  }

  ngOnDestroy() {
    this.routeSub?.unsubscribe();
  }

  filterSnapshots() {
    // hier kannst du anhand von selectedNodeId filtern
  }

  getTransformations() {
    if (this.selectedNodeId) {
      this.transformationService
        .getTransformations(this.selectedNodeId)
        .subscribe(data => {
          this.transformations = data;
        });
    }
  }

  getSnapshotsForTransformation(id: number | undefined) {
    return this.filteredSnapshots.filter(s => s.transformationId === id);
  }

  replaySnapshot(id: number) {
    this.router.navigate(['/replay']);

  }
}
