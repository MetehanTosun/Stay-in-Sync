import { Injectable, signal } from "@angular/core";
import { ExistsNodeComponent } from "./nodes/exists-node.component";
import { DynamicNode, Edge } from "ngx-vflow";
import { ConstantNodeComponent } from "./nodes/constant-node.component";
import { NotExistsNodeComponent } from "./nodes/not-exists-node.component";
import { OutputNodeComponent } from "./nodes/IO/output-node.component";

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
