# Spring AI Alibaba 错误处理功能分析

本报告分析了如何使用 Spring AI Alibaba 实现“异常忽略策略”和“超时控制”功能。

## 摘要

Spring AI Alibaba 目前没有内置开箱即用的“忽略异常”开关或“超时配置”注解，但基于其 `CompletableFuture` 的异步架构，可以通过标准的 Java 模式轻松扩展实现这些功能。

| 功能模块 | 功能名称 | 实现状态 | 关键组件 / 接口 | S-A-A 实现策略 |
| :--- | :--- | :--- | :--- | :--- |
| **错误处理** | 异常忽略策略 | **需手动实现** | `NodeAction` | 在自定义 Node 中使用 `try-catch` 捕获异常，并在 `catch` 块中返回默认值。 |
| | 超时控制 | **需手动实现** | `AsyncNodeAction` | 包装 `CompletableFuture`，使用 JDK 9+ 的 `orTimeout()` 或 `completeOnTimeout()` 方法。 |

---

## 详细实现指南

### 1. 异常忽略策略 (Exception Ignore Strategy)
**功能描述**: 通过开关启用忽略异常，异常时返回默认输出。

*   **现状**:
    框架核心 `GraphRunner` 遇到异常会终止流程并在 `onError` 回调中抛出。
*   **实现策略**:
    创建一个通用的 `SafeNode` 包装器（装饰器模式）。
*   **代码示例**:
    ```java
    public class SafeNode implements NodeAction {
        private final NodeAction delegate;
        private final Map<String, Object> fallbackOutput;
        private final boolean ignoreErrors;

        public SafeNode(NodeAction delegate, Map<String, Object> fallbackOutput, boolean ignoreErrors) {
            this.delegate = delegate;
            this.fallbackOutput = fallbackOutput;
            this.ignoreErrors = ignoreErrors;
        }

        @Override
        public Map<String, Object> apply(OverAllState state) {
            try {
                return delegate.apply(state);
            } catch (Exception e) {
                if (ignoreErrors) {
                    System.err.println("Node execution failed but ignored: " + e.getMessage());
                    return fallbackOutput; // 返回默认值，保证流程继续
                }
                throw e; // 重新抛出，中断流程
            }
        }
    }
    
    // 使用
    graph.addNode("myNode", node_async(new SafeNode(realNode, Map.of("result", "default"), true)));
    ```

### 2. 超时控制 (Timeout Control)
**功能描述**: 设置节点最大执行时间，防止流程阻塞。

*   **现状**:
    `AsyncNodeAction` 返回 `CompletableFuture`，但默认没有设置超时取消。
*   **实现策略**:
    在构建异步节点时，对返回的 `Future` 设置超时处理。
*   **代码示例**:
    ```java
    public static AsyncNodeAction node_async_timeout(NodeAction syncAction, long timeout, TimeUnit unit) {
        return state -> {
            CompletableFuture<Map<String, Object>> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return syncAction.apply(state);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            // JDK 9+ 方法
            return future.orTimeout(timeout, unit)
                   .exceptionally(ex -> {
                       if (ex instanceof TimeoutException) {
                           throw new RuntimeException("Node execution timed out!");
                       }
                       throw new RuntimeException(ex);
                   });
        };
    }
    
    // 使用
    graph.addNode("slowNode", node_async_timeout(new SlowNode(), 5, TimeUnit.SECONDS));
    ```
