import { Component } from '@angular/core';
import {Sidebar} from 'primeng/sidebar';
import {Avatar} from 'primeng/avatar';
import {Button} from 'primeng/button';
import {StyleClass} from 'primeng/styleclass';
import {PrimeTemplate} from 'primeng/api';
import {Ripple} from 'primeng/ripple';

@Component({
  selector: 'app-sidebar-menu',
  imports: [Sidebar, Avatar, Button, StyleClass, PrimeTemplate, Ripple],
  templateUrl: './sidebar-menu.component.html',
  styleUrl: './sidebar-menu.component.css'
})
export class SidebarMenuComponent {

  sidebarVisible: boolean = true;

  closeCallback($event: MouseEvent) {

  }
}
