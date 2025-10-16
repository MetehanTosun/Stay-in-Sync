# Working with Sync Rules

This guide explains how to work with Sync Rules inside the Stay-in-Sync Configurator UI.

## What are Sync Rules?

Sync Rules allow you to define, edit, and manage transformation rule graphs that control data synchronization logic.  
For this purpose, this component uses mainly the Rules Overview page and the Rule Editor.

- **Rules Overview**: View, and manage the the list of transformation rules.
- **Rule Editor**: Edit the graph-based boolean condition of a transformation rule.

## Rules Overview

Inside the rules overview you'll find all graph-based transformation rules in a table

Here you may **search**, **create**, **edit** or **delete** a rule.

###

- **Search** for rules by name using the search bar on the top right.
- **Create** a new rule with the button on the top right.  
- **Edit** an existing rule using the edit action to the right. - *This will open the Rule Editor*
- **Delete** a rule using the delete action to the right.

## Rule Editor

> Opens if you have chosen to either create or edit a rule.

### Create a new Node

- **Node Palette**

To add a new node you'll need to open the node palette with the "Add Node" button on the top right

or by right clicking the canvas - the area the graph is rendered on.

- **Node type**

Inside the node palette you'll have to choose a node type:

- **Provider node**: An input node that contains a variable pulled from an ARC
- **Logic node**: Logical operation nodes used to evaluate the values of one or multiple nodes
- **Constant node**: A static node containing a set value
- **Schema node**: A static node similar to a constant node - used for JSON schemas

---

- *Note: Logic nodes*

In contrast to the other node types, logic nodes have 2 sub-palettes.

In the first one, you can choose between different categories of operations (e.g. number, boolean),

after which you may choose the logic node to create.

---

- **Additional Information**

After choosing a node you may be asked to provide additional information about the node you want to create.

- **Node Instantiation**

Lastly depending on wether you opened the node palette with the top right button or via right click,

the pending node creation will wait for you to click on the desired node position

or the node may be created where the node palette was opened.

### Editing Node Data

To edit a node you may click on a node.

This will open a node context menu that differs between all node types.

### Special Nodes

> The following nodes are created on rule creation and cannot be created or deleted

- **Final node**: This node receives only boolean inputs and represents the end result of the transformation graphs boolean condition
- **Config node**: This node controls the behavior of a transformation graphs logic and only accepts values from provider nodes.  

---

- *Note: Config nodes*

You may configure the config nodes by toggling their mode, status or setting the time window.

- **Mode: `AND`** - outputs true if all connected provider nodes changed their values
- **Mode: `OR`** - outputs true if one of all connected provider nodes changed their value

The status just toggles if the node is activated or deactivated.

The time window sets how long the Config node waits for multiple provider changes when in AND mode.

You may choose from 5, 10, 15, 20, 25 or 30 seconds

---

### Error Panel

On the right you find the Error Panel.

This component displays all the validation errors the application identifies while saving the rule.

Errors about specific nodes are clickable and allow you to jump to their position.
