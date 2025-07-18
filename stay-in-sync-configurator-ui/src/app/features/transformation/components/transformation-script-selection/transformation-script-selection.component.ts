import {Component, EventEmitter, Input, numberAttribute, Output} from '@angular/core';
import {Button} from "primeng/button";
import {PrimeTemplate} from "primeng/api";
import {TableModule} from "primeng/table";
import {TransformationService} from '../../services/transformation.service';
import {UpdateTransformationRequest} from '../../models/transformation.model';
import {Router} from '@angular/router';
import {ScriptEditorNavigationService} from '../../../script-editor/script-editor-navigation.service';
import {TransformationScript} from '../../models/transformation-script.model';
import {TransformationScriptService} from '../../services/transformation-script.service';
import {Dialog} from 'primeng/dialog';
import {FormsModule} from '@angular/forms';
import {InputText} from 'primeng/inputtext';

@Component({
  selector: 'app-transformation-script-selection',
  imports: [
    Button,
    PrimeTemplate,
    TableModule,
    Dialog,
    FormsModule,
    InputText
  ],
  templateUrl: './transformation-script-selection.component.html',
  styleUrl: './transformation-script-selection.component.css'
})
export class TransformationScriptSelectionComponent {
  @Output() closeDialog = new EventEmitter<void>();
  @Output() transformationUpdated = new EventEmitter<void>();
  @Input({transform: numberAttribute}) transformationId: number | null = null;
  items: TransformationScript[] = [];
  displayCreateDialog: boolean = false;
  newScript: TransformationScript = {};

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
        const updateRequest: UpdateTransformationRequest = {
          id: transformation.id,
          syncJobId: transformation.syncJobId ?? null,
          sourceSystemEndpointIds: [], // oder transformation.sourceSystemEndpointIds ?? []
          targetSystemEndpointId: transformation.targetSystemEndpointId ?? null,
          transformationRuleId: transformation.transformationRuleId ?? null,
          transformationScriptId: item.id
        };
        console.log('UpdateTransformationRequest:', updateRequest);
        this.transformationService.update(updateRequest).subscribe(updated => {
          console.log('Update Response:', updated);
          this.closeDialog.emit();
          this.closeDialog.emit();
          this.transformationUpdated.emit();
        });
      });
    }
  }


  createNewScript() {
    this.displayCreateDialog = true;
  }

  private loadScripts() {
    this.transformationScriptService.getAll().subscribe(scripts => {
      this.items = scripts;
    })
  }

  cancelCreateScriptDialog() {
    this.displayCreateDialog = false;
  }
//TODO: Fix
  createScriptShell() {
    this.transformationScriptService.create(this.newScript).subscribe(
      (createdScript) => {
        console.log('Script created:', createdScript);
        this.displayCreateDialog = false;
        this.loadScripts();
        this.scriptEditorNavigationService.navigateToScriptEditor({
          id: String(createdScript.id),
          transformationId: this.transformationId ?? 0,
          scriptName: createdScript.name
        });
      },
      (error) => {
        console.error('Error creating script:', error);
      }
    );
  }
}
