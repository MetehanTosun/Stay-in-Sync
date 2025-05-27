import { Component } from '@angular/core';
import {Panel} from 'primeng/panel';
import {Sidebar} from 'primeng/sidebar';
import {Button} from 'primeng/button';

@Component({
  selector: 'app-legend-panel',
  imports: [
    Sidebar,
    Button
  ],
  templateUrl: './legend-panel.component.html',
  styleUrl: './legend-panel.component.css'
})
export class LegendPanelComponent {
  visibleSidebar: boolean = false;

}
