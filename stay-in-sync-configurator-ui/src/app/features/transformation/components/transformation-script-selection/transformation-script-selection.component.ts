import {Component, EventEmitter, Input, numberAttribute, Output} from '@angular/core';
import {Button} from "primeng/button";
import {PrimeTemplate} from "primeng/api";
import {TableModule} from "primeng/table";
import {TransformationService} from '../../services/transformation.service';
import {Transformation} from '../../models/transformation.model';
import {Router} from '@angular/router';
import {ScriptEditorNavigationService} from '../../../script-editor/script-editor-navigation.service';
import {TransformationScript} from '../../models/transformation-script.model';
import {TransformationScriptService} from '../../services/transformation-script.service';

@Component({
  selector: 'app-transformation-script-selection',
    imports: [
        Button,
        PrimeTemplate,
        TableModule
    ],
  templateUrl: './transformation-script-selection.component.html',
  styleUrl: './transformation-script-selection.component.css'
})
export class TransformationScriptSelectionComponent {
  @Output() closeDialog = new EventEmitter<void>();
  @Input({transform: numberAttribute}) transformationId: number | null = null;
  items: TransformationScript[] = [];
  private selectedTransformation: Transformation = {};

  constructor(private transformationService: TransformationService, private router: Router, private scriptEditorNavigationService: ScriptEditorNavigationService, private transformationScriptService: TransformationScriptService) {
    this.loadScripts();
  }

  viewDetails(item: any) {
    console.log('Viewing details for:', item);
  }

  useItem(item: any) {
    console.log('Using item:', item);
    if (this.transformationId !== null) {
      this.transformationService.getById(this.transformationId).subscribe(transformation => {
        this.selectedTransformation = transformation;
        this.selectedTransformation.transformationScript = item;
        this.transformationService.update(transformation);
        this.closeDialog.emit();
      });
    }
  }

  createNewScript() {
    //this.scriptEditorNavigationService.navigateToScriptEditor(this.transformationId)
    this.router.navigate(['/script-editor', this.transformationId]);
  }

  private loadScripts() {
    this.transformationScriptService.getAll().subscribe(scripts => {
      this.items = scripts;
    })
  }
}
