import { WritableSignal } from "@angular/core";
import { CustomDynamicNodeComponent, DynamicNode } from "ngx-vflow";


export interface CustomNodeData {
  inputNodes?: any[];
  name: string;
  arcId?: number;
  jsonPath?: string;
  value?: any;
  operatorType?: string;
}


export interface CustomDynamicNode extends CustomDynamicNodeComponent {
  data: WritableSignal<CustomNodeData>;
}
