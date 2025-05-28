import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import {SidebarMenuComponent} from './features/sidebar-menu/sidebar-menu.component';
import {MessageModule} from 'primeng/message';
import {ToastModule} from 'primeng/toast';
import {SyncJobPageComponent} from './features/sync-job/components/sync-job-page/sync-job-page.component';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, SidebarMenuComponent, ToastModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {
  title = 'configurator-ui';
  showForm = false;
}
