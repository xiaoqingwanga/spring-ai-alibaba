# Dynamic Graph Construction from Database

This tutorial demonstrates how to build a `StateGraph` dynamically based on configuration stored in a database. This pattern is useful when you want to allow users or administrators to define workflows at runtime without redeploying code.

## Concept

Instead of hardcoding the `StateGraph` structure in your Java code, you fetch node and edge definitions from a repository and construct the graph programmatically.

## Example Implementation

```java
@Service
public class DatabaseGraphLoader {

    // Registry of available actions (Bean Name -> NodeAction)
    private final Map<String, NodeAction> actionRegistry;

    public DatabaseGraphLoader(Map<String, NodeAction> actionRegistry) {
        this.actionRegistry = actionRegistry;
    }

    public CompiledGraph loadGraph(String workflowId) {
        // 1. Fetch config from DB
        List<NodeEntity> dbNodes = nodeRepository.findByWorkflowId(workflowId);
        List<EdgeEntity> dbEdges = edgeRepository.findByWorkflowId(workflowId);

        // 2. Initialize Graph
        StateGraph graph = new StateGraph(defaultKeyStrategy());

        // 3. Add Nodes dynamically
        for (NodeEntity node : dbNodes) {
            NodeAction action = actionRegistry.get(node.getActionType());
            if (action == null) throw new RuntimeException("Unknown action: " + node.getActionType());
            
            // Optional: Configure action with node.getConfig() JSON
            
            graph.addNode(node.getNodeId(), node_async(action));
        }

        // 4. Add Edges dynamically
        for (EdgeEntity edge : dbEdges) {
            if (edge.getCondition() == null) {
                // Simple Edge
                graph.addEdge(edge.getSourceNode(), edge.getTargetNode());
            } else {
                // Conditional Edge
                // You would need a registry for Condition functions too!
                graph.addConditionalEdges(
                    edge.getSourceNode(),
                    conditionRegistry.get(edge.getCondition()), 
                    Map.of("true", edge.getTargetNode(), "false", "END") // Simplified
                );
            }
        }

        // 5. Compile
        return graph.compile();
    }
}
```