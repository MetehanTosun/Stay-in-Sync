import {Component, Input} from '@angular/core';
import { RouterOutlet } from '@angular/router';
import {SidebarMenuComponent} from './features/sidebar-menu/sidebar-menu.component';
import {MessageModule} from 'primeng/message';
import {ToastModule} from 'primeng/toast';
import {SyncJobPageComponent} from './features/sync-job/components/sync-job-page/sync-job-page.component';
import {NgIf} from '@angular/common';
import {Button} from 'primeng/button';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, SidebarMenuComponent, ToastModule, NgIf, Button],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})

export class AppComponent {
  /** Steuert, ob die Sidebar angezeigt wird */
  sidebarVisible = true;

  /**
   * Optional: Methode zur Reaktion auf Änderungen.
   * Diese könnte z. B. Logging oder weitere UI-Updates auslösen.
   */
  onSidebarToggle(visible: boolean): void {
    this.sidebarVisible = visible;
    console.log('Sidebar Sichtbarkeit geändert:', visible);
  }
}
