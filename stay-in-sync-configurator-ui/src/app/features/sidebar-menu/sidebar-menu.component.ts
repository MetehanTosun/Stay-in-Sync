import {Component, EventEmitter, Output} from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import {NgIf} from '@angular/common';

@Component({
  selector: 'app-sidebar-menu',
  standalone: true,
  templateUrl: './sidebar-menu.component.html',
  styleUrl: './sidebar-menu.component.css',
  imports: [RouterLink, RouterLinkActive, NgIf],
})
export class SidebarMenuComponent {

  /** interner State des PrimeNG-Sidebars */
  private _sidebarVisible = true;

  /**
   * Wird jedes Mal ausgelöst, wenn sich der Sichtbarkeits-
   * status des Sidebars ändert.  Eine übergeordnete oder
   * beliebige andere Komponente kann dieses Event binden, z. B.:
   *
   * <app-config-sidebar
   *   (sidebarVisibleChange)="onSidebarToggle($event)">
   * </app-config-sidebar>
   */
  @Output() sidebarVisibleChange = new EventEmitter<boolean>();

  /** Property, an das PrimeNG mit [(visible)] bindet. */
  get sidebarVisible(): boolean {
    return this._sidebarVisible;
  }
  set sidebarVisible(value: boolean) {
    if (this._sidebarVisible !== value) {
      this._sidebarVisible = value;
      /* Wert an alle Listeners weiterreichen */
      this.sidebarVisibleChange.emit(value);
    }
  }

  /** Wird in der Vorlage von “Sync-rules”, “Scripts” … aufgerufen. */
  keepSidebarOpen(): void {
    this.sidebarVisible = true;

  }


}
