import { Component, OnInit, Input, Output, EventEmitter } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';

// PrimeNG
import { DialogModule } from 'primeng/dialog';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';
import { TextareaModule } from 'primeng/textarea';

// Services und DTOs
import { SourceSystemResourceService } from '../../../../generated/api/sourceSystemResource.service';
import { CreateSourceSystemDTO } from '../../../../generated/model/createSourceSystemDTO';
import { ApiAuthType } from '../../../../generated/model/apiAuthType';
import { BasicAuthDTO } from '../../../../generated/model/basicAuthDTO';
import { ApiKeyAuthDTO } from '../../../../generated/model/apiKeyAuthDTO';

@Component({
  standalone: true,
  selector: 'app-create-source-system',
  templateUrl: './create-source-system.component.html',
  styleUrls: ['./create-source-system.component.css'],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    DialogModule,
    DropdownModule,
    InputTextModule,
    ButtonModule,
    TextareaModule
  ]
})
export class CreateSourceSystemComponent implements OnInit {
  @Input() visible = false; // Steuert die Sichtbarkeit des Dialogs
  @Output() visibleChange = new EventEmitter<boolean>(); // Meldet Änderungen der Sichtbarkeit zurück
  @Output() nextStep = new EventEmitter<number>(); // Gibt die sourceSystemId zurück, um zum nächsten Schritt zu wechseln

  form!: FormGroup;
  selectedFile: File | null = null;
  fileSelected = false;

  typeOptions = [
    { label: 'REST-OpenAPI', value: 'REST_OPENAPI' },
    { label: 'AAS', value: 'AAS' }
  ];

  authTypeOptions = [
    { label: 'None', value: 'NONE' },
    { label: 'Basic', value: ApiAuthType.Basic },
    { label: 'API Key', value: ApiAuthType.ApiKey }
  ];

  constructor(private fb: FormBuilder, private sourceSystemService: SourceSystemResourceService) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      name: ['', Validators.required],
      apiUrl: ['', [Validators.required, Validators.pattern('https?://.+')]],
      description: [''],
      apiType: ['REST_OPENAPI', Validators.required],
      apiAuthType: ['NONE'],
      authConfig: this.fb.group({
        username: [''],
        password: [''],
        apiKey: [''],
        headerName: ['']
      }),
      openApiSpec: [null]
    });

    this.setupAuthTypeValidators();
  }

  private setupAuthTypeValidators(): void {
    this.form.get('apiAuthType')!.valueChanges.subscribe((authType) => {
      const authConfigGroup = this.form.get('authConfig') as FormGroup;
      authConfigGroup.get('username')!.clearValidators();
      authConfigGroup.get('password')!.clearValidators();
      authConfigGroup.get('apiKey')!.clearValidators();
      authConfigGroup.get('headerName')!.clearValidators();

      if (authType === ApiAuthType.Basic) {
        authConfigGroup.get('username')!.setValidators(Validators.required);
        authConfigGroup.get('password')!.setValidators(Validators.required);
      } else if (authType === ApiAuthType.ApiKey) {
        authConfigGroup.get('apiKey')!.setValidators(Validators.required);
        authConfigGroup.get('headerName')!.setValidators(Validators.required);
      }

      authConfigGroup.get('username')!.updateValueAndValidity();
      authConfigGroup.get('password')!.updateValueAndValidity();
      authConfigGroup.get('apiKey')!.updateValueAndValidity();
      authConfigGroup.get('headerName')!.updateValueAndValidity();
    });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length) {
      this.selectedFile = input.files[0];
      this.fileSelected = true;
      this.form.get('openApiSpec')?.reset();
    }
  }

  cancel(): void {
    this.visible = false;
    this.visibleChange.emit(this.visible);
    this.form.reset({ apiType: 'REST_OPENAPI', apiAuthType: 'NONE' });
    this.selectedFile = null;
    this.fileSelected = false;
  }

  save(): void {
    if (this.form.valid) {
      const dto: CreateSourceSystemDTO = this.form.value;

      if (this.selectedFile) {
        // Datei in Base64 umwandeln
        const reader = new FileReader();
        reader.onload = () => {
          const base64File = reader.result as string;

          // Base64-String in Blob umwandeln
          const blobFile = new Blob([base64File], { type: 'application/octet-stream' });

          // JSON-Objekt mit Blob-Datei erstellen
          const jsonDto = {
            ...dto,
            openApiSpec: blobFile // Blob der Datei
          };

          // JSON-Upload
          this.sourceSystemService.apiConfigSourceSystemPost(jsonDto).subscribe({
            next: (sourceSystemId: number) => {
              console.log('Source System created with ID:', sourceSystemId);
              this.nextStep.emit(sourceSystemId);
            },
            error: (err) => {
              console.error('Failed to create Source System:', err);
            }
          });
        };

        reader.readAsDataURL(this.selectedFile); // Datei lesen und Base64-String erzeugen
      } else {
        // JSON-Upload ohne Datei
        this.sourceSystemService.apiConfigSourceSystemPost(dto).subscribe({
          next: (sourceSystemId: number) => {
            console.log('Source System created with ID:', sourceSystemId);
            this.nextStep.emit(sourceSystemId);
          },
          error: (err) => {
            console.error('Failed to create Source System:', err);
          }
        });
      }
    }
  }
}