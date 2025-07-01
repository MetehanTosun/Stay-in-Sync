import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { EdcInstancesComponent } from '../edc-instances/edc-instances.component';
import { EdcAssetsAndPoliciesComponent } from '../edc-assets-and-policies/edc-assets-and-policies.component';

@Component({
  selector: 'app-edc-base',
  standalone: true,
  imports: [CommonModule, EdcInstancesComponent, EdcAssetsAndPoliciesComponent],
  templateUrl: './edc-base.component.html',
  styleUrls: ['./edc-base.component.css']
})
export class EdcBaseComponent {
  activeTab: string = 'instances';
}
