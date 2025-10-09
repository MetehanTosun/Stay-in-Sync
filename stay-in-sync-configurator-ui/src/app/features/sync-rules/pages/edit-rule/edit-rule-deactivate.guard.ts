import { Injectable } from '@angular/core';
import { CanDeactivate } from '@angular/router';
import { EditRuleComponent } from './edit-rule.component';

/**
 * This class represents the guard that requests user confirmation
 * if the user tries to exit the graph editor with unsaved changes
 */
@Injectable({ providedIn: 'root' })
export class EditRuleDeactivateGuard implements CanDeactivate<EditRuleComponent> {
  /**
   * Synchronous canDeactivate check. Returns true to allow navigation.
   *
   * @param component The instance of the EditRuleComponent that's being deactivated
   * @returns boolean - true if navigation may proceed, false otherwise
   */
  canDeactivate(component: EditRuleComponent): boolean {
    if (component?.canvas?.hasUnsavedChanges) {
      return confirm('You have unsaved changes. Are you sure you want to leave this page?');
    }
    return true;
  }
}
