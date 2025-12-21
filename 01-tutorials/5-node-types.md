# Node Types in StateGraph

Based on the `StateGraph.java` and internal node implementations in `spring-ai-alibaba-graph-core`, the `StateGraph` supports the following types of nodes:

## 1. Standard Nodes (`Node`)

The most basic unit of processing.

- **Purpose**: Executes a specific function or action (`AsyncNodeAction`).
- **Usage**: Added via `addNode(String id, AsyncNodeAction action)`.
- **Behavior**: It takes the current state, performs an operation, and returns a partial state update.

## 2. SubGraph Nodes

These allow you to nest graphs within graphs, enabling composition and modularity.

### Compiled SubGraph (`SubCompiledGraphNode`)
- **Usage**: Added via `addNode(String id, CompiledGraph subGraph)`.
- **Behavior**: Executes a pre-compiled graph as a single step in the parent graph.

### StateGraph SubGraph (`SubStateGraphNode`)
- **Usage**: Added via `addNode(String id, StateGraph subGraph)`.
- **Behavior**: Executes a raw `StateGraph` (which is compiled at runtime or managed by the parent) as a step.

## 3. Conditional "Nodes" (Logic via Edges)

While not strictly a "node" type in terms of data structure, the API treats conditional logic as a routing step.

- **Usage**: `addConditionalEdges(String sourceId, AsyncCommandAction condition, Map<String, String> mappings)`.
- **Behavior**: Determines the next node to transition to based on the current state (e.g., routing to "END" or another processing node).

## Internal / Advanced

- **ParallelNode**: Found in `com.alibaba.cloud.ai.graph.internal.node`, suggesting support for parallel execution branches, likely handled during the compilation or execution phase when multiple nodes are eligible to run simultaneously.

## Special Constants

The graph also relies on special node identifiers:

- `START`: The entry point of the graph.
- `END`: The terminal point of the graph.
- `ERROR`: Handling error states.