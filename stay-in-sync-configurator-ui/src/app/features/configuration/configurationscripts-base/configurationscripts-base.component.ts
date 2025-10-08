import {Component, OnInit} from '@angular/core';
import {ButtonModule} from 'primeng/button';
import {TransformationService} from '../../transformation/services/transformation.service';
import {CommonModule} from '@angular/common';
import {Transformation} from '../../transformation/models/transformation.model';
import {Select} from "primeng/select";
import {TableModule} from "primeng/table";
import {Tag} from "primeng/tag";
import {FormsModule} from '@angular/forms';
import {Router} from '@angular/router';

@Component({
  selector: 'app-configurationscripts-base',
  imports: [ButtonModule, CommonModule, Select, TableModule, Tag, FormsModule],
  templateUrl: './configurationscripts-base.component.html',
  styleUrl: './configurationscripts-base.component.css',
  standalone: true
})
export class ConfigurationscriptsBaseComponent implements OnInit {
  ngOnInit(): void {
    this.loadTransformationScripts();
  }

  items: Transformation[] = [];
  selectedStatus: String = '';
  stati = ['DRAFT', 'VALIDATED'];

  constructor(protected router: Router, readonly transformationService: TransformationService) {
  }


  onCreateScript() {
    this.transformationService.create({name: "test"}).subscribe({
      next: (transformation: Transformation) => {
        this.router.navigate(['/script-editor/', transformation.id]);
      },
      error: (error: any) => {
        console.error('Fehler beim Erstellen der Transformation:', error);
      }
    });
  }


  private loadTransformationScripts(): void {
    this.transformationService.getAll().subscribe({
      next: data => {
        this.items = data;
      },
      error: err => {
        console.log(err);
      }
    });
  }

  getSeverity(value: string) {
    switch (value) {
      case 'DRAFT':
        return 'warning'
      case 'VALIDATED':
        return 'success'
      default:
        return 'warning'
    }
  }

  edit(transformation: Transformation) {
    this.router.navigate(['/script-editor/', transformation.id]);
  }

  delete(transformation: Transformation) {
    this.transformationService.delete(transformation).subscribe({
      next: data => {
        this.loadTransformationScripts()
      },
      error: err => {
        console.log(err);
      }
    });
  }
}
