import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import {SidebarMenuComponent} from './sidebar-menu/sidebar-menu.component';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, SidebarMenuComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {
  title = 'configurator-ui';
  showForm = false;
}
