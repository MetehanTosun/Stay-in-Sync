import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Dialog } from 'primeng/dialog';
import { Button } from 'primeng/button';
/**
 * This class handles the logic of the modal to set a node's name
 */
@Component({
  selector: 'app-set-node-name-modal',
  standalone: true,
  imports: [FormsModule, Dialog, Button],
  templateUrl: './set-node-name-modal.component.html',
  styleUrls: ['../modal-shared.component.css', './set-node-name-modal.component.css']
})
export class SetNodeNameModalComponent implements OnChanges, OnInit {
  /** Controls dialog visibility (two-way binding with `visibleChange`) */
  @Input() visible = true;

  /** Emits when dialog visibility changes (two-way binding with `visible`) */
  @Output() visibleChange = new EventEmitter<boolean>();

  /** The current node name when editing an existing node (empty when creating) */
  @Input() currentName = '';

  /** Emitted when the user saves the new name */
  @Output() save = new EventEmitter<string>();

  /** Emitted when the modal is closed without saving */
  @Output() close = new EventEmitter<void>();

  /** User input for the new node name */
  newName = '';

  /** Load the current name */
  ngOnInit() {
    this.newName = this.currentName;
  }

  /**
   * Syncs editor content when the modal visibility or provided `currentName` change.
   */
  ngOnChanges(changes: SimpleChanges) {
    if (!changes['visible'] && !changes['currentName']) return;
    this.newName = this.currentName || '';
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
    this.visible = false;
    this.visibleChange.emit(false);
  }
}
