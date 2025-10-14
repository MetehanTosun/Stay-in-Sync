import { CustomVFlowNode, LogicNodeData, NodeType } from '../../models';
import { ProviderNodeComponent } from '../nodes/provider-node/provider-node.component';
import { ConstantNodeComponent } from '../nodes/constant-node/constant-node.component';
import { LogicNodeComponent } from '../nodes/logic-node/logic-node.component';
import { SchemaNodeComponent } from '../nodes/schema-node/schema-node.component';
import { FinalNodeComponent } from '../nodes/final-node/final-node.component';
import { ConfigNodeComponent } from '../nodes/config-node/config-node.component';
import { MessageService } from 'primeng/api';

//#region Getters
/**
 * Maps a NodeType to its corresponding component class.
 * @param nodeType The NodeType to map.
 * @param messageService the MessageService errors should be sent with
 * @returns The component class for the NodeType.
 */
export function getNodeType(nodeType: NodeType, messageService?: MessageService) {
  switch (nodeType) {
    case NodeType.PROVIDER: return ProviderNodeComponent;
    case NodeType.CONSTANT: return ConstantNodeComponent;
    case NodeType.LOGIC: return LogicNodeComponent;
    case NodeType.SCHEMA: return SchemaNodeComponent;
    case NodeType.FINAL: return FinalNodeComponent;
    case NodeType.CONFIG: return ConfigNodeComponent;
    default:
      messageService?.add({
        severity: 'error',
        summary: 'Unknown NodeType',
        detail: 'An unknown node type was attempted to be accessed. \n Please check the logs or the console.'
      });
      throw Error("Unknown NodeType"); // *For compiler validation
  }
}

/**
 * Returns the default node size for a given NodeType.
 * If nodeType is not recognized, returns object { width: 320, height: 175 }
 */
export function getDefaultNodeSize(nodeType: NodeType): { width: number; height: number } {
  switch (nodeType) {
    case NodeType.CONSTANT:
      return { width: 220, height: 138 };
    case NodeType.PROVIDER:
      return { width: 420, height: 208 };
    case NodeType.LOGIC:
      return { width: 320, height: 175 };
    case NodeType.FINAL:
      return { width: 320, height: 175 };
    case NodeType.CONFIG:
      return { width: 320, height: 175 };
    case NodeType.SCHEMA:
      return { width: 320, height: 175 };
    default:
      return { width: 320, height: 175 };
  }
}

/**
 * Returns the expected input type of a targetted node.
 */
export function getExpectedInputType(target: CustomVFlowNode, targetHandle?: string): string {
  const inputTypes = (target.data as LogicNodeData).inputTypes;
  if (target.data.nodeType === NodeType.FINAL) return 'BOOLEAN';

  if (targetHandle) {
    const match = targetHandle.match(/input-(\d+)/);
    const index = match ? parseInt(match[1], 10) : 0;
    return inputTypes![index];
  }
  return inputTypes![0];
}
//#endregion

//#region Template Helpers
/**
 * Checks whether an object has an own property with the given name.
 *
 * @param obj the object to check
 * @param propName the property name to look for
 * @returns true when `obj` is non-null and owns property `propName`
 */
export function hasProp(obj: any, propName: string): boolean {
  return !!obj && Object.prototype.hasOwnProperty.call(obj, propName);
}

/**
 * Checks whether an object's property exists and has the requested type
 *
 * @param obj the object to inspect
 * @param propName property name to check
 * @param type expected type: 'string' | 'number' | 'boolean' | 'array' | 'object'
 * @returns true when the property exists and matches the expected type
 */
export function hasPropOfType(obj: any, propName: string, type: 'string' | 'number' | 'boolean' | 'array' | 'object'): boolean {
  if (!obj) return false;
  const val = (obj as any)[propName];
  if (type === 'array') return Array.isArray(val);
  if (type === 'object') return val !== null && typeof val === 'object' && !Array.isArray(val);
  return typeof val === type;
}

/**
 * Returns a property from an object or a provided default if it does not exist.
 *
 * @param obj object to read from
 * @param propName property name
 * @param defaultValue value to return when property is missing or null
 */
export function getPropIfExists<T>(obj: any | undefined | null, propName: string, defaultValue: T): T {
  if (!obj) return defaultValue;
  const val = (obj as any)[propName];
  return val !== undefined && val !== null ? val : defaultValue;
}
//#endregion

