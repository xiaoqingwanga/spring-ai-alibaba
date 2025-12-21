# Python Code Execution Node

The `StateGraph` framework supports executing Python code directly as a node in your graph. This is achieved using the `CodeExecutorNodeAction` which leverages a `CodeExecutor` (either Docker-based or Local) to run the script.

## 1. Prerequisites

Ensure you have the builtin nodes starter dependency:
```xml
<dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>spring-ai-alibaba-starter-builtin-nodes</artifactId>
</dependency>
```

## 2. Choosing an Executor

You need to provide a `CodeExecutor` implementation.

### Option A: Docker (Recommended)
Runs code in an isolated container. Requires Docker to be running on the host.

```java
CodeExecutionConfig config = new CodeExecutionConfig();
config.setDockerHost("unix:///var/run/docker.sock"); // or tcp://localhost:2375
config.setImage("python:3.9-slim");
config.setWorkDir("/tmp/ai-graph/exec");

CodeExecutor executor = new DockerCodeExecutor();
```

### Option B: Local Command Line
Runs code directly on the host machine. Requires `python3` to be in the system PATH.

```java
CodeExecutionConfig config = new CodeExecutionConfig();
config.setWorkDir("/tmp/ai-graph/exec");

CodeExecutor executor = new LocalCommandlineCodeExecutor();
```

## 3. Writing the Python Code

The builtin `Python3TemplateTransformer` expects your code to define a `main` function.
- **Input**: The function should accept arguments matching your declared parameters.
- **Output**: The function should return a JSON-serializable object (dict, list, str, int, etc.).

**Example Code:**
```python
def main(a, b):
    return {
        "sum": a + b,
        "product": a * b
    }
```

## 4. Creating the Node

Use `CodeExecutorNodeAction.builder()` to create the node action.

```java
// Define parameters (mapping state keys to function arguments)
List<CodeParam> params = List.of(
    new CodeParam("a", "input_a"), // maps state["input_a"] to python arg "a"
    new CodeParam("b", "input_b")  // maps state["input_b"] to python arg "b"
);

CodeExecutorNodeAction pythonNode = CodeExecutorNodeAction.builder()
    .codeExecutor(executor)
    .config(config)
    .codeLanguage("python3")
    .code("""
        def main(a, b):
            return {
                "sum": a + b,
                "product": a * b
            }
        """)
    .params(params)
    .outputKey("math_result") // The result will be stored in state["math_result"]
    .build();
```

## 5. Adding to Graph

Add it to your `StateGraph` like any other node.

```java
StateGraph graph = new StateGraph();
graph.addNode("python_step", pythonNode);
// ... edges ...
```
