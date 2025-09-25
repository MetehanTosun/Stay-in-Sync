import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { GrafanaComponent } from './grafana.component';
import { HttpClientModule } from '@angular/common/http';

@NgModule({
  declarations: [GrafanaComponent],
  imports: [CommonModule, HttpClientModule],
  exports: [GrafanaComponent]
})
export class GrafanaModule { }
