import { Component } from '@angular/core';
import {Sidebar} from 'primeng/sidebar';
import {Button} from 'primeng/button';
import {RouterLink, RouterLinkActive, RouterOutlet} from '@angular/router';
import {NgForOf} from '@angular/common';

@Component({
  selector: 'app-sidebar-menu',
  imports: [Sidebar, Button, RouterOutlet, RouterLink, RouterLinkActive, NgForOf],
  templateUrl: './sidebar-menu.component.html',
  styleUrl: './sidebar-menu.component.css'
})
export class SidebarMenuComponent {

  sidebarVisible: boolean = true;


}
