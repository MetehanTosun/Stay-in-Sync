/**
 * @file transformation-base.component.ts
 * @description This component serves as the base for managing transformations in the application.
 * It provides functionality for creating, editing, deleting, and managing transformations, as well
 * as handling interactions with transformation rules and scripts.
 */

import {Component, EventEmitter, OnInit, Output} from '@angular/core';
import {TableModule} from 'primeng/table';
import {Transformation, UpdateTransformationRequest} from '../../models/transformation.model';
import {Button} from 'primeng/button';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {ToggleButtonModule} from 'primeng/togglebutton';
import {TransformationTempStoreService} from '../../services/transformation.tempstore.service';
import {TransformationService} from '../../services/transformation.service';
import {Dialog} from 'primeng/dialog';
import {InputText} from 'primeng/inputtext';
import {NgIf} from '@angular/common';
import {Router} from '@angular/router';
import {TransformationScriptSelectionComponent} from '../transformation-script-selection/transformation-script-selection.component';
import {TransformationRuleSelectionComponent} from '../transformation-rule-selection/transformation-rule-selection.component';

/**
 * @class TransformationBaseComponent
 * @description Angular component for managing transformations, including their creation, editing,
 * deletion, and association with rules and scripts.
 */
@Component({
  selector: 'app-transformation-base',
  imports: [
    TableModule,
    Button,
    FormsModule,
    ToggleButtonModule,
    Dialog,
    ReactiveFormsModule,
    InputText,
    NgIf,
    TransformationScriptSelectionComponent,
    TransformationRuleSelectionComponent
  ],
  templateUrl: './transformation-base.component.html',
  styleUrl: './transformation-base.component.css'
})
export class TransformationBaseComponent implements OnInit {
  /**
   * @event transformationsChanged - Emits changes to the list of added transformations.
   */
  @Output() transformationsChanged = new EventEmitter<Transformation[]>();

  /**
   * @property {Transformation[]} transformations - List of all transformations loaded from the backend.
   */
  transformations: Transformation[] = [];

  /**
   * @property {Transformation[]} addedTransformations - List of transformations added to the temp store.
   */
  addedTransformations: Transformation[] = [];

  /**
   * @property {Transformation} selectedTransformation - Currently selected transformation for editing or association.
   */
  selectedTransformation: Transformation = {};

  /**
   * @property {boolean} displayCreateDialog - Controls the visibility of the transformation creation dialog.
   */
  displayCreateDialog: boolean = false;

  /**
   * @property {boolean} displayRuleSelectionDialog - Controls the visibility of the rule selection dialog.
   */
  displayRuleSelectionDialog: boolean = false;

  /**
   * @property {boolean} displayScriptSelectionDialog - Controls the visibility of the script selection dialog.
   */
  displayScriptSelectionDialog: boolean = false;

  /**
   * @property {Transformation} newTransformation - Represents a new transformation being created.
   */
  newTransformation: Transformation = {};

  /**
   * @constructor
   * @param {TransformationService} transformationService - Service for managing transformations.
   * @param {Router} router - Angular Router for navigation.
   * @param {TransformationTempStoreService} tempStore - Temporary store for transformations.
   */
  constructor(private transformationService: TransformationService, private router: Router, private tempStore: TransformationTempStoreService) {}

  /**
   * Lifecycle hook that is called after the component is initialized.
   * Loads transformations from the backend and initializes the component state.
   */
  ngOnInit() {
    this.transformationService.getAll().subscribe(
      (transformations: Transformation[]) => {
        this.transformations = transformations;
        this.setAddedFlagForIntersection();
        this.transformationChanged();
        console.log('Transformations from backend loaded:', this.transformations);
      },
      (error: any) => {
        console.error('Error loading transformations from backend:', error);
      }
    );
  }

  /**
   * Sets the `added` flag for transformations that are present in the temp store.
   */
  setAddedFlagForIntersection() {
    this.addedTransformations = this.tempStore.getTransformations();
    const addedIds = this.addedTransformations.map(t => t.id);
    console.log('Added Transformation IDs:', addedIds);
    console.log('All Transformations:', this.transformations);
    this.transformations.forEach(transformation => {
      transformation.added = addedIds.includes(transformation.id);
    });
  }

  /**
   * Reloads transformations from the backend and updates the component state.
   */
  loadTransformationsFromBackend() {
    this.transformationService.getAll().subscribe(
      (transformations: Transformation[]) => {
        this.transformations = transformations;
        this.addedTransformations = this.tempStore.getTransformations();
        this.setAddedFlagForIntersection();
        this.transformationChanged();
        console.log('Transformations loaded:', this.transformations);
      },
      (error: any) => {
        console.error('Error loading transformations from backend:', error);
      }
    );
  }

  /**
   * Edits the specified transformation.
   * @param {any} rowData - Data of the transformation to edit.
   */
  edit(rowData: any) {
    console.log('Edit transformation:', rowData);
  }

  /**
   * Deletes the specified transformation.
   * @param {any} rowData - Data of the transformation to delete.
   */
  delete(rowData: any) {
    this.transformationService.delete(rowData).subscribe({
      next: () => this.loadTransformationsFromBackend(),
      error: (error: any) => console.error('Error deleting transformation:', error)
    });
    console.log('Delete transformation:', rowData);
  }

