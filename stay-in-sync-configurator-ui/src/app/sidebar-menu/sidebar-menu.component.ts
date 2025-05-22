import { Component } from '@angular/core';
import {Sidebar} from 'primeng/sidebar';
import {Button} from 'primeng/button';

@Component({
  selector: 'app-sidebar-menu',
  imports: [Sidebar, Button],
  templateUrl: './sidebar-menu.component.html',
  styleUrl: './sidebar-menu.component.css'
})
export class SidebarMenuComponent {

  sidebarVisible: boolean = true;

}