//#region Node Creation Helpers

/**
 * Build node data for a new node. This function does not mutate component state.
 * @param nodeType the NodeType
 * @param opts provider/constant/operator inputs
 * @param nextNodeId optional next numeric node id used for schema naming
 * @param messageService optional MessageService to report an error when inputs are invalid
 */
export function buildNodeData(
  nodeType: NodeType,
  opts: {
    providerData?: { jsonPath: string, outputType: string },
    constantValue?: any,
    operatorData?: any
  },
  nextNodeId?: number,
  messageService?: MessageService
): any | undefined {
  if (nodeType === NodeType.PROVIDER && opts.providerData) {
    return {
      name: opts.providerData.jsonPath,
      jsonPath: opts.providerData.jsonPath,
      outputType: opts.providerData.outputType
    };
  }

  if (nodeType === NodeType.CONSTANT && opts.constantValue !== undefined) {
    return {
      name: `Constant: ${opts.constantValue}`,
      value: opts.constantValue,
      outputType: inferTypeFromValue(opts.constantValue)
    };
  }

  if (nodeType === NodeType.LOGIC && opts.operatorData) {
    return {
      ...opts.operatorData,
      name: opts.operatorData.operatorName,
      operatorType: opts.operatorData.operatorName,
    };
  }

  if (nodeType === NodeType.SCHEMA && opts.constantValue !== undefined) {
    return {
      name: `Schema: ${nextNodeId ?? 0}`,
      value: opts.constantValue,
      outputType: 'JSON'
    };
  }

  messageService?.add({
    severity: 'error',
    summary: 'Unable to create node',
    detail: 'Unable to create node. \n Please check the logs.'
  });
  return undefined;
}

/**
 * Calculate the top-left point for the node so it is visually centered on the given position.
 */
export function calculateNodeCenter(nodeType: NodeType, pos: { x: number, y: number }) {
  const size = getDefaultNodeSize(nodeType);
  return {
    x: pos.x - size.width / 2,
    y: pos.y - size.height /2
  };
}

/**
 * Create a new CustomVFlowNode from provided values
 */
export function createNode(nodeType: NodeType, id: string, point: { x: number, y: number }, data: any): CustomVFlowNode {
  const newNode: CustomVFlowNode = {
    id,
    point,
    type: getNodeType(nodeType),
    width: getDefaultNodeSize(nodeType).width,
    height: getDefaultNodeSize(nodeType).height,
    data: {
      ...data,
      nodeType: nodeType
    },
    contextMenuItems: []
  };
  return newNode;
}
//#endregion

//#region Genereic Helpers
/**
 * Infers a data type label from a JS value.
 */
export function inferTypeFromValue(value: any): string {
  if (value === null) return 'ANY';

  const jsType = typeof value;
  switch (jsType) {
    case 'number':
      return 'NUMBER';
    case 'string':
      if (isValidDateTime(value)) return 'DATE';
      return 'STRING';
    case 'boolean':
      return 'BOOLEAN';
    case 'object':
      if (Array.isArray(value)) return 'ARRAY';
      return 'JSON';
    default:
      return 'ANY';
  }
}

/**
 * Simple ISO-8601 check used by inferTypeFromValue.
 */
function isValidDateTime(value: string): boolean {
  const iso8601Pattern = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d{1,3})?(Z|[+-]\d{2}:\d{2})?$/;
  if (!iso8601Pattern.test(value)) return false;
  try {
    const date = new Date(value);
    return !isNaN(date.getTime());
  } catch (err) {
    return false;
  }
}

/**
 * Converts screen coordinates to VFlow coordinates.
 * @param mouseEvent The MouseEvent the coordinates should be calculated of
 * @param viewport The viewport object from VFlow.
 * @returns VFlow coordinates
 */
export function calculateVFlowCoordinates(mouseEvent: MouseEvent, viewport: { x: number; y: number; zoom: number }
): { x: number, y: number } {
  const rect = (mouseEvent.currentTarget as HTMLElement).getBoundingClientRect();
  const screenX = mouseEvent.clientX - rect.left;
  const screenY = mouseEvent.clientY - rect.top;

  const flowX = (screenX - viewport.x) / viewport.zoom;
  const flowY = (screenY - viewport.y) / viewport.zoom;
  return { x: flowX, y: flowY };
}
//#endregion
