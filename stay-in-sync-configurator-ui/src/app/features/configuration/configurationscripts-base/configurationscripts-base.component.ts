import {Component} from '@angular/core';
import {ButtonModule} from 'primeng/button';
import {ScriptEditorNavigationService} from '../../script-editor/script-editor-navigation.service';
import {TransformationService} from '../../transformation/services/transformation.service';
import {CommonModule} from '@angular/common';
import {Transformation} from '../../transformation/models/transformation.model';

@Component({
  selector: 'app-configurationscripts-base',
  imports: [ButtonModule, CommonModule],
  templateUrl: './configurationscripts-base.component.html',
  styleUrl: './configurationscripts-base.component.css',
  standalone: true
})
export class ConfigurationscriptsBaseComponent {

  constructor(protected navService: ScriptEditorNavigationService, readonly transformationService: TransformationService) {
  }


  onCreateScript() {
    this.transformationService.create({name: "test"}).subscribe({
      next: (transformation: Transformation) => {
        this.navService.navigateToScriptEditor({
          transformationId: transformation.id!,
          scriptName: "New Script!"
        })
      },
      error: (error: any) => {
        console.error('Fehler beim Erstellen der Transformation:', error);
      }
    });
  }

}
