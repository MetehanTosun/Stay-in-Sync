import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { CreateSourceSystemComponent } from '../create-source-system/create-source-system.component'; // Pfad anpassen, falls nötig
// biome-ignore lint/style/useImportType: <explanation>
import { AasService } from '../../../../app/aas.service'; // Pfad anpassen
import { TableModule } from 'primeng/table'; // Falls du eine Tabelle verwenden möchtest
// Dummy-Interface für SourceSystem, ersetze es durch dein echtes Modell
interface SourceSystem {
  id: string;
  name: string;
  type: 'AAS' | 'REST';
  aasId?: string;
  endpoint?: string;
}

@Component({
  selector: 'app-source-system-base',
  standalone: true,
  imports: [
    CommonModule,
    ButtonModule,
    DialogModule,
    CreateSourceSystemComponent,
    TableModule // Falls du eine Tabelle verwenden möchtest, importiere TableModule
  ],
  templateUrl: './source-system-base.component.html',
  styleUrls: ['./source-system-base.component.css']
})
export class SourceSystemBaseComponent implements OnInit {
  showCreateDialog = false;
  systems: SourceSystem[] = []; // Hier deine Source Systems speichern
  isLoadingSystems = false;

  // Beispielhafte Daten, ersetze dies durch echte Service-Aufrufe
  private nextId = 1;

  constructor(private aasService: AasService) {} // aasService nur als Beispiel, falls benötigt

  ngOnInit(): void {
    this.loadSystems();
  }

  loadSystems(): void {
    this.isLoadingSystems = true;
    // Simuliere das Laden von Systemen
    // Ersetze dies durch einen echten Service-Aufruf, um deine Systeme zu laden
    console.log('Loading source systems...');
    setTimeout(() => {
      // this.systems = []; // Beispiel: Lade echte Daten
      this.isLoadingSystems = false;
      console.log('Source systems loaded (simulated).');
    }, 1000);
  }

  openCreateDialog(): void {
    this.showCreateDialog = true;
  }

  onSourceSystemSaved(newSystemData: any): void {
    console.log('Source system creation data received in parent:', newSystemData);
    // Füge das neue System zur Liste hinzu (simuliert)
    // In einer echten Anwendung würdest du das System an ein Backend senden
    // und dann die Liste neu laden oder das neue System direkt hinzufügen.
    const newSystem: SourceSystem = {
      id: `sys-${this.nextId++}`,
      name: newSystemData.name,
      type: newSystemData.sourceType, // Annahme, dass sourceType im Payload ist
      aasId: newSystemData.aasId,
      endpoint: newSystemData.endpoint
    };
    this.systems.push(newSystem);
    console.log('New system added to list (simulated):', newSystem);

    this.showCreateDialog = false; // Schließe den Dialog
    // Optional: Lade die Liste der Systeme neu vom Server, falls das Backend die ID generiert
    // this.loadSystems();
  }

  onSourceSystemCancelled(): void {
    this.showCreateDialog = false; // Schließe den Dialog
    console.log('Source system creation cancelled.');
  }

  onDialogClose(): void {
    // Wird aufgerufen, wenn der Dialog über 'x' oder Escape geschlossen wird
    // Stellt sicher, dass der Status konsistent ist, falls der Dialog
    // auf andere Weise als durch die (saved) oder (cancelled) Events geschlossen wird.
    this.showCreateDialog = false;
    console.log('Create Source System dialog closed via onHide.');
  }
}