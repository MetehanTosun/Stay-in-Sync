import { Component } from '@angular/core';
import {Sidebar} from 'primeng/sidebar';
import {Button} from 'primeng/button';
import {RouterLink, RouterLinkActive} from '@angular/router';
import {MessageService} from 'primeng/api';

@Component({
  selector: 'app-sidebar-menu',
  imports: [Sidebar, Button, RouterLink, RouterLinkActive],
  templateUrl: './sidebar-menu.component.html',
  standalone: true,
  styleUrl: './sidebar-menu.component.css'
})
export class SidebarMenuComponent {

  sidebarVisible: boolean = true;

  constructor(readonly messageService: MessageService) {

  }


  test() {
    this.messageService.add({ severity: 'success', summary: 'Success', detail: `Danke für dein Feedback! Du wirst gleich weitergeleitet.`, life: 2000});


  }

}
