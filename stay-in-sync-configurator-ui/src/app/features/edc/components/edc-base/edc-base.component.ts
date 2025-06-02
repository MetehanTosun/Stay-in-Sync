import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { EdcInstancesComponent } from '../edc-instances/edc-instances.component';
import { EdcPartnersComponent } from '../edc-partners/edc-partners.component';
import { EdcAssetsComponent } from '../edc-assets/edc-assets.component';

@Component({
  selector: 'app-edc-base',
  standalone: true,
  imports: [CommonModule, EdcInstancesComponent, EdcPartnersComponent, EdcAssetsComponent],
  templateUrl: './edc-base.component.html',
  styleUrls: ['./edc-base.component.css']
})
export class EdcBaseComponent {
  activeTab: string = 'instances';
}
