import {Component, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {NgClass, NgForOf, NgIf, NgStyle} from '@angular/common';
import {SnapshotModel} from '../../core/models/snapshot.model';
import {TransformationService} from '../../core/services/transformation.service';
import {Panel} from 'primeng/panel';
import {Message} from 'primeng/message';
import {PrimeTemplate} from 'primeng/api';

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
    NgStyle
  ],
  styleUrl: './error-snapshot-panel.component.css'
})
export class ErrorSnapshotPanelComponent implements OnInit {
  selectedNodeId?: string;
  errorSnapshots: string[] = []; // Beispiel-Daten
  filteredSnapshots: SnapshotModel[] = [];
  transformations: any[] = [];

  constructor(private route: ActivatedRoute, private transformationService: TransformationService) {}

  ngOnInit() {
    this.route.queryParams.subscribe(params => {
      this.selectedNodeId = params['input'];
      this.filterSnapshots();
    });

    this.getTransformations();

    // Beispiel-Daten initialisieren
    this.filteredSnapshots = [{ message: 'Error1', transformationId: '1'}, { message: 'Error2', transformationId: '1'}];
  }

  filterSnapshots() {
  }

  getTransformations() {
    if (this.selectedNodeId) {
      this.transformationService.getTransformations(this.selectedNodeId).subscribe(data => {
        this.transformations = data;
      });
    }
  }
}
