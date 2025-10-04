# Working with Sync Rules

This guide explains how to work with Sync Rules inside the Stay-in-Sync Configurator UI.

## What are Sync Rules?

Sync Rules allow you to define, edit, and manage transformation rule graphs that control data synchronization logic.  
For this purpose, this component uses mainly the Rules Overview page and the Rule Editor.

- **Rules Overview**: View, and manage the the list of transformation rules.
- **Rule Editor**: Edit the graph-based boolean condition of a transformation rule.

## Rules Overview

This page lists all graph-based transformation rules in a table.
Included features in this page are:

- **Search** for rules by name using the search bar on the top right.
- **Create** a new rule with the button on the top right or by right clicking the canvas.  
*This will open up a modal dialog and requires you to input both a name and description of this new rule*
- **Edit** an existing rule using the edit action. - *This will open the Rule Editor*
- **Delete** a rule using the delete action.

Additionally, if you click on the name of a  particular rule, the rule will expand with its description inside the table.

## Rule Editor

> Opens if you have chosen to either create or edit a rule.

### Create a new Node

#### Using the "Add Node" button

1. Open the node palette with the "Add Node" button on the top right
2. Choose a node type:
   - **Provider node**: An input node that contains a variable pulled from an ARC
   - **Logic node**: Logical operation nodes used to evaluate the values of one or more nodes
   - **Constant node**: An static node containing a set value
3. If you have chosen to create a logic node, the palette expands with the categories the logic nodes are grouped under.
In which you than can choose the to be created logic node.
4. After selecting a node you may click on the canvas to create the node on the cursor position.
If you have selected to create a provider or constant node, you will be prompted to add additional information to configure the node.

#### Using right-click on the canvas

Alternatively you can right click the canvas to display the node palate next to your cursor. Following the steps 2 and 3 from before, the node will now be created where the node palate was initially called from.

### Additional Nodes

> Those nodes cannot be created or deleted

- **Final node**: This node receives only boolean inputs and represents the end result of the transformation graphs boolean condition
- **Config node**: This node controls the behavior of a transformation graphs logic and only accepts values from provider nodes.  
  If enabled, all nodes aside the ones connected to the config node will be ignored  
  and you may choose from two operations `AND` or `OR`, after which the node will register any changes to the values of connected Provider nodes.
  - `AND`: True, if all values changed
  - `OR`: True, if any value changed

### Error Panel

This page component displays all validation errors that the application identifies when you try to save the transformation graph.  
If any validation errors are identified, the rule will be saved as a draft until you fix the error.  
Certain errors contain information about the problematic node. In this case you can click on the error panel item to quickly navigate to the node.
