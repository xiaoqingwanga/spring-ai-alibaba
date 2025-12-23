# Spring AI Alibaba 调试与监控功能分析

本报告分析了如何使用 Spring AI Alibaba 实现单节点调试、执行追踪和测试运行功能。

## 摘要

Spring AI Alibaba 提供了基于 Micrometer Observation 的强大追踪能力，支持全链路监控。对于单节点调试和测试运行，可以利用现有的 API 进行灵活的单元测试和集成测试。

| 功能模块 | 功能名称 | 实现状态 | 关键组件 / 接口 | S-A-A 实现策略 |
| :--- | :--- | :--- | :--- | :--- |
| **调试与监控** | 单节点调试 | **支持** | `NodeAction.apply(State)` | 独立实例化 Node 类，构造 Mock State 直接调用 `apply` 方法。 |
| | 执行追踪 | **支持** | `GraphObservationLifecycleListener` | 集成 Micrometer Tracing/Observation，自动记录节点耗时、输入输出状态（High Cardinality Tags）。 |
| | 测试运行 | **支持** | `CompiledGraph.invoke()` | 使用 `CompiledGraph` 在测试环境运行完整流程，或使用 Mock Agent 进行隔离测试。 |

---

## 详细实现指南

### 1. 单节点调试 (Single Node Debugging)
**功能描述**: 提供调试，实时运行当前节点并返回结果。

*   **实现策略**:
    `NodeAction` 是一个函数接口，不依赖于整个图的上下文（除了 `OverAllState`）。因此，可以独立于图运行单个节点。
*   **代码示例**:
    ```java
    // 1. 实例化待调试的节点
    NodeAction node = new MyCustomNode();

    // 2. 构造模拟状态 (Mock State)
    Map<String, Object> inputData = Map.of("query", "test input");
    OverAllState mockState = new OverAllState(inputData, Map.of());

    // 3. 直接调用 apply
    Map<String, Object> result = node.apply(mockState);

    // 4. 验证结果
    System.out.println("Node Result: " + result);
    ```

### 2. 执行追踪 (Execution Tracing)
**功能描述**: 展示流程执行详情，包括各节点耗时、输入/输出参数、执行状态、成本估算等。

*   **实现策略**:
    Spring AI Alibaba 集成了 Spring Boot 的 Observation 机制 (`spring-ai-alibaba-starter-graph-observation`)。
*   **核心组件**: `GraphObservationLifecycleListener`
    *   自动监听图的 `onStart`, `before`, `after`, `onError`, `onComplete` 事件。
    *   **耗时**: Micrometer Timer 自动记录。
    *   **输入/输出**: 记录为 High Cardinality KeyValues (`node.before.state`, `node.after.state`)。
    *   **集成**: 引入 Starter 后自动生效。
    ```xml
    <dependency>
        <groupId>com.alibaba.cloud.ai</groupId>
        <artifactId>spring-ai-alibaba-starter-graph-observation</artifactId>
    </dependency>
    ```

### 3. 测试运行 (Test Run)
**功能描述**: 提供独立测试环境，支持模拟输入并查看完整流程的执行结果与耗时。

*   **实现策略**:
    利用 `CompiledGraph` 的 `invoke` 或 `stream` 方法进行完整流程测试。
*   **代码示例**:
    ```java
    @Test
    public void testFullWorkflow() {
        // 1. 构建图
        StateGraph graph = new StateGraph(...);
        // ... 添加节点和边

        // 2. 编译
        CompiledGraph compiled = graph.compile();

        // 3. 模拟输入
        Map<String, Object> inputs = Map.of("input", "simulation data");

        // 4. 执行并断言
        Optional<OverAllState> result = compiled.invoke(inputs);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals("expected", result.get().value("output").orElse(null));
    }
    ```
