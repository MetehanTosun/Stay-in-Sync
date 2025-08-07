import { Component, OnInit } from '@angular/core';
import { GrafanaService } from './grafana.service';

@Component({
  selector: 'app-grafana',
  template: `
    <div *ngIf="snapshots.length > 0; else loading">
      <div *ngFor="let snapshot of snapshots" class="snapshot-card">
        <h3>{{ snapshot.name }}</h3>
        <iframe
          [src]="snapshot.url"
          width="100%"
          height="600"
          style="border:1px solid #ccc;"
        ></iframe>
      </div>
    </div>
    <ng-template #loading>
      <p>Snapshots werden geladen...</p>
    </ng-template>
  `,
  styles: [`
    .snapshot-card {
      margin-bottom: 2rem;
    }
  `]
})
export class GrafanaComponent implements OnInit {
  snapshots: any[] = [];

  constructor(private grafanaService: GrafanaService) {}

  ngOnInit(): void {
    this.grafanaService.getSnapshots().subscribe(data => {
      this.snapshots = data;
    });
  }
}
