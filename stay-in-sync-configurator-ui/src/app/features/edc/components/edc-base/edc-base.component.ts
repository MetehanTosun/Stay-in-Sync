import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { EdcInstancesComponent } from '../edc-instances/edc-instances.component';
import { EdcAssetsAndPoliciesComponent } from '../edc-assets-and-policies/edc-assets-and-policies.component';

import {
  PolicyTemplatesComponent
} from '../policy-templates/policy-templates.component';

type EdcTab = 'instances' | 'assets and policies' | 'asset templates';

@Component({
  selector: 'app-edc-base',
  standalone: true,

  imports: [
    CommonModule,
    EdcInstancesComponent,
    EdcAssetsAndPoliciesComponent,
    PolicyTemplatesComponent,
  ],
  templateUrl: './edc-base.component.html',
  styleUrls: ['./edc-base.component.css'],
})
export class EdcBaseComponent {

  activeTab: EdcTab = 'asset templates';

  setActiveTab(tab: EdcTab): void {
    this.activeTab = tab;
  }
}
