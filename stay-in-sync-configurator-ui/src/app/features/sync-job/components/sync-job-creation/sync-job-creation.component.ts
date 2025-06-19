import {Component, EventEmitter, Input, NgIterable, Output} from '@angular/core';
import {Dialog} from 'primeng/dialog';
import {Button} from 'primeng/button';
import {Step, StepList, StepPanel, StepPanels, Stepper} from 'primeng/stepper';
import {Router} from '@angular/router';
import {FormsModule} from '@angular/forms';
import {InputText} from 'primeng/inputtext';
import {NgForOf} from '@angular/common';
import {SourceSystem} from '../../../source-system/models/source-system.model';
import {InputSwitch} from 'primeng/inputswitch';

@Component({
  selector: 'app-sync-job-creation',
  imports: [
    Dialog,
    Button,
    StepPanel,
    Step,
    Stepper,
    StepList,
    StepPanels,
    FormsModule,
    InputText,
    NgForOf,
    InputSwitch
  ],
  templateUrl: './sync-job-creation.component.html',
  styleUrl: './sync-job-creation.component.css'
})
export class SyncJobCreationComponent {
  @Input() visible = false;
  @Output() visibleChange = new EventEmitter<boolean>();
  syncJobName: String = '';
  syncJobDescription: String= '';
  selectedSourceSystem: any;
  sourceSystems: (NgIterable<SourceSystem>) | undefined | null;
  isSimulation: boolean = false;


  constructor(private router: Router) {
  }

  cancel() {
    this.visible = false;
    this.visibleChange.emit(false);
    this.router.navigate(['sync-jobs']);
  }
}
