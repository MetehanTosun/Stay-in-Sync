import {Component, EventEmitter, Output} from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import {NgIf} from '@angular/common';
import {MyPreset} from '../../mypreset';

@Component({
  selector: 'app-sidebar-menu',
  standalone: true,
  templateUrl: './sidebar-menu.component.html',
  styleUrl: './sidebar-menu.component.css',
  imports: [RouterLink, RouterLinkActive],
})
export class SidebarMenuComponent {


}
