/**
 * Interface representing an item of a node's context menu
 */
export interface NodeMenuItem {
  label?: string;
  action?: () => void;
  submenu?: NodeMenuItem[];
}
