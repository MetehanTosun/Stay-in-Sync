import {Component} from '@angular/core';
import {TableModule} from 'primeng/table';
import {Transformation} from '../../models/transformation.model';
import {Button} from 'primeng/button';
import {FormsModule} from '@angular/forms';
import { ToggleButtonModule } from 'primeng/togglebutton';
import {min} from 'rxjs';

@Component({
  selector: 'app-transformation-base',
  imports: [
    TableModule,
    Button,
    FormsModule,
    ToggleButtonModule
  ],
  templateUrl: './transformation-base.component.html',
  styleUrl: './transformation-base.component.css'
})
export class TransformationBaseComponent {
  transformations: Transformation[] = [{
    name: 'Beispieltransformation',
    transformationRule: 'Rule1',
    transformationsScript: 'Script1'
  }];
  checked: boolean = false;

  edit(rowData: any) {
    // Implement edit functionality here
    console.log('Edit transformation:', rowData);
  }

  delete(rowData: any) {
    // Implement delete functionality here
    console.log('Delete transformation:', rowData);
  }

  add(rowData: any) {
    // Implement add functionality here
    console.log('Add transformation:', rowData);
    rowData.added = true;
  }

  remove(rowData: any) {
    rowData.added = false;
  }

  protected readonly min = min;
}
