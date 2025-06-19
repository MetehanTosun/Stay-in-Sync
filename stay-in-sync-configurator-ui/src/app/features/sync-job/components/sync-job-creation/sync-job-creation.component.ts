import {Component, EventEmitter, Input, Output} from '@angular/core';
import {Dialog} from 'primeng/dialog';
import {Button} from 'primeng/button';
import {Step, StepList, StepPanel, StepPanels, Stepper} from 'primeng/stepper';
import {Router} from '@angular/router';

@Component({
  selector: 'app-sync-job-creation',
  imports: [
    Dialog,
    Button,
    StepPanel,
    Step,
    Stepper,
    StepList,
    StepPanels
  ],
  templateUrl: './sync-job-creation.component.html',
  styleUrl: './sync-job-creation.component.css'
})
export class SyncJobCreationComponent {
  @Input() visible = false;
  @Output() visibleChange = new EventEmitter<boolean>();

  constructor(private router: Router) {
  }

  cancel() {
    this.visible = false;
    this.visibleChange.emit(false);
    this.router.navigate(['sync-jobs']);
  }
}
