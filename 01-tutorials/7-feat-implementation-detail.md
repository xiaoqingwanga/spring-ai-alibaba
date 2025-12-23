# Spring AI Alibaba 工作流功能特性分析

本报告分析了如何使用 `spring-ai-alibaba` 代码库（特别是 `spring-ai-alibaba-agent-framework` 和 `spring-ai-alibaba-graph-core` 模块）来实现特定的工作流和图与功能。

## 摘要

`spring-ai-alibaba-agent-framework` 提供了一个类似于 LangGraph 的强大的 StateGraph 引擎，支持大多数请求的功能。然而，一些“低代码”配置功能（如预置的条件操作符或声明式参数映射）并非内置，通过自定义实现模式来支持。

| 功能分类 | 功能名称 | 实现状态 | 关键组件 / 接口 |
| :--- | :--- | :--- | :--- |
| **节点配置** | 节点参数配置 | **需手动实现** | `OverAllState`, `NodeAction` |
| | 节点类型扩展 | **支持** | `NodeAction`, `NodeActionWithConfig` |
| **流程控制逻辑** | 条件分支设置 | **代码实现** | `addConditionalEdges`, `ConditionEvaluator` |
| | 并行执行控制 | **支持** | `StateGraph` 拓扑结构, `ParallelGraphBuildingStrategy` |
| | 嵌套流程调用 (新) | **支持** | `StateGraph.addNode(name, subGraph)` |
| | 嵌套已有 Workflow | **支持** | 复用现有的 `StateGraph` 实例 |

---

## 详细实现指南

### 1. 节点配置

#### 1.1 节点参数配置
**需求**: 为每个节点设置输入/输出参数。

*   **当前架构**: `spring-ai-alibaba` 中的节点是接受全局 `OverAllState` 的功能单元 (`NodeAction`)。目前没有内置的“声明式映射”引擎来自动将状态键映射到节点方法参数变量。
*   **实现模式**:
    1.  **节点内部**: 节点实现显式地从状态中读取特定的键。
        ```java
        // 示例：节点从全局状态中读取 "query"
        String input = state.value("query", "").toString();
        ```
    2.  **状态更新**: 节点返回一个 `Map<String, Object>`，该 Map 会根据 `KeyStrategy`（例如 `ReplaceStrategy` 覆盖策略, `AppendStrategy` 追加策略）合并回全局状态。
    3.  **可配置映射 (推荐)**: 为了实现可重用性，可以创建一个自定义的节点包装器，接受参数映射配置。
        ```java
        // 概念代码
        public class ConfigurableNode implements NodeAction {
            private final String inputKey;
            private final String outputKey;

            public ConfigurableNode(String inputKey, String outputKey) {
                this.inputKey = inputKey;
                this.outputKey = outputKey;
            }

            @Override
            public Map<String, Object> apply(OverAllState state) {
                // 从配置的输入键读取
                Object data = state.value(this.inputKey).orElse(null);
                // 处理逻辑...
                return Map.of(this.outputKey, result);
            }
        }
        ```

#### 1.2 节点类型扩展
**需求**: 支持自定义节点类型。

*   **实现**: 实现 `NodeAction` 或 `NodeActionWithConfig` 接口。
*   **代码参考**: `WorkflowExample.java` (第 106-120 行)。
    ```java
    public class MyCustomNode implements NodeAction {
        @Override
        public Map<String, Object> apply(OverAllState state) throws Exception {
            // 自定义逻辑
            return Map.of("result", "done");
        }
    }
    ```
*   **集成**: 通过 `graph.addNode("myNode", node_async(new MyCustomNode()));` 添加到图中。

---

### 2. 流程控制逻辑

#### 2.1 条件分支设置
**需求**: 配置节点执行条件（空值判断、值相等判断、字符串前缀/后缀、子串包含判断等）。

*   **当前架构**: 条件是返回下一个节点 ID 的函数 (`Function<OverAllState, String>`)。
*   **实现**: 使用 `StateGraph.addConditionalEdges`。
*   **缺少内置库**: 代码库提供了基础模板 `ConditionEvaluator`，但没有“操作符对象”库（如 `Operators.equals()`）。你需要用 Java 代码编写逻辑。
*   **代码参考**: `WorkflowExample.java` (第 292-300 行)。
    ```java
    graph.addConditionalEdges(
        "sourceNode",
        edge_async(state -> {
            String val = state.value("key").toString();
            // 在此实现操作符逻辑：
            if (val == null) return "routeA"; // 空值判断
            if (val.startsWith("prefix")) return "routeB"; // 前缀判断
            return "default";
        }),
        Map.of("routeA", "TargetNodeA", "routeB", "TargetNodeB", "default", END)
    );
    ```

#### 2.2 并行执行控制
**需求**: 通过开关启用/禁用节点并行处理。

*   **实现**:
    1.  **基于拓扑 (隐式)**: 如果一个节点有多个指向不同节点的出边，这些目标节点将并行执行。
    2.  **基于策略 (显式)**: 使用带有 `ParallelGraphBuildingStrategy` 的 `FlowGraphBuilder`。
        *   此策略自动构建“扇出-扇入” (Fan-Out, Fan-In) 图。
        *   它使用 `EnhancedParallelResultAggregator` 等待所有分支完成。
    3.  **控制**: 要“禁用”并行处理，必须将图结构更改为顺序执行（链式节点 A -> B，而不是 Root -> A, Root -> B）。

#### 2.3 嵌套流程调用 & 嵌套已有 Workflow
**需求**: 复用现有 Workflow 作为子流程。

*   **实现**: `StateGraph` 支持通过 `addNode` 将另一个 `StateGraph`（或 `CompiledGraph`）作为节点添加。
*   **代码参考**: `SubgraphAsStateGraphExample.java` 和 `CompiledGraph.java` (SubStateGraphNodes 逻辑)。
    ```java
    // 1. 定义子工作流
    StateGraph subGraph = new StateGraph(...);

    // 2. 定义父工作流
    StateGraph parentGraph = new StateGraph(...);

    // 3. 将子工作流作为节点添加
    parentGraph.addNode("subProcess", subGraph); // 嵌套

    // 4. 连接
    parentGraph.addEdge("start", "subProcess");
    ```
*   **状态隔离**: 默认情况下，子图共享父图的 `OverAllState`，除非进行包装（例如使用示例中看到的 `IsolatedSubGraphNode` 模式）以重新映射键。
