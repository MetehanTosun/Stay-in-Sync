import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { NodeType } from '../../models';

@Component({
  selector: 'app-node-palette',
  imports: [CommonModule],
  templateUrl: './node-palette.component.html',
  styleUrl: './node-palette.component.css'
})
export class NodePaletteComponent {
  @Input() showMainNodePalette = false;
  @Output() exit = new EventEmitter<void>();
  @Output() nodeSelected = new EventEmitter<NodeType>();

  selectProviderNode() {
    this.nodeSelected.emit(NodeType.PROVIDER);
    this.exit.emit();
  }

  selectLogicNode() {
    // TODO-s
  }

  selectConstantNode() {
    this.nodeSelected.emit(NodeType.CONSTANT);
    this.exit.emit();
  }
}
