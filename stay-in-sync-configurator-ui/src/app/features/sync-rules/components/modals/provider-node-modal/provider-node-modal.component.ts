import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-provider-node-modal',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './provider-node-modal.component.html',
  styleUrl: './provider-node-modal.component.css'
})
export class ProviderNodeModalComponent {
  @Input() currentJsonPath: string = '';
  @Output() providerCreated = new EventEmitter<string>();
  @Output() save = new EventEmitter<string>();
  @Output() modalsClosed = new EventEmitter<void>();

  jsonPath: string = '';

  ngOnInit() {
    this.jsonPath = this.currentJsonPath || '';
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
    //TODO-s Check if JSON Path is valid
    (this.currentJsonPath.trim() !== '' ? this.save : this.providerCreated).emit(`source.${this.jsonPath.trim()}`);
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
