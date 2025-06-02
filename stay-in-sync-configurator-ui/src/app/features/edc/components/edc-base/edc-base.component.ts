import { Component } from '@angular/core';

@Component({
  selector: 'app-edc-base',
  templateUrl: './edc-base.component.html',
  styleUrls: ['./edc-base.component.css']
})
export class EdcBaseComponent {
  activeTab: string = 'tab1';
}
