import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ArcAPIService } from '../../../service/api/arc-api.service';
import { CommonModule } from '@angular/common';
import { MessageService } from 'primeng/api';
import { Dialog } from 'primeng/dialog';
import { Button } from 'primeng/button';

/**
 * This component manages the modal for setting the values of a provider node
 */
@Component({
  selector: 'app-set-json-path-modal',
  standalone: true,
  imports: [FormsModule, CommonModule, Dialog, Button],
  templateUrl: './set-json-path-modal.component.html',
  styleUrls: ['../modal-shared.component.css', './set-json-path-modal.component.css']
})
export class SetJsonPathModalComponent {

  /** Controls dialog visibility (two-way binding with `visibleChange`) */
  @Input() visible = true;

  /** Emits when dialog visibility changes (two-way binding with `visible`) */
  @Output() visibleChange = new EventEmitter<boolean>();

  /** The currently JSON path to be edited (empty when creating) */
  @Input() currentJsonPath: string = '';

  /** Emitted when the user creates a new provider node (payload: { jsonPath, outputType }) */
  @Output() providerCreated = new EventEmitter<{ jsonPath: string, outputType: string }>();

  /** Emitted when the user saves changes to an existing provider node (payload: { jsonPath, outputType }) */
  @Output() save = new EventEmitter<{ jsonPath: string, outputType: string }>();

  /** Emitted when the modal closes */
  @Output() modalsClosed = new EventEmitter<void>();

  /** Cached mapping of extracted JSON paths and their output types (populated from API) */
  jsonPaths: { [key: string]: string } = {};

  /** JSON path the user selects */
  jsonPath: string = '';

  /** Whether the suggestions dropdown is currently visible */
  showSuggestions: boolean = false;

  /** Filtered list of suggestion paths matching current input */
  filteredPaths: string[] = [];

  constructor(
    private arcApi: ArcAPIService,
    private messageService: MessageService
  ) { }

  /**
   * Load JSON paths and current JSON path if available
   */
  ngOnInit() {

    // Remove leading `source.` prefix when showing/editing the path in the modal
    this.jsonPath = this.currentJsonPath.replace(/^source\./, '') || '';

    // Load paths for autocomplete and initialize the filtered list
    this.arcApi.getJsonPaths().subscribe(paths => {
      this.jsonPaths = paths;
      this.filteredPaths = this.getJsonPathKeys();
    });
  }

  onInputChange(event: any) {
    const value = event.target.value;
    if (value.trim() === '') {
      this.filteredPaths = this.getJsonPathKeys();
    } else {
      this.filteredPaths = this.getJsonPathKeys().filter(path =>
        path.toLowerCase().includes(value.toLowerCase())
      );
    }
  }

  getFilteredPaths(): string[] {
    if (this.filteredPaths.length === 1 && this.filteredPaths[0] === this.jsonPath) {
      return [];
    }
    return this.filteredPaths;
  }

  selectPath(path: string) {
    this.jsonPath = path;
    this.filteredPaths = this.getJsonPathKeys().filter(p =>
      p.toLowerCase().includes(path.toLowerCase())
    );
  }

  /**
 * Returns the keys of the jsonPaths object for use in the template
 */
  getJsonPathKeys(): string[] {
    return Object.keys(this.jsonPaths);
  }

  isValidArrayPath(path: string): boolean {
    if (path.includes('[*]')) {
      return false;
    }

    // Check if all array notations have valid indices
    const arrayMatches = path.match(/\[([^\]]*)\]/g);
    if (arrayMatches) {
      return arrayMatches.every(match => {
        const index = match.slice(1, -1); // Remove the brackets [ and ]
        return /^\d+$/.test(index); // Check if it's a valid number
      });
    }
    return true;
  }

  //#region Modal Methods
  /**
   * Concludes the provider node creation by forwarding the properties to node creation
   *
   * @returns
   */
  submit() {
    if (!this.jsonPath.trim()) {
      this.messageService.add({
        severity: 'warn',
        summary: 'No JSON Path',
        detail: "Please enter a JSON Path"
      })
      return;
    }

    if (!this.isValidArrayPath(this.jsonPath)) {
      this.messageService.add({
        severity: 'warn',
        summary: 'No valid array index',
        detail: "Please replace [*] with a specific array index (e.g. [0], [1], ...)"
      })
      return;
    }

    const finalJsonPath = this.currentJsonPath.includes('source.')
      ? `${this.jsonPath.trim()}`
      : `source.${this.jsonPath.trim()}`;

    const normalizedPath = this.jsonPath.replace(/\[\d+\]/g, '[*]');
    const outputType = this.jsonPaths[normalizedPath] || 'unknown';

    const nodeData = {
      jsonPath: finalJsonPath,
      outputType: outputType,
    };

    (this.currentJsonPath.trim() !== '' ? this.save : this.providerCreated).emit(nodeData);
    this.closeModal();
  }

  /**
   * Closes this modal
   */
  closeModal() {
    this.jsonPath = '';
    this.modalsClosed.emit();
    this.visible = false;
    this.visibleChange.emit(false);
  }
  //#endregion
}
