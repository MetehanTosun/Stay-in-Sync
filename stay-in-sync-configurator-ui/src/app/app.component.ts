import {Component} from '@angular/core';
import { RouterOutlet } from '@angular/router';
import {SidebarMenuComponent} from './features/sidebar-menu/sidebar-menu.component';
import {ToastModule} from 'primeng/toast';
import {NgIf} from '@angular/common';
import {Button} from 'primeng/button';
import {ToggleSwitch} from 'primeng/toggleswitch';
import {FormsModule} from '@angular/forms';


@Component({
  selector: 'app-root',
  imports: [RouterOutlet, SidebarMenuComponent, ToastModule, NgIf, Button, ToggleSwitch, FormsModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css',
})

export class AppComponent {

  sidebarVisible = true;
  darkModeEnabled: boolean = false;

  onSidebarToggle(visible: boolean): void {
    this.sidebarVisible = visible;
  }

  toggleDarkMode() {
    const element = document.querySelector('html');
    if (element) {
      element.classList.toggle('my-app-dark');
    }
  }
}
