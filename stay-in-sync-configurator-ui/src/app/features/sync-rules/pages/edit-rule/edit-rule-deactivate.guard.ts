import { Injectable } from '@angular/core';
import { CanDeactivate } from '@angular/router';
import { EditRuleComponent } from './edit-rule.component';

/**
 * This class represents the guard that requests user confirmation
 * if one tries to exit the graph editor with unsaved changes
 */
@Injectable({ providedIn: 'root' })
export class EditRuleDeactivateGuard implements CanDeactivate<EditRuleComponent> {
  canDeactivate(component: EditRuleComponent): boolean {
    if (component.canvas && component.canvas.hasUnsavedChanges) {
      return confirm('You have unsaved changes. Are you sure you want to leave this page?');
    }
    return true;
  }
}
