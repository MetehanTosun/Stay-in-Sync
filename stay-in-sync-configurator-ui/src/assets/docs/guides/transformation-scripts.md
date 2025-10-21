# The Transformation Script Editor

Welcome to the Stay-in-Sync Transformation Script Editor! This guide will walk you through its features and help you write powerful scripts to transform and route data between your systems.

<br>

> ### ⚠️ **Important Disclaimer & Known Issues**
>
> **Known Issue:** The polling nodes are unable to get data from an AAS instance because the code architecture is not implemented. This makes development with the usage of AAS instances as a source system **impossible**.

> **Known Issue:** The core-management implementation of an AAS instance as a target system only works on live data and doesn't support snapshot recordings of the instance, which are required to serialize typesafe `.d.ts` files for directives of such type.
>
> **Workaround:** If an AAS instance needs to be written to as a target system, this AAS instance has to be registered as a **SOURCE SYSTEM** and provided with required authentication headers in the Source System Management Tab.

> **Known Issue:** After creating or deleting a Target ARC, you may briefly see TypeScript errors in the editor, or the autocompletion for the `targets` object may not update immediately.
>
> **Workaround:** If you experience this and the monaco editor doesn't find the types after a couple seconds, the most reliable way to fix it is a **simple browser refresh (F5 or Ctrl+R)**. This will force the editor to reload with the latest, correct type definitions from the server. Make sure to press the **Save Script** button in the top right corner of the editor.

## Introduction to Transformation Scripts

The Transformation Script Editor is the heart of Stay-in-Sync's data mapping capabilities. Its primary purpose is to allow you to write custom TypeScript code that defines how data from one or more **Source Systems** is processed and sent to one or more **Target Systems**.

Every script revolves around a single entry point: the `transform()` function. Your job is to implement this function to read data from your configured sources and return a specific set of instructions, called **Directives**, for your targets.

## Core Concepts

Before diving into the UI, it's essential to understand two key concepts: **Source ARCs** and **Target ARCs**.

### Source ARCs (API Request Configurations)
A Source ARC is a reusable configuration that defines how to **fetch data *from*** a source system. It's essentially a pre-configured, scheduled API call. When you create a Source ARC, you give it a unique **Alias** (e.g., `getActiveUsers`). In your script, this becomes available under the global `source` object, like this:

`const users = source.MyCRM.getActiveUsers;`

The script editor automatically provides the full TypeScript types for the data returned by each Source ARC, giving you excellent autocompletion.

### Target ARCs & Directives
A Target ARC defines a pattern of interaction **_with_** a target system. Instead of fetching data, it provides a "builder" that you use to create instructions, or **Directives**. For example, a "List Upsert" ARC provides builders to `CHECK` if an item exists, `CREATE` it if it doesn't, or `UPDATE` it if it does.

You access these builders through the global `targets` object. An ARC with the alias `synchronizeProducts` would be used like this:

`const directive = targets.synchronizeProducts.defineUpsert().build();`

The final output of your `transform()` function is an object where the keys match your Target ARC aliases and the values are arrays of these directives.

## Anatomy of the Editor

The editor is divided into three main panels, each serving a distinct purpose.

---

### 1. The Data Sources Panel (Left)

This panel is your library of available data. It lists all configured Source Systems (both REST and AAS) and their available endpoints or submodels. This is where you configure and manage the global `source` object.

#### Creating a Source ARC (REST)

To make an endpoint's data available in your script, you must create a Source ARC for it.

1.  Find the Source System and Endpoint you want to use (e.g., `GET /products`).
2.  Click the **`+`** button next to the endpoint to open the "API Request Configuration (ARC)" wizard.

The wizard has three steps:

**Step 1: Configuration**

*   **Alias:** This is the most important field. Give your ARC a descriptive, camelCase name (e.g., `getAllProducts`, `fetchUserDetails`). This alias will be used to access the data in your script: `source.SystemName.yourAlias`. It is important that this alias adheres to the identifier conventions for TypeScript-Code.
*   **Polling Rate:** Defines how often Stay-in-Sync should fetch this data in the background.

**Step 2: Parameters**

Here, you can set values for any query parameters or headers required by the API endpoint. You can add predefined parameters (if detected from an OpenAPI spec) or add custom ones.

**Step 3: Test & Review**

This is a crucial step.
1.  Click **"Test API Call"** to execute a live request with your configuration.
2.  The wizard will show you the exact structure of the response payload in a tree view.
3.  It also automatically generates the **TypeScript Type** for that payload.
4.  You **must** run a successful test call before you can save the ARC. This ensures that the data provided to your script is always strongly typed.
5.  Click **"Save ARC"**. The wizard will close, and your new ARC will appear in the "Data Sources" panel, ready to be used.

