import { Component, Input, OnInit } from '@angular/core';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ApiHeaderValueResourceService, ApiHeaderValueDTO } from '../../../../generated';

@Component({
  standalone: true,
  selector: 'app-manage-header-values',
  imports: [
    TableModule,
    ButtonModule,
    InputTextModule,
    ReactiveFormsModule
  ],
  templateUrl: './manage-header-values.component.html',
  styleUrls: ['./manage-header-values.component.css']
})
export class ManageHeaderValuesComponent implements OnInit {
  @Input() requestConfigId!: number;
  @Input() headerName!: string;

  values: ApiHeaderValueDTO[] = [];
  form: FormGroup;

  constructor(
    private service: ApiHeaderValueResourceService,
    private fb: FormBuilder
  ) {
    this.form = this.fb.group({
      headerValue: ['', Validators.required]
    });
  }

  ngOnInit() {
    this.loadValues();
  }

  loadValues() {
    this.service
      .apiConfigRequestConfigurationRequestConfigIdRequestHeaderGet(
        this.requestConfigId,
        'body'
      )
      .subscribe(list => this.values = list as ApiHeaderValueDTO[]);
  }

  addValue() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const dto: ApiHeaderValueDTO = {
      headerName: this.headerName,
      headerValue: this.form.value.headerValue
    };
    this.service
      .apiConfigRequestConfigurationRequestConfigIdRequestHeaderPost(
        this.requestConfigId,
        dto,
        'body'
      )
      .subscribe(() => {
        this.form.reset();
        this.loadValues();
      }, console.error);
  }

  deleteValue(value: ApiHeaderValueDTO) {
    const id = (value as any).id as number;
    this.service
      .apiConfigRequestConfigurationRequestHeaderIdDelete(
        id,
        'body'
      )
      .subscribe(() => {
        this.values = this.values.filter(v => (v as any).id !== id);
      }, console.error);
  }
}
