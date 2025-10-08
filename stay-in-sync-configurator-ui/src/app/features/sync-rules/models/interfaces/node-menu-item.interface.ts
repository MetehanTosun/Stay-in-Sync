/**
 * Type representing an item or a collection of items of a node's context menu
 */
export type NodeMenuItem =
  | {
    label: string;
    action: () => void;
  }
  | {
    submenu: NodeMenuItem[];
  }
