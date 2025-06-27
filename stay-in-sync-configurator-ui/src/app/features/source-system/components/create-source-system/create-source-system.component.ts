import { Component, OnInit, Input, Output, EventEmitter } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { SourceSystemApiService } from '../../../../services/source-system-api.service';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';

// PrimeNG-Module
import { DialogModule } from 'primeng/dialog';
import { StepsModule } from 'primeng/steps';
import { DropdownModule } from 'primeng/dropdown';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { InputTextarea } from 'primeng/inputtextarea';
import { InputGroupModule } from 'primeng/inputgroup';

interface Step {
  label: string;
}

@Component({
  selector: 'app-create-source-system',
  templateUrl: './create-source-system.component.html',
  styleUrls: ['./create-source-system.component.css'],
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    DialogModule,
    StepsModule,
    DropdownModule,
    ButtonModule,
    InputTextModule,
    InputTextModule,
    InputGroupModule,
  ]
})
export class CreateSourceSystemComponent implements OnInit {
  /** Controls dialog visibility */
  @Input() visible = false;
  @Output() visibleChange = new EventEmitter<boolean>();

  currentStep = 1;
  selectedFile: File | null = null;
  fileSelected = false;

  steps: Step[] = [
    { label: 'Metadaten' },
    { label: 'Specification' },
  ];

  typeOptions = [
    { label: 'AAS', value: 'AAS' },
    { label: 'REST-OpenAPI', value: 'REST_OPENAPI' },
  ];

  form!: FormGroup;

  constructor(
    private fb: FormBuilder,
    private api: SourceSystemApiService
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      name: ['', Validators.required],
      description: [''],
      type: ['', Validators.required],
      apiUrl: [''],
      submodelLink: [''],
      openApiSpec: [''],
    });
  }

  open(): void {
    this.visible = true;
    this.visibleChange.emit(true);
  }

  cancel(): void {
    this.visible = false;
    this.visibleChange.emit(false);
    this.currentStep = 1;
    this.form.reset();
    this.selectedFile = null;
    this.fileSelected = false;
  }

  prev(): void {
    if (this.currentStep > 1) {
      this.currentStep--;
    }
  }

  next(): void {
    if (this.currentStep === 1) {
      this.currentStep = 2;
      return;
    }

    const dto: any = {
      name: this.form.value.name,
      description: this.form.value.description,
      type: this.form.value.type,
      apiUrl: this.form.value.apiUrl,
      submodelLink: this.form.value.submodelLink,
    };

    if (this.form.value.type === 'REST_OPENAPI') {
      if (this.fileSelected && this.selectedFile) {
        this.api.uploadSpecFile(dto, this.selectedFile)
          .subscribe(() => this.finish());
      } else {
        dto.openApiSpecUrl = this.form.value.openApiSpec;
        this.api.create(dto)
          .subscribe(() => this.finish());
      }
    } else {
      this.api.create(dto)
        .subscribe(() => this.finish());
    }
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length) {
      this.selectedFile = input.files[0];
      this.fileSelected = true;
      this.form.patchValue({ openApiSpec: '' });
    }
  }

  private finish(): void {
    this.cancel();
  }
}