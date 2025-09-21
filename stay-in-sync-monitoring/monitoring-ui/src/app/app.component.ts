import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import {ToggleSwitch} from 'primeng/toggleswitch';
import {FormsModule} from '@angular/forms';

@Component({
  selector: 'app-root',
  imports: [ToggleSwitch, FormsModule, RouterOutlet],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {
  title = 'monitoring-ui';
  darkModeEnabled: boolean = false;

  toggleDarkMode() {
    const element = document.querySelector('html');
    if (element) {
      element.classList.toggle('my-app-dark');
    }
  }
}
