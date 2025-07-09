import { Component, inject, OnInit, signal, TemplateRef, ViewChild, viewChild } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { Connection, DynamicNode, Edge, Node, Vflow, VflowComponent } from 'ngx-vflow';
import { DndModule } from 'ngx-drag-drop';
import { delay, of } from 'rxjs';
import { FlowStoreService } from '../../components/customNodes/flow-store.service';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ProviderNodeComponent } from '../../components/nodeTypes/provider-node.component';
import { ConstantNodeComponent } from '../../components/nodeTypes/constant-node.component';
import { LogicNodeComponent } from '../../components/nodeTypes/logic-node.component';
import { CustomDynamicNode, CustomNodeData } from '../../components/nodeTypes/custom-node';

@Component({
  selector: 'app-edit-rule',
  imports: [Vflow, DndModule, MatProgressSpinnerModule, CommonModule],
  templateUrl: './edit-rule.component.html',
  styleUrl: './edit-rule.component.scss',
  providers: [FlowStoreService]
})
export class EditRuleComponent implements OnInit {

  //#region old implementation sTODO
  /* protected store = inject(FlowStoreService);
  protected vflow = viewChild.required(VflowComponent);

  // Map of node types with lazy loading
  private readonly nodeTypeMap = new Map<string, () => Promise<any>>([
    // General predicates
    ['exists', () => import('../../components/customNodes/nodes/general/exists-node.component').then(m => m.ExistsNodeComponent)],
    ['notExists', () => import('../../components/customNodes/nodes/general/not-exists-node.component').then(m => m.NotExistsNodeComponent)],
    ['isNull', () => import('../../components/customNodes/nodes/general/is-null-node.component').then(m => m.IsNullNodeComponent)],
    ['isNotNull', () => import('../../components/customNodes/nodes/general/is-not-null-node.component').then(m => m.IsNotNullNodeComponent)],
    ['typeIs', () => import('../../components/customNodes/nodes/general/type-is-node.component').then(m => m.TypeIsNodeComponent)],
    ['equals', () => import('../../components/customNodes/nodes/general/equals-node.component').then(m => m.EqualsNodeComponent)],
    ['notEquals', () => import('../../components/customNodes/nodes/general/not-equals-node.component').then(m => m.NotEqualsNodeComponent)],
    ['inSet', () => import('../../components/customNodes/nodes/general/in-set-node.component').then(m => m.InSetNodeComponent)],
    ['notInSet', () => import('../../components/customNodes/nodes/general/not-in-set-node.component').then(m => m.NotInSetNodeComponent)],
    ['oneOf', () => import('../../components/customNodes/nodes/general/one-of-node.component').then(m => m.OneOfNodeComponent)],
    ['allOf', () => import('../../components/customNodes/nodes/general/all-of-node.component').then(m => m.AllOfNodeComponent)],
    ['noneOf', () => import('../../components/customNodes/nodes/general/none-of-node.component').then(m => m.NoneOfNodeComponent)],
    ['matchesSchema', () => import('../../components/customNodes/nodes/general/matches-schema-node.component').then(m => m.MatchesSchemaNodeComponent)],

    // Logic operators (general)
    ['and', () => import('../../components/customNodes/nodes/general/and-node.component').then(m => m.AndNodeComponent)],
    ['or', () => import('../../components/customNodes/nodes/general/or-node.component').then(m => m.OrNodeComponent)],
    ['not', () => import('../../components/customNodes/nodes/general/not-node.component').then(m => m.NotNodeComponent)],
    ['xor', () => import('../../components/customNodes/nodes/general/xor-node.component').then(m => m.XorNodeComponent)],

    // Number predicates
    ['greaterThan', () => import('../../components/customNodes/nodes/number/greater-than-node.component').then(m => m.GreaterThanNodeComponent)],
    ['lessThan', () => import('../../components/customNodes/nodes/number/less-than-node.component').then(m => m.LessThanNodeComponent)],
    ['greaterOrEqual', () => import('../../components/customNodes/nodes/number/greater-or-equal-node.component').then(m => m.GreaterOrEqualNodeComponent)],
    ['lessOrEqual', () => import('../../components/customNodes/nodes/number/less-or-equal-node.component').then(m => m.LessOrEqualNodeComponent)],
    ['between', () => import('../../components/customNodes/nodes/number/between-node.component').then(m => m.BetweenNodeComponent)],
    ['notBetween', () => import('../../components/customNodes/nodes/number/not-between-node.component').then(m => m.NotBetweenNodeComponent)],

    // String predicates
    ['contains', () => import('../../components/customNodes/nodes/string/contains-node.component').then(m => m.ContainsNodeComponent)],
    ['notContains', () => import('../../components/customNodes/nodes/string/not-contains-node.component').then(m => m.NotContainsNodeComponent)],
    ['startsWith', () => import('../../components/customNodes/nodes/string/starts-with-node.component').then(m => m.StartsWithNodeComponent)],
    ['endsWith', () => import('../../components/customNodes/nodes/string/ends-with-node.component').then(m => m.EndsWithNodeComponent)],
    ['equalsCaseSensitive', () => import('../../components/customNodes/nodes/string/equals-case-sensitive-node.component').then(m => m.EqualsCaseSensitiveNodeComponent)],
    ['equalsIgnoreCase', () => import('../../components/customNodes/nodes/string/equals-ignore-case-node.component').then(m => m.EqualsIgnoreCaseNodeComponent)],
    ['regexMatch', () => import('../../components/customNodes/nodes/string/regex-match-node.component').then(m => m.RegexMatchNodeComponent)],
    ['lengthEquals', () => import('../../components/customNodes/nodes/string/length-equals-node.component').then(m => m.LengthEqualsNodeComponent)],
    ['lengthGt', () => import('../../components/customNodes/nodes/string/length-gt-node.component').then(m => m.LengthGtNodeComponent)],
    ['lengthLt', () => import('../../components/customNodes/nodes/string/length-lt-node.component').then(m => m.LengthLtNodeComponent)],
    ['lengthBetween', () => import('../../components/customNodes/nodes/string/length-between-node.component').then(m => m.LengthBetweenNodeComponent)],

    // Array predicates
    ['notEmpty', () => import('../../components/customNodes/nodes/array/not-empty-node.component').then(m => m.NotEmptyNodeComponent)],
    ['containsElement', () => import('../../components/customNodes/nodes/array/contains-element-node.component').then(m => m.ContainsElementNodeComponent)],
    ['notContainsElement', () => import('../../components/customNodes/nodes/array/not-contains-element-node.component').then(m => m.NotContainsElementNodeComponent)],
    ['containsAll', () => import('../../components/customNodes/nodes/array/contains-all-node.component').then(m => m.ContainsAllNodeComponent)],
    ['containsAny', () => import('../../components/customNodes/nodes/array/contains-any-node.component').then(m => m.ContainsAnyNodeComponent)],
    ['containsNone', () => import('../../components/customNodes/nodes/array/contains-none-node.component').then(m => m.ContainsNoneNodeComponent)],
    ['aggregate', () => import('../../components/customNodes/nodes/array/aggregate-node.component').then(m => m.AggregateNodeComponent)],

    // Object predicates
    ['hasKey', () => import('../../components/customNodes/nodes/object/has-key-node.component').then(m => m.HasKeyNodeComponent)],
    ['lacksKey', () => import('../../components/customNodes/nodes/object/lacks-key-node.component').then(m => m.LacksKeyNodeComponent)],
    ['hasAllKeys', () => import('../../components/customNodes/nodes/object/has-all-keys-node.component').then(m => m.HasAllKeysNodeComponent)],
    ['hasAnyKey', () => import('../../components/customNodes/nodes/object/has-any-key-node.component').then(m => m.HasAnyKeyNodeComponent)],
    ['hasNoKeys', () => import('../../components/customNodes/nodes/object/has-no-keys-node.component').then(m => m.HasNoKeysNodeComponent)],

    // DateTime predicates
    ['before', () => import('../../components/customNodes/nodes/datetime/before-node.component').then(m => m.BeforeNodeComponent)],
    ['after', () => import('../../components/customNodes/nodes/datetime/after-node.component').then(m => m.AfterNodeComponent)],
    ['betweenDates', () => import('../../components/customNodes/nodes/datetime/between-dates-node.component').then(m => m.BetweenDatesNodeComponent)],
    ['sameDay', () => import('../../components/customNodes/nodes/datetime/same-day-node.component').then(m => m.SameDayNodeComponent)],
    ['sameMonth', () => import('../../components/customNodes/nodes/datetime/same-month-node.component').then(m => m.SameMonthNodeComponent)],
    ['sameYear', () => import('../../components/customNodes/nodes/datetime/same-year-node.component').then(m => m.SameYearNodeComponent)],
    ['weekdayIs', () => import('../../components/customNodes/nodes/datetime/weekday-is-node.component').then(m => m.WeekdayIsNodeComponent)],
    ['monthIs', () => import('../../components/customNodes/nodes/datetime/month-is-node.component').then(m => m.MonthIsNodeComponent)],
    ['ageGreaterThan', () => import('../../components/customNodes/nodes/datetime/age-greater-than-node.component').then(m => m.AgeGreaterThanNodeComponent)],
    ['withinLast', () => import('../../components/customNodes/nodes/datetime/within-last-node.component').then(m => m.WithinLastNodeComponent)],
    ['withinNext', () => import('../../components/customNodes/nodes/datetime/within-next-node.component').then(m => m.WithinNextNodeComponent)],
    ['timezoneOffsetEquals', () => import('../../components/customNodes/nodes/datetime/timezone-offset-equals-node.component').then(m => m.TimezoneOffsetEqualsNodeComponent)],

    // IO nodes
    ['constant', () => import('../../components/customNodes/nodes/IO/constant-node.component').then(m => m.ConstantNodeComponent)],
    ['input', () => import('../../components/customNodes/nodes/IO/input-node.component').then(m => m.InputNodeComponent)],

    // Boolean nodes
    ['isTrue', () => import('../../components/customNodes/nodes/bool/is-true-node.component').then(m => m.IsTrueNodeComponent)],
    ['isFalse', () => import('../../components/customNodes/nodes/bool/is-false-node.component').then(m => m.IsFalseNodeComponent)],
  ]);

  // For a cleaner .html and better readability
  readonly nodeCategories = [
    {
      title: 'General Predicates',
      nodes: [
        { type: 'exists', label: 'Exists' },
        { type: 'notExists', label: 'Not Exists' },
        { type: 'isNull', label: 'Is Null' },
        { type: 'isNotNull', label: 'Is Not Null' },
        { type: 'typeIs', label: 'Type Is' },
        { type: 'equals', label: 'Equals' },
        { type: 'notEquals', label: 'Not Equals' },
        { type: 'inSet', label: 'In Set' },
        { type: 'notInSet', label: 'Not In Set' },
        { type: 'oneOf', label: 'One Of' },
        { type: 'allOf', label: 'All Of' },
        { type: 'noneOf', label: 'None Of' },
        { type: 'matchesSchema', label: 'Matches Schema' },
      ]
    },
    {
      title: 'Logic Operators',
      nodes: [
        { type: 'and', label: 'And' },
        { type: 'or', label: 'Or' },
        { type: 'xor', label: 'XOR' },
        { type: 'not', label: 'Not' },
      ]
    },
    {
      title: 'Number Predicates',
      nodes: [
        { type: 'greaterThan', label: 'Greater Than' },
        { type: 'lessThan', label: 'Less Than' },
        { type: 'greaterOrEqual', label: 'Greater Or Equal' },
        { type: 'lessOrEqual', label: 'Less Or Equal' },
        { type: 'between', label: 'Between' },
        { type: 'notBetween', label: 'Not Between' },
      ]
    },
    {
      title: 'String Predicates',
      nodes: [
        { type: 'contains', label: 'Contains' },
        { type: 'notContains', label: 'Not Contains' },
        { type: 'startsWith', label: 'Starts With' },
        { type: 'endsWith', label: 'Ends With' },
        { type: 'equalsCaseSensitive', label: 'Equals (Case Sensitive)' },
        { type: 'equalsIgnoreCase', label: 'Equals (Ignore Case)' },
        { type: 'regexMatch', label: 'Regex Match' },
        { type: 'lengthEquals', label: 'Length Equals' },
        { type: 'lengthGt', label: 'Length Greater Than' },
        { type: 'lengthLt', label: 'Length Less Than' },
        { type: 'lengthBetween', label: 'Length Between' },
      ]
    },
    {
      title: 'Boolean Predicates',
      nodes: [
        { type: 'isTrue', label: 'Is True' },
        { type: 'isFalse', label: 'Is False' },
      ]
    },
    {
      title: 'Array/List Predicates',
      nodes: [
        { type: 'lengthEquals', label: 'Length Equals' },
        { type: 'lengthGt', label: 'Length Greater Than' },
        { type: 'lengthLt', label: 'Length Less Than' },
        { type: 'notEmpty', label: 'Not Empty' },
        { type: 'containsElement', label: 'Contains Element' },
        { type: 'notContainsElement', label: 'Not Contains Element' },
        { type: 'containsAll', label: 'Contains All' },
        { type: 'containsAny', label: 'Contains Any' },
        { type: 'containsNone', label: 'Contains None' },
        { type: 'aggregate', label: 'Aggregate' },
      ]
    },
    {
      title: 'Object Predicates',
      nodes: [
        { type: 'hasKey', label: 'Has Key' },
        { type: 'lacksKey', label: 'Lacks Key' },
        { type: 'hasAllKeys', label: 'Has All Keys' },
        { type: 'hasAnyKey', label: 'Has Any Key' },
        { type: 'hasNoKeys', label: 'Has No Keys' },
      ]
    },
    {
      title: 'Date/Time Predicates',
      nodes: [
        { type: 'before', label: 'Before' },
        { type: 'after', label: 'After' },
        { type: 'betweenDates', label: 'Between Dates' },
        { type: 'sameDay', label: 'Same Day' },
        { type: 'sameMonth', label: 'Same Month' },
        { type: 'sameYear', label: 'Same Year' },
        { type: 'weekdayIs', label: 'Weekday Is' },
        { type: 'monthIs', label: 'Month Is' },
        { type: 'ageGreaterThan', label: 'Age Greater Than' },
        { type: 'withinLast', label: 'Within Last' },
        { type: 'withinNext', label: 'Within Next' },
        { type: 'timezoneOffsetEquals', label: 'Timezone Offset Equals' },
      ]
    },
    {
      title: 'Input Nodes',
      nodes: [
        { type: 'input', label: 'Input' },
        { type: 'constant', label: 'Constant' },
      ]
    }
  ];

  constructor(private location: Location) { }

  private async getNodeType(type: string): Promise<any> {
    if (!type) return 'html-template';

    try {
      const importFunction = this.nodeTypeMap.get(type);
      if (importFunction) {
        return await importFunction();
      } else {
        console.warn(`Unknown node type: ${type}`);
        return 'html-template';
      }
    } catch (error) {
      console.error(`Failed to load node component for type: ${type}`, error);
      return 'html-template';
    }
  }

  return() {
    this.location.back();
  }

  public async createNode(event: any) { */
  /* sTODO Better error handling */
  /* if (!(event && event.event && event.data)) {
    console.log("An Error occurred while trying to create a node");
    return;
  }

  const { event: dragEvent, data } = event;
  const spaces = this.vflow().documentPointToFlowPoint(
    {
      x: dragEvent.x,
      y: dragEvent.y,
    },
    { spaces: true },
  );
  const [point] = spaces;

  const nodeType = await this.getNodeType(data?.type);

  this.store.nodes.set([
    ...this.store.nodes(),
    {
      id: crypto.randomUUID(),
      point: signal(point),
      type: nodeType,
      parentId: signal(point.spaceNodeId),
      data: signal({
        canDetach: spaces.length > 1,
      })
    }
  ]);
}

public createEdge({ source, target }: Connection) {
  this.store.edges.set([
    ...this.store.edges(),
    {
      id: `${source} -> ${target}`,
      source,
      target
    }
  ]);
} */
  //#endregion

