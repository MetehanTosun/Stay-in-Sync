import { Component, inject, signal, viewChild } from '@angular/core';
import { Location } from '@angular/common';
import { Connection, Vflow, VflowComponent } from 'ngx-vflow';
import { DndModule } from 'ngx-drag-drop';
import { FlowStoreService } from '../../components/customNodes/flow-store.service';


@Component({
  selector: 'app-edit-rule',
  imports: [Vflow, DndModule],
  templateUrl: './edit-rule.component.html',
  styleUrl: './edit-rule.component.css',
  providers: [FlowStoreService]
})
export class EditRuleComponent {
  protected store = inject(FlowStoreService);
  protected vflow = viewChild.required(VflowComponent);

  // Map of node types with lazy loading
  private readonly nodeTypeMap = new Map<string, () => Promise<any>>([
    ['exists', () => import('../../components/customNodes/nodes/exists-node.component').then(m => m.ExistsNodeComponent)],
    ['notExists', () => import('../../components/customNodes/nodes/not-exists-node.component').then(m => m.NotExistsNodeComponent)],
    ['constant', () => import('../../components/customNodes/nodes/constant-node.component').then(m => m.ConstantNodeComponent)],
    ['input', () => import('../../components/customNodes/nodes/input-node.component').then(m => m.InputNodeComponent)],
    ['isNull', () => import('../../components/customNodes/nodes/is-null-node.component').then(m => m.IsNullNodeComponent)],
    ['isNotNull', () => import('../../components/customNodes/nodes/is-not-null-node.component').then(m => m.IsNotNullNodeComponent)],
    ['typeIs', () => import('../../components/customNodes/nodes/type-is-node.component').then(m => m.TypeIsNodeComponent)],
    ['equals', () => import('../../components/customNodes/nodes/equals-node.component').then(m => m.EqualsNodeComponent)],
    ['notEquals', () => import('../../components/customNodes/nodes/not-equals-node.component').then(m => m.NotEqualsNodeComponent)],
    ['and', () => import('../../components/customNodes/nodes/and-node.component').then(m => m.AndNodeComponent)],
    ['or', () => import('../../components/customNodes/nodes/or-node.component').then(m => m.OrNodeComponent)],
    ['not', () => import('../../components/customNodes/nodes/not-node.component').then(m => m.NotNodeComponent)],
    ['greaterThan', () => import('../../components/customNodes/nodes/greater-than-node.component').then(m => m.GreaterThanNodeComponent)],
    ['lessThan', () => import('../../components/customNodes/nodes/less-than-node.component').then(m => m.LessThanNodeComponent)],
    ['between', () => import('../../components/customNodes/nodes/between-node.component').then(m => m.BetweenNodeComponent)],
    ['contains', () => import('../../components/customNodes/nodes/contains-node.component').then(m => m.ContainsNodeComponent)],
    ['startsWith', () => import('../../components/customNodes/nodes/starts-with-node.component').then(m => m.StartsWithNodeComponent)],
    ['endsWith', () => import('../../components/customNodes/nodes/ends-with-node.component').then(m => m.EndsWithNodeComponent)],
    ['isTrue', () => import('../../components/customNodes/nodes/is-true-node.component').then(m => m.IsTrueNodeComponent)],
    ['isFalse', () => import('../../components/customNodes/nodes/is-false-node.component').then(m => m.IsFalseNodeComponent)],
    ['lengthEquals', () => import('../../components/customNodes/nodes/length-equals-node.component').then(m => m.LengthEqualsNodeComponent)],
    ['notEmpty', () => import('../../components/customNodes/nodes/not-empty-node.component').then(m => m.NotEmptyNodeComponent)],
    ['hasKey', () => import('../../components/customNodes/nodes/has-key-node.component').then(m => m.HasKeyNodeComponent)],
    ['before', () => import('../../components/customNodes/nodes/before-node.component').then(m => m.BeforeNodeComponent)],
    ['after', () => import('../../components/customNodes/nodes/after-node.component').then(m => m.AfterNodeComponent)],
    ['inSet', () => import('../../components/customNodes/nodes/in-set-node.component').then(m => m.InSetNodeComponent)],
    ['notInSet', () => import('../../components/customNodes/nodes/not-in-set-node.component').then(m => m.NotInSetNodeComponent)],
    ['oneOf', () => import('../../components/customNodes/nodes/one-of-node.component').then(m => m.OneOfNodeComponent)],
    ['allOf', () => import('../../components/customNodes/nodes/all-of-node.component').then(m => m.AllOfNodeComponent)],
    ['noneOf', () => import('../../components/customNodes/nodes/none-of-node.component').then(m => m.NoneOfNodeComponent)],
    ['xor', () => import('../../components/customNodes/nodes/xor-node.component').then(m => m.XorNodeComponent)],
    ['matchesSchema', () => import('../../components/customNodes/nodes/matches-schema-node.component').then(m => m.MatchesSchemaNodeComponent)],
    ['greaterOrEqual', () => import('../../components/customNodes/nodes/greater-or-equal-node.component').then(m => m.GreaterOrEqualNodeComponent)],
    ['lessOrEqual', () => import('../../components/customNodes/nodes/less-or-equal-node.component').then(m => m.LessOrEqualNodeComponent)],
    ['notBetween', () => import('../../components/customNodes/nodes/not-between-node.component').then(m => m.NotBetweenNodeComponent)],
    ['equalsCaseSensitive', () => import('../../components/customNodes/nodes/equals-case-sensitive-node.component').then(m => m.EqualsCaseSensitiveNodeComponent)],
    ['equalsIgnoreCase', () => import('../../components/customNodes/nodes/equals-ignore-case-node.component').then(m => m.EqualsIgnoreCaseNodeComponent)],
    ['notContains', () => import('../../components/customNodes/nodes/not-contains-node.component').then(m => m.NotContainsNodeComponent)],
    ['regexMatch', () => import('../../components/customNodes/nodes/regex-match-node.component').then(m => m.RegexMatchNodeComponent)],
    ['lengthGt', () => import('../../components/customNodes/nodes/length-gt-node.component').then(m => m.LengthGtNodeComponent)],
    ['lengthLt', () => import('../../components/customNodes/nodes/length-lt-node.component').then(m => m.LengthLtNodeComponent)],
    ['lengthBetween', () => import('../../components/customNodes/nodes/length-between-node.component').then(m => m.LengthBetweenNodeComponent)],
    ['containsElement', () => import('../../components/customNodes/nodes/contains-element-node.component').then(m => m.ContainsElementNodeComponent)],
    ['notContainsElement', () => import('../../components/customNodes/nodes/not-contains-element-node.component').then(m => m.NotContainsElementNodeComponent)],
    ['containsAll', () => import('../../components/customNodes/nodes/contains-all-node.component').then(m => m.ContainsAllNodeComponent)],
    ['containsAny', () => import('../../components/customNodes/nodes/contains-any-node.component').then(m => m.ContainsAnyNodeComponent)],
    ['containsNone', () => import('../../components/customNodes/nodes/contains-none-node.component').then(m => m.ContainsNoneNodeComponent)],
    ['aggregate', () => import('../../components/customNodes/nodes/aggregate-node.component').then(m => m.AggregateNodeComponent)],
    ['lacksKey', () => import('../../components/customNodes/nodes/lacks-key-node.component').then(m => m.LacksKeyNodeComponent)],
    ['hasAllKeys', () => import('../../components/customNodes/nodes/has-all-keys-node.component').then(m => m.HasAllKeysNodeComponent)],
    ['hasAnyKey', () => import('../../components/customNodes/nodes/has-any-key-node.component').then(m => m.HasAnyKeyNodeComponent)],
    ['hasNoKeys', () => import('../../components/customNodes/nodes/has-no-keys-node.component').then(m => m.HasNoKeysNodeComponent)],
    ['betweenDates', () => import('../../components/customNodes/nodes/between-dates-node.component').then(m => m.BetweenDatesNodeComponent)],
    ['sameDay', () => import('../../components/customNodes/nodes/same-day-node.component').then(m => m.SameDayNodeComponent)],
    ['sameMonth', () => import('../../components/customNodes/nodes/same-month-node.component').then(m => m.SameMonthNodeComponent)],
    ['sameYear', () => import('../../components/customNodes/nodes/same-year-node.component').then(m => m.SameYearNodeComponent)],
    ['weekdayIs', () => import('../../components/customNodes/nodes/weekday-is-node.component').then(m => m.WeekdayIsNodeComponent)],
    ['monthIs', () => import('../../components/customNodes/nodes/month-is-node.component').then(m => m.MonthIsNodeComponent)],
    ['ageGreaterThan', () => import('../../components/customNodes/nodes/age-greater-than-node.component').then(m => m.AgeGreaterThanNodeComponent)],
    ['withinLast', () => import('../../components/customNodes/nodes/within-last-node.component').then(m => m.WithinLastNodeComponent)],
    ['withinNext', () => import('../../components/customNodes/nodes/within-next-node.component').then(m => m.WithinNextNodeComponent)],
    ['timezoneOffsetEquals', () => import('../../components/customNodes/nodes/timezone-offset-equals-node.component').then(m => m.TimezoneOffsetEqualsNodeComponent)],
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

  public async createNode(event: any) {
    /* TODO Better error handling */
    if (!(event && event.event && event.data)) {
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
  }
}
