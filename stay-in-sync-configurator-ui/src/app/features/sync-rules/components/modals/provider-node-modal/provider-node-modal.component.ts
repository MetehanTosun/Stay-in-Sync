import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ArcAPIService } from '../../../service/api/arc-api.service';
import { CommonModule } from '@angular/common';

/**
 * This component manages the modal for setting the values of a provider node
 */
@Component({
  selector: 'app-provider-node-modal',
  standalone: true,
  imports: [FormsModule, CommonModule],
  templateUrl: './provider-node-modal.component.html',
  styleUrl: './provider-node-modal.component.css'
})
export class ProviderNodeModalComponent {
  @Input() currentJsonPath: string = '';
  @Output() providerCreated = new EventEmitter<{ jsonPath: string, outputType: string }>();
  @Output() save = new EventEmitter<{ jsonPath: string, outputType: string }>();
  @Output() modalsClosed = new EventEmitter<void>();

  jsonPaths: { [key: string]: string } = {};
  jsonPath: string = '';
  showSuggestions: boolean = false;
  filteredPaths: string[] = [];

  constructor(
    private arcApi: ArcAPIService
  ) { }

  ngOnInit() {
    this.arcApi.getJsonPaths().subscribe({
      next: (jsonPaths) => {
        console.log('JSON Paths:', jsonPaths);
      },
      error: (error) => {
        console.error('Error fetching JSON paths:', error);
      }
    }); // TODO-s DELETE

    this.jsonPath = this.currentJsonPath.replace(/^source\./, '') || '';

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
      alert('Please enter a JSON Path');
      return;
    }

    if (!this.isValidArrayPath(this.jsonPath)) {
      alert('Please replace [*] with a specific array index (e.g., [0], [1], [2]...)');
      return;
    }

    const finalJsonPath = this.currentJsonPath.includes('source.')
      ? `${this.jsonPath.trim()}`
      : `source.${this.jsonPath.trim()}`;

    const outputType = this.jsonPaths[this.jsonPath] || 'unknown';

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
  }
  //#endregion
}
