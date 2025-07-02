import { Injectable, signal } from "@angular/core";
import { DynamicNode, Edge } from "ngx-vflow";
import { ConstantNodeComponent } from "./nodes/IO/constant-node.component";
import { OutputNodeComponent } from "./nodes/IO/output-node.component";
import { ExistsNodeComponent } from "./nodes/general/exists-node.component";
import { NotExistsNodeComponent } from "./nodes/general/not-exists-node.component";

@Injectable()
export class FlowStoreService {
  nodes = signal([
    {
      id: '1',
      point: signal({ x:200, y:100 }),
      type: ExistsNodeComponent
    },
    {
      id: '2',
      point: signal({ x: 0, y:10 }),
      type: ConstantNodeComponent
    },
    {
      id: '3',
      point: signal({ x:400, y:10 }),
      type: NotExistsNodeComponent
    },
    {
      id: '4',
      point: signal({ x:600, y:100 }),
      type: OutputNodeComponent
    }
  ] as DynamicNode[]);

  edges = signal ([
    {
      id: '2 -> 1',
      source: '2',
      target: '1',
    }
  ] as Edge[]);
}