  //#region new implementation
  private vflow = viewChild.required(VflowComponent);

  protected vflowNodes: DynamicNode[] = [];
  protected vflowEdges: Edge[] = [];
  protected isLoading = true;

  private readonly nodeTypeMap = new Map<string, any>([
    ['PROVIDER', ProviderNodeComponent],
    ['CONSTANT', ConstantNodeComponent],
    ['LOGIC', LogicNodeComponent]
  ])

  ngOnInit(): void {
    this.loadGraph();
  }

  private loadGraph(): void {
    //sTODO request to real backend API
    this.mockApiGetGraphData().subscribe({
      next: ({ nodes, edges }) => {
        this.loadNodes(nodes);
        this.loadEdges(edges);
        this.isLoading = false;

        // Delay for proper loading and rendering
        setTimeout(() => this.vflow().fitView({ padding: 0.2 }), 100);
      },
      error: (err) => {
        console.error('Failed to load graph data', err);
        this.isLoading = false;
      }
    });
  }

  private loadNodes(nodes: any[]): void {
    const scaleFactor = 200;
    this.vflowNodes = nodes.map(node => {
      const component = this.getNodeTypeComponent(node.nodeType);

      const nodeData = {
        name: node.name,
        arcId: node.arcId,
        jsonPath: node.jsonPath,
        value: node.value,
        operatorType: node.operatorType,
        inputNodes: node.inputNodes
      };

      return {
        id: node.id.toString(),
        type: component,
        point: signal({
          x: node.offsetX * scaleFactor,
          y: node.offsetY * scaleFactor
        }),
        data: signal(nodeData)
      };
    });
  }

