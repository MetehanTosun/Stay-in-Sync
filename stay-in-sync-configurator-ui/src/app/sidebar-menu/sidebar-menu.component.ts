import { Component } from '@angular/core';
import {Sidebar} from 'primeng/sidebar';
import {Button} from 'primeng/button';
import {RouterLink, RouterLinkActive} from '@angular/router';

@Component({
  selector: 'app-sidebar-menu',
  imports: [Sidebar, Button, RouterLink, RouterLinkActive],
  templateUrl: './sidebar-menu.component.html',
  styleUrl: './sidebar-menu.component.css'
})
export class SidebarMenuComponent {

  sidebarVisible: boolean = true;


}
