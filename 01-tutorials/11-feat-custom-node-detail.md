# Spring AI Alibaba 自定义节点功能分析

本报告分析了如何使用 Spring AI Alibaba 实现“插件(Plugin)”、“MCP 集成”和“新增自定义节点”功能。

## 摘要

Spring AI Alibaba 采用标准化的 `ToolCallback` 机制来支持插件集成。对于 MCP（Model Context Protocol），通过依赖集成提供了基础支持。新增节点只需实现标准 Java 接口即可。

| 功能模块 | 功能名称 | 实现状态 | 关键组件 / 接口 | S-A-A 实现策略 |
| :--- | :--- | :--- | :--- | :--- |
| **自定义节点** | Plugin (集成API) | **内置支持** | `AgentToolNode` / `ToolCallback` | 将外部 API 封装为 Spring AI `ToolCallback`，通过 `AgentToolNode` 注册到图中。支持 `@Tool` 注解和函数式注册。 |
| | MCP 集成 | **支持** | `McpClient` / `ToolCallback` | 引入 `mcp-sdk`，创建连接到 MCP Server 的 Client，并将其暴露的 Tools 适配为 `ToolCallback`。 |
| | 新增节点 | **支持** | `NodeAction` / `NodeActionWithConfig` | 实现 `NodeAction` 接口，定义 `apply(State)` 逻辑，通过 `graph.addNode()` 注册。 |

---

## 详细实现指南

### 1. Plugin (API 集成)
**功能描述**: 集成第三方 API 服务，通过标准化配置对接外部系统。

*   **实现策略**:
    Spring AI Alibaba 复用 Spring AI 的 `Tool` 机制。
    1.  **定义工具**: 使用 `@Tool` 注解 Java 方法，或者使用 `FunctionToolCallback`。
    2.  **注册节点**: 使用 `AgentToolNode` 来执行这些工具。
*   **代码示例**:
    ```java
    // 1. 定义工具
    public class WeatherService {
        @Tool(description = "Get weather for a city")
        public String getWeather(String city) {
            return "Sunny in " + city;
        }
    }
    
    // 2. 注册为 Bean 或手动创建 Callback
    ToolCallback weatherTool = MethodToolCallback.builder()
        .toolDefinition(ToolDefinitions.builder(method).build())
        .toolObject(new WeatherService())
        .build();

    // 3. 在图中使用 AgentToolNode
    AgentToolNode toolNode = AgentToolNode.builder()
        .toolCallbacks(List.of(weatherTool))
        .build();
    graph.addNode("weather_tool", toolNode);
    ```

### 2. MCP 集成 (Model Context Protocol)
**功能描述**: 集成第三方标准化 MCP 服务。

*   **实现策略**:
    项目 `pom.xml` 已包含 `mcp-bom`。需构建适配器将 MCP Tool 转换为 Spring AI `ToolCallback`。
    *(注: 代码库中 `AgentToolNode` 有注释 `FIXME, currently MCP Tool does not support...`，说明处于早期支持阶段，可能需要手动编写适配层)*
*   **适配逻辑**:
    1.  建立 `McpClient` 连接。
    2.  调用 `client.listTools()` 获取工具列表。
    3.  为每个 MCP Tool 创建一个 `SoftwareToolCallback` 代理，内部调用 `client.callTool()`。

### 3. 新增节点 (自定义节点)
**功能描述**: 开发全新的功能节点。

*   **实现策略**:
    标准的扩展方式是实现 `NodeAction` 接口。
*   **代码示例**:
    ```java
    public class MyCustomNode implements NodeAction {
        @Override
        public Map<String, Object> apply(OverAllState state) {
            // 1. 读取输入
            String input = state.value("inputKey").toString();
            
            // 2. 执行自定义业务逻辑
            String result = executeLogic(input);
            
            // 3. 返回输出
            return Map.of("outputKey", result);
        }
    }
    
    // 注册
    graph.addNode("custom_node", node_async(new MyCustomNode()));
    ```
