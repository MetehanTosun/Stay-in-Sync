import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { EdcInstancesComponent } from '../edc-instances/edc-instances.component';
import { EdcAssetsAndPoliciesComponent } from '../edc-assets-and-policies/edc-assets-and-policies.component';
import { EdcInstance } from '../edc-instances/models/edc-instance.model';
import { TemplatesComponent } from '../templates/templates.component';

@Component({
  selector: 'app-edc-base',
  standalone: true,
  imports: [
    CommonModule,
    EdcInstancesComponent,
    EdcAssetsAndPoliciesComponent,
    TemplatesComponent,
  ],
  templateUrl: './edc-base.component.html',
  styleUrls: ['./edc-base.component.css'],
})
export class EdcBaseComponent {
  selectedInstance: EdcInstance | null = null;

  onInstanceSelected(instance: EdcInstance): void {
    this.selectedInstance = instance;
  }

  onBack(): void {
    this.selectedInstance = null;
  }
}
