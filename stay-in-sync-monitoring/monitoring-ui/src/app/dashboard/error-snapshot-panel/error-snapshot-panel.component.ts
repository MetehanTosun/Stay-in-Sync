import {Component, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {NgForOf, NgIf} from '@angular/common';

@Component({
  selector: 'app-error-snapshot-panel',
  templateUrl: './error-snapshot-panel.component.html',
  imports: [
    NgIf,
    NgForOf
  ],
  styleUrl: './error-snapshot-panel.component.css'
})
export class ErrorSnapshotPanelComponent implements OnInit {
  selectedNodeId?: string;
  errorSnapshots: string[] = []; // Beispiel-Daten
  filteredSnapshots: string[] = [];

  constructor(private route: ActivatedRoute) {}

  ngOnInit() {
    this.route.queryParams.subscribe(params => {
      this.selectedNodeId = params['input'];
      this.filterSnapshots();
    });

    // Beispiel-Daten initialisieren
    this.errorSnapshots = ['Error 1', 'Error 2', 'Error 3'];
  }

  filterSnapshots() {
  }
}
