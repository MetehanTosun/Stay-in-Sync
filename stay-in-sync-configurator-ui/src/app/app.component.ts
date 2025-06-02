import {Component} from '@angular/core';
import { RouterOutlet } from '@angular/router';
import {SidebarMenuComponent} from './features/sidebar-menu/sidebar-menu.component';
import {ToastModule} from 'primeng/toast';
import {NgIf} from '@angular/common';


@Component({
  selector: 'app-root',
  imports: [RouterOutlet, SidebarMenuComponent, ToastModule, NgIf],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})

export class AppComponent {

  sidebarVisible = true;

  onSidebarToggle(visible: boolean): void {
    this.sidebarVisible = visible;
  }
}
