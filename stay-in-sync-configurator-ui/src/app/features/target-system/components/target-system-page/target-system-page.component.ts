import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { TabViewModule } from 'primeng/tabview';
import { InplaceModule } from 'primeng/inplace';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { ButtonModule } from 'primeng/button';
import { ToastModule } from 'primeng/toast';
import { MessageService } from 'primeng/api';

import { TargetSystemDTO } from '../../models/targetSystemDTO';
import { TargetSystemResourceService } from '../../service/targetSystemResource.service';
import { HttpErrorService } from '../../../../core/services/http-error.service';
import { ManageApiHeadersComponent } from '../../../source-system/components/manage-api-headers/manage-api-headers.component';
import { ManageTargetEndpointsComponent } from '../manage-target-endpoints/manage-target-endpoints.component';
import { AasManagementComponent } from '../aas-management/aas-management.component';

@Component({
  selector: 'app-target-system-page',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    TabViewModule,
    InplaceModule,
    InputTextModule,
    TextareaModule,
    ButtonModule,
    ToastModule,
    ManageApiHeadersComponent,
    ManageTargetEndpointsComponent,
    AasManagementComponent
  ],
  templateUrl: './target-system-page.component.html',
  styleUrl: './target-system-page.component.css',
  providers: [MessageService]
})
export class TargetSystemPageComponent implements OnInit {
  selectedSystem?: TargetSystemDTO;
  private originalSystem: TargetSystemDTO | null = null;

  // AAS Test state
  aasTestLoading = false;
  aasTestError: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private api: TargetSystemResourceService,
    private errorService: HttpErrorService,
    private messageService: MessageService
  ) {}

  ngOnInit(): void {
    const id = Number.parseInt(this.route.snapshot.paramMap.get('id')!);
    this.api.getById(id).subscribe({
      next: data => { this.selectedSystem = data; },
      error: err => { this.errorService.handleError(err); }
    });
  }

  isAasSelected(): boolean {
    return (this.selectedSystem?.apiType || '').toUpperCase().includes('AAS');
  }

  onInplaceActivate(): void {
    if (this.selectedSystem) {
      this.originalSystem = { ...this.selectedSystem };
    }
  }

  onSave(closeCallback: () => void): void {
    if (!this.selectedSystem?.id) return;
    this.api.update(this.selectedSystem.id, this.selectedSystem).subscribe({
      next: () => {
        closeCallback();
        this.messageService.add({
          key: 'targetPage',
          severity: 'success',
          summary: 'Updated',
          detail: 'Target System updated successfully.',
          life: 3000
        });
      },
      error: (err) => {
        if (this.originalSystem) {
          Object.assign(this.selectedSystem!, this.originalSystem);
        }
        this.errorService.handleError(err);
        closeCallback();
      }
    });
  }

  onClose(closeCallback: () => void): void {
    if (this.selectedSystem && this.originalSystem) {
      Object.assign(this.selectedSystem, this.originalSystem);
    }
    closeCallback();
  }

  aasTest(): void {
    if (!this.selectedSystem?.id) return;
    this.aasTestLoading = true;
    this.aasTestError = null;
    
    this.api.update(this.selectedSystem.id, this.selectedSystem).subscribe({
      next: () => {
        this.aasTestLoading = false;
        this.messageService.add({
          key: 'targetPage',
          severity: 'success',
          summary: 'AAS ID saved',
          detail: 'AAS ID has been updated.',
          life: 3000
        });
      },
      error: (err) => {
        this.aasTestLoading = false;
        this.aasTestError = err?.message || 'Test failed';
        this.errorService.handleError(err);
      }
    });
  }

  onAasRefreshRequested(): void {
    // Reload target system if needed
  }
}

