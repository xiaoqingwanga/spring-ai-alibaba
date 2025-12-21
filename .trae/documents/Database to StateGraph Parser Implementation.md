# Implementation Plan: Bean-Based StateGraph Parser

I will implement a `GraphParser` that constructs a `StateGraph` by resolving node logic from the Spring ApplicationContext (Beans) or loading nested graphs. This approach avoids "class names" and custom registries, relying instead on standard Spring Bean names and the node types described in `5-node-types.md`.

## 1. Data Models (POJOs)
I will create these classes in `com.alibaba.cloud.ai.graph.parser`.

*   **`NodeDefinition`**:
    *   `id`: Node identifier (String).
    *   `type`: Enum `NodeType` (`STANDARD`, `SUB_GRAPH`).
    *   `resource`: The reference to the logic.
        *   For `STANDARD`: The **Spring Bean Name** of the `AsyncNodeAction` or `AsyncCommandAction`.
        *   For `SUB_GRAPH`: The **Graph ID** to be loaded.
    *   `properties`: Configuration map (optional).
*   **`EdgeDefinition`**:
    *   `source`: Source node ID.
    *   `target`: Target node ID.
    *   `condition`: (Optional) The routing key (e.g., "true", "next") for conditional edges.

## 2. GraphParser Implementation
The `GraphParser` will be a Spring Component.

*   **Dependencies**:
    *   `ApplicationContext`: To look up beans by name.
    *   `GraphDefinitionRepository` (Interface): To fetch `NodeDefinition`/`EdgeDefinition` for sub-graphs (if recursive loading is needed).
*   **Parsing Logic (`parse` method)**:
    1.  **Iterate Nodes**:
        *   **Standard Nodes**:
            *   Look up the Bean using `node.getResource()`.
            *   If the bean implements `AsyncCommandAction`, treat it as a **Router Node**.
            *   If the bean implements `AsyncNodeAction`, treat it as a **Standard Processor Node**.
        *   **SubGraph Nodes**:
            *   Recursively load/parse the referenced graph using the repository.
            *   Add as a `SubCompiledGraphNode` or `SubStateGraphNode`.
    2.  **Iterate Edges**:
        *   **Standard Edges** (No condition): Call `stateGraph.addEdge(source, target)`.
        *   **Conditional Edges** (With condition):
            *   These imply the *source node* is a Router Node (or has an attached router).
            *   If the source node was registered as an `AsyncCommandAction`, collect all its outgoing edges into a `Map<String, String>` (Condition -> Target) and register it using `addNode(id, action, mappings)`.
            *   *Note*: This requires a 2-pass approach: First collect edges for routers, then register nodes.

## 3. Location
*   `spring-ai-alibaba-graph-core/src/main/java/com/alibaba/cloud/ai/graph/parser/`

## 4. Verification
*   Create a test with a Mock ApplicationContext.
*   Register dummy `AsyncNodeAction` and `AsyncCommandAction` beans.
*   Parse a definition that uses both.
*   Verify the `StateGraph` structure.
