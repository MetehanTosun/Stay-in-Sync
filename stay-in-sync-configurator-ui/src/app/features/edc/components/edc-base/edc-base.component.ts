import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { EdcInstancesComponent } from '../edc-instances/edc-instances.component';
import { EdcAssetsAndPoliciesComponent } from '../edc-assets-and-policies/edc-assets-and-policies.component';

type EdcTab = 'instances' | 'assets and policies';

@Component({
  selector: 'app-edc-base',
  standalone: true,
  imports: [CommonModule, EdcInstancesComponent, EdcAssetsAndPoliciesComponent],
  templateUrl: './edc-base.component.html',
  styleUrls: ['./edc-base.component.css'],
})
export class EdcBaseComponent {

  activeTab: EdcTab = 'instances';

  setActiveTab(tab: EdcTab): void {
    this.activeTab = tab;
  }
}
