import { Component } from '@angular/core';
import { CustomDynamicNodeComponent, Vflow } from 'ngx-vflow';

@Component({
  template: `
  <div>
    Constant
    <handle type="source" position="right" />
  </div>
  `,
  selector: 'app-constant-node',
  imports: [Vflow],
  styleUrl: '../default.css'
})
export class ConstantNodeComponent extends CustomDynamicNodeComponent{ }