  /**
   * Adds the specified transformation to the temp store.
   * @param {any} rowData - Data of the transformation to add.
   */
  add(rowData: any) {
    console.log('Adding transformation:', rowData);
    rowData.added = true;
    this.tempStore.addTransformation(rowData);
    console.log('Transformations in tempStore:', this.tempStore.getTransformations());
    this.addedTransformations = this.tempStore.getTransformations();
    this.transformationChanged();
  }

  /**
   * Removes the specified transformation from the temp store.
   * @param {any} rowData - Data of the transformation to remove.
   */
  remove(rowData: any) {
    console.log('Removing transformation:', rowData);
    rowData.added = false;
    this.tempStore.removeTransformation(rowData);
    console.log('Transformations in tempStore after removal:', this.tempStore.getTransformations());
    this.addedTransformations = this.tempStore.getTransformations();
    this.transformationChanged();
  }

  /**
   * Emits an event to notify the parent component of changes to the transformations list.
   */
  transformationChanged() {
    this.transformationsChanged.emit(this.addedTransformations);
  }

  /**
   * Creates a new transformation shell and adds it to the backend.
   */
  createTransformationShell() {
    console.log('Creating new transformation shell');
    this.transformationService.create(this.newTransformation).subscribe({
      next: (transformation: Transformation) => {
        this.transformations.push(transformation);
        this.newTransformation = {};
        this.displayCreateDialog = false;
      },
      error: (error: any) => {
        console.error('Fehler beim Erstellen der Transformation:', error);
      }
    });
  }

  /**
   * Opens the transformation creation dialog.
   */
  openCreateDialog() {
    this.displayCreateDialog = true;
  }

  /**
   * @property {any} selectedRule - Represents the currently selected rule for a transformation.
   */
  selectedRule: any;

  /**
   * @property {number | null} transformationId - ID of the currently selected transformation.
   */
  private _transformationId: number | null = null;

  /**
   * Getter for `transformationId`.
   * @returns {number | null} The ID of the selected transformation.
   */
  get transformationId(): number | null {
    return this._transformationId;
  }

  /**
   * Setter for `transformationId`.
   * @param {number | null} value - The new ID of the selected transformation.
   */
  set transformationId(value: number | null) {
    this._transformationId = value;
  }

  /**
   * Opens the rule selection dialog for the specified transformation.
   * @param {any} rowData - Data of the transformation to associate with a rule.
   */
  addRule(rowData: any) {
    this.selectedTransformation = rowData;
    this.displayRuleSelectionDialog = true;
    this.router.navigate([`/sync-jobs/create/rule/${rowData.id}`]);
    console.log('Selected transformation for rule:', rowData.id);
    this.transformationId = rowData.id;
  }

  /**
   * Handles the event when a rule is selected.
   * @param {any} event - Data of the selected rule.
   */
  onUseRuleEvent(event: any) {
    console.log('Selected rule:', event);
    this.selectedRule = event;
  }

  /**
   * Opens the script selection dialog for the specified transformation.
   * @param {any} rowData - Data of the transformation to associate with a script.
   */
  addScript(rowData: any) {
    this.selectedTransformation = rowData;
    this.displayScriptSelectionDialog = true;
    this.router.navigate([`/sync-jobs/create/script/${rowData.id}`]);
    this.transformationId = rowData.id;
  }

  /**
   * Displays a message indicating missing required fields.
   */
  showMissingFieldsMessage() {
    alert('Bitte füllen Sie alle erforderlichen Felder aus.');
  }

  /**
   * Cancels the rule selection dialog.
   */
  cancelRuleSelectionDialog() {
    this.displayRuleSelectionDialog = false;
    this.router.navigate([`/sync-jobs/create`]);
  }

  /**
   * Cancels the transformation creation dialog.
   */
  cancelCreateTransformationDialog() {
    this.displayCreateDialog = false;
  }

  /**
   * Cancels the script selection dialog.
   */
  cancelScriptSelectionDialog() {
    this.displayScriptSelectionDialog = false;
    this.router.navigate([`/sync-jobs/create`]);
  }

  /**
   * Removes the script associated with the specified transformation.
   * @param {any} rowData - Data of the transformation to update.
   */
  removeScript(rowData: any) {
    if (rowData.id !== null) {
      this.transformationService.getById(rowData.id).subscribe(transformation => {
        const updateRequest: UpdateTransformationRequest = {
          id: transformation.id,
          syncJobId: transformation.syncJobId ?? null,
          sourceSystemEndpointIds: [],
          targetSystemEndpointId: transformation.targetSystemEndpointId ?? null,
          transformationRuleId: transformation.transformationRuleId ?? null,
          transformationScriptId: null
        };
        console.log('UpdateTransformationRequest:', updateRequest);
        this.transformationService.update(updateRequest).subscribe(updated => {
          console.log('Update Response:', updated);
          this.loadTransformationsFromBackend();
        });
      });
    }
  }
}
