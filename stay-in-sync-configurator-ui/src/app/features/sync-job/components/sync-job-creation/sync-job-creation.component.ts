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
import {AasService} from "../../../source-system/services/aas.service";
import {ToggleSwitch} from "primeng/toggleswitch";
import {FloatLabel} from "primeng/floatlabel";
import {Textarea} from "primeng/textarea";
import {
  TransformationBaseComponent
} from "../../../transformation/components/transformation-base/transformation-base.component";

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
    ToggleSwitch,
    FloatLabel,
    Textarea,
    TransformationBaseComponent
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


  constructor(private router: Router, private aas: AasService) {
  }

  cancel() {
    this.visible = false;
    this.visibleChange.emit(false);
    this.router.navigate(['sync-jobs']);
  }

  ngOnInit() {
    this.loadSystems();
  }

  private loadSystems() {
    this.aas.getAll().subscribe({
      next: list => {
        this.sourceSystems = list.map(s => ({ id: s.id, name: s.name}));
      },
      error: err => {
        console.error(err);
      }
    });
  }
}
