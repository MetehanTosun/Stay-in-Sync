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
  @Input() visible = false;
  @Output() visibleChange = new EventEmitter<boolean>();
  @Output() nextStep = new EventEmitter<number>();

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

  constructor(
    private fb: FormBuilder,
    private sourceSystemService: SourceSystemResourceService
  ) {}

  ngOnInit(): void {
    // Schritt 1: FormGroup inkl. authConfig-Gruppe
    this.form = this.fb.group({
      name: ['', Validators.required],
      apiUrl: ['', [Validators.required, Validators.pattern('https?://.+')]],
      description: [''],
      apiType: ['REST_OPENAPI', Validators.required],
      apiAuthType: ['NONE', Validators.required],
      authConfig: this.fb.group({
        username: [''],
        password: [''],
        apiKey: [''],
        headerName: [''],
      }),
      openApiSpec: [null]
    });

    this.setupAuthTypeValidators();
  }

  private setupAuthTypeValidators(): void {
    const authConfigGroup = this.form.get('authConfig') as FormGroup;
    this.form.get('apiAuthType')!.valueChanges.subscribe((authType: ApiAuthType) => {
      // alle vorherigen Validatoren löschen
      ['username','password','apiKey','headerName'].forEach(key =>
        authConfigGroup.get(key)!.clearValidators()
      );

      // neue Validatoren setzen
      if (authType === ApiAuthType.Basic) {
        authConfigGroup.get('username')!.setValidators([Validators.required]);
        authConfigGroup.get('password')!.setValidators([Validators.required]);
      } else if (authType === ApiAuthType.ApiKey) {
        authConfigGroup.get('apiKey')!.setValidators([Validators.required]);
        authConfigGroup.get('headerName')!.setValidators([Validators.required]);
      }

      // Validierung neu berechnen
      ['username','password','apiKey','headerName'].forEach(key =>
        authConfigGroup.get(key)!.updateValueAndValidity()
      );
    });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.selectedFile = input.files[0];
      this.fileSelected = true;
      // falls URL-Feld da war, zurücksetzen
      this.form.get('openApiSpec')!.reset();
    }
  }

  cancel(): void {
    this.visible = false;
    this.visibleChange.emit(this.visible);
    // Form zurücksetzen auf Default-Werte
    this.form.reset({
      apiType: 'REST_OPENAPI',
      apiAuthType: 'NONE'
    });
    this.selectedFile = null;
    this.fileSelected = false;
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    // Basispayload aus dem FormGroup holen (hat aktuell noch das verschachtelte authConfig-Objekt)
    const dto: CreateSourceSystemDTO = {
      ...this.form.value,
      authConfig: undefined,   // setzen wir gleich manuell
      openApiSpec: undefined   // ebenfalls manuell je nach Datei
    };

    // authConfig je nach Typ
    const authConfigGroup = this.form.get('authConfig') as FormGroup;
    const authType = this.form.get('apiAuthType')!.value as ApiAuthType;
    if (authType === ApiAuthType.Basic) {
      dto.authConfig = {
        authType: ApiAuthType.Basic,
        username: authConfigGroup.get('username')!.value,
        password: authConfigGroup.get('password')!.value
      } as BasicAuthDTO;
    } else if (authType === ApiAuthType.ApiKey) {
      dto.authConfig = {
        authType: ApiAuthType.ApiKey,
        apiKey: authConfigGroup.get('apiKey')!.value,
        headerName: authConfigGroup.get('headerName')!.value
      } as ApiKeyAuthDTO;
    }

    // Datei-Upload oder reines JSON
    if (this.selectedFile) {
      const reader = new FileReader();
      reader.onload = () => {
        // reader.result enthält DataURL, splitten wir ab
        const base64 = (reader.result as string).split(',')[1];
        // hier nehmen wir einfach die Base64-String direkt
        const byteCharacters = atob(base64);
        const byteNumbers = Array.from(byteCharacters).map(char => char.charCodeAt(0));
        const byteArray = new Uint8Array(byteNumbers);
        dto.openApiSpec = new Blob([byteArray], { type: 'application/octet-stream' });
        this.postDto(dto);
      };
      reader.readAsDataURL(this.selectedFile);
    } else {
      this.postDto(dto);
    }
  }

  private postDto(dto: CreateSourceSystemDTO) {
    this.sourceSystemService
      .apiConfigSourceSystemPost(dto, 'body', false)
      .subscribe({
        next: (res) => {
          // wenn der Service die neue ID zurückliefert, emitten wir sie
          const newId = typeof res === 'number' ? res : undefined;
          if (newId) {
            this.nextStep.emit(newId);
          }
        },
        error: (err) => {
          console.error('Failed to create Source System:', err);
        }
      });
  }
}