  private loadEdges(edges: any[]): void {
    this.vflowEdges = edges.map(edge => ({
      id: edge.id,
      source: edge.source.toString(),
      target: edge.target.toString(),
      animated: edge.animated || false
    }))
  }

  private getNodeTypeComponent(nodeType: string): any {
    switch (nodeType) {
      case 'PROVIDER': return ProviderNodeComponent;
      case 'CONSTANT': return ConstantNodeComponent;
      case 'LOGIC': return LogicNodeComponent;
      default: return null;
    }
  }

  // Mock API service sTODO replace with real implementation
  private mockApiGetGraphData() {
    const mockData = {
      nodes: [
        {
          id: 0,
          offsetX: 0,
          offsetY: 0,
          nodeType: "PROVIDER",
          name: "currentTemperature",
          arcId: 22,
          jsonPath: "source.anlageAAS.arc_temperature22.sensorData.currentTemperature"
        },
        {
          id: 1,
          offsetX: 1,
          offsetY: 1,
          nodeType: "CONSTANT",
          name: "tempOffset",
          value: -2.0
        },
        {
          id: 2,
          offsetX: 1,
          offsetY: 0,
          nodeType: "PROVIDER",
          name: "maxAllowedThreshold",
          arcId: 11,
          jsonPath: "source.anlageAAS.arc_thresholds38.maxAllowedTemp"
        },
        {
          id: 3,
          offsetX: 0,
          offsetY: 1,
          nodeType: "CONSTANT",
          name: "SystemEnabledFlag",
          value: true
        },
        {
          id: 4,
          offsetX: 2,
          offsetY: 1,
          nodeType: "LOGIC",
          operatorType: "ADD",
          inputNodes: [{ id: 0, orderIndex: 0 }, { id: 1, orderIndex: 1 }]
        },
        {
          id: 5,
          offsetX: 2,
          offsetY: 2,
          nodeType: "LOGIC",
          operatorType: "LESS_THAN",
          inputNodes: [{ id: 4, orderIndex: 0 }, { id: 2, orderIndex: 1 }]
        },
        {
          id: 6,
          offsetX: 4,
          offsetY: 0,
          nodeType: "LOGIC",
          operatorType: "AND",
          inputNodes: [{ id: 5, orderIndex: 0 }, { id: 3, orderIndex: 1 }]
        }
      ],
      edges: [
        { id: '0->4', source: 0, target: 4 },
        { id: '1->4', source: 1, target: 4 },
        { id: '4->5', source: 4, target: 5 },
        { id: '2->5', source: 2, target: 5 },
        { id: '5->6', source: 5, target: 6 },
        { id: '3->6', source: 3, target: 6 }
      ]
    };

    return of(mockData).pipe(delay(500));
  }
  //#endregion
}
