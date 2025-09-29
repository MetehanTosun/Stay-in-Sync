import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
/**
 * This class handles the logic of the modal to set a node's name
 */
@Component({
  selector: 'app-set-node-name-modal',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './set-node-name-modal.component.html',
  styleUrl: './set-node-name-modal.component.css'
})
export class SetNodeNameModalComponent {
  @Input() currentName: string = '';
  @Output() save = new EventEmitter<string>();
  @Output() close = new EventEmitter<void>();

  newName: string = '';

  ngOnInit() {
    this.newName = this.currentName;
  }

  /**
   * Concludes the nodes name change by forwarding the properties to node creation
   *
   * @returns
   */
  submit() {
    this.save.emit(this.newName);
  }

  /**
   * Closes this modal
   */
  closeModal() {
    this.close.emit();
  }
}