#### Creating a Source ARC (AAS)
If you have an AAS-enabled Source System, you can also create a Source ARC to poll data from one of its submodels.

1.  Click the **`+`** button next to the desired Submodel (e.g., `TechnicalData`).
2.  The "Create AAS ARC" wizard will open.
3.  Provide an **Alias** and a **Polling Rate**.
4.  Click **"Create ARC"**. The submodel's data structure will now be available and fully typed under `source.YourAasSystem.yourAlias`.

---

### 2. The Code Editor (Center)

This is where you write your transformation logic in TypeScript. The editor provides rich autocompletion and type-checking based on the Source and Target ARCs you've configured.

#### Global Objects

Your script has access to three pre-defined global objects:

*   **`stayinsync`**: A helper object with utility functions.
    *   `stayinsync.log(message, level)`: Logs a message to the Sync Job's execution history.
*   **`source`**: Contains all your configured Source ARCs, grouped by system name. This is how you get your input data.
*   **`targets`**: Contains all your configured Target ARCs. This is how you build your output directives.

#### The `transform()` Function

Your script must contain a function named `transform`. This function is the entry point for the execution engine. It must return an object that conforms to the `DirectiveMap` interface, which is dynamically generated for you.

```typescript
/**
 * Transforms products from the source into upsert directives for the target.
 */
function transform(): DirectiveMap {
    // Log the start of the transformation
    stayinsync.log('Transformation started: Upserting products...', 'INFO');

    // 1. ACCESS DATA from a Source ARC
    // The path is: source.[SystemName].[ArcAlias].[...payload structure]
    const products = source.Dummy_JSON.syncProductsArc.products;
    const supplierName = source.TestAAS.technicalData.SupplierName; // Also works for AAS Sources

    // 2. BUILD DIRECTIVES for your Target ARCs
    // The path is: targets.[TargetArcAlias]
    const productDirectives = products.map(product => {
        return targets.synchronizeProducts.defineUpsert()
            // Configure the CHECK action using a builder
            .usingCheck(config => {
                config.withQueryParamQ(product.title);
            })
            // Configure the CREATE action
            .usingCreate(config => {
                config.withPayload({ title: product.title, /* ... */ });
            })
            // Configure the UPDATE action
            .usingUpdate(config => {
                config.withPathParamProductId(checkResponse => checkResponse.products[0].id)
                      .withPayload({ price: product.price, /* ... */ });
            })
            // Finalize the directive
            .build();
    });

    // You can also build directives for AAS targets
    const aasDirective = targets.synchronizeShell.SupplierID.setValue("123").build();

    // 3. RETURN THE DIRECTIVEMAP
    // The keys MUST match your Target ARC aliases.
    // If you are unable to fit the right type, Check out the definitions of the returnType. 
    // It might be required to wrap the directive as an array as presented for
    return {
        synchronizeProducts: productDirectives,
        synchronizeShell: [aasDirective] // array wrap might be required
    };
}
```

### 3. The Target Directives Panel (Right)

This panel shows the Target ARCs that are active for this transformation script. This is how you configure the `targets` object and the structure of the `DirectiveMap` your script must return.

#### Adding Target ARCs

You have two ways to add a Target ARC to your script's context:

**1. Create New**

Clicking the **"Create New"** button gives you two options:

*   **New REST ARC:** This opens a wizard to configure a REST-based interaction pattern.
    *   **Alias:** A unique, camelCase name for this target configuration (e.g., `synchronizeProducts`). This becomes the key in the `DirectiveMap` and the property on the `targets` object. (TS identifier conventions apply too)
    *   **Target System:** The destination system for these directives.
    *   **ARC Type:** The interaction pattern. For example, "List Upsert" requires you to define endpoints for checking, creating, and updating items.
    *   **Endpoint Mapping (CHECK, CREATE, UPDATE):** You must select the specific API endpoints from the Target System that correspond to each action in the chosen pattern.

*   **New AAS ARC:** This opens a wizard to configure a Target ARC for an AAS Submodel.
    *   **Alias:** A unique, camelCase name (e.g., `updateTechnicalData`).
    *   **Target System:** The AAS-enabled system.
    *   **Submodel:** The specific Submodel you intend to interact with. The editor will automatically generate a deeply-typed builder for this Submodel's structure.

> **Workaround for Single API-Call:** A Basic API Call is only possible to be constructed as an **Upsert** operation where the real api endpoint call is placed as the **CHECK** selection and the other fields are simply selected with a random endpoint. When filling out the declaration of the UpsertObject, only fill out the **CHECK** response and call `build()` immediately.

**2. From Library**

Clicking the **"From Library"** button opens a dialog that lists all Target ARCs that exist in your entire Stay-in-Sync instance, grouped by their Target System. This allows you to quickly reuse a Target ARC that you have already configured for another transformation without having to create it again.
