# Building an End-to-End Chatbot Agent

This guide walks you through building a complete, persistent Chatbot Agent using Spring AI Alibaba. We will create a "ReAct" (Reason-Act) agent capable of executing shell commands, running Python code, and reading files to solve complex tasks.

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- An OpenAI-compatible API Key (e.g., Alibaba Cloud DashScope)

## 1. Project Setup

Create a new Spring Boot project and add the necessary dependencies in your `pom.xml`.

```xml
<dependencies>
    <!-- Core Spring AI Alibaba Starter -->
    <dependency>
        <groupId>com.alibaba.cloud.ai</groupId>
        <artifactId>spring-ai-alibaba-agent-framework</artifactId>
        <version>${spring-ai-alibaba.version}</version>
    </dependency>

    <!-- Built-in Nodes (for Python execution, etc.) -->
    <dependency>
        <groupId>com.alibaba.cloud.ai</groupId>
        <artifactId>spring-ai-alibaba-starter-builtin-nodes</artifactId>
        <version>${spring-ai-alibaba.version}</version>
    </dependency>

    <!-- Spring Boot Starter Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
</dependencies>
```

## 2. Application Entry Point

Create the main Spring Boot application class `ChatbotApplication.java`.

```java
package com.example.chatbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ChatbotApplication {
    public static void main(String[] args) {
        SpringApplication.run(ChatbotApplication.class, args);
    }
}
```

## 3. Configuring the Agent

Define the agent configuration in `ChatbotAgent.java`. This is where we assemble the `ReactAgent` with its tools and memory.

```java
package com.example.chatbot;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.shelltool.ShellToolAgentHook;
import com.alibaba.cloud.ai.graph.agent.tools.ShellTool;
import com.alibaba.cloud.ai.graph.agent.extension.tools.filesystem.ReadFileTool;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.util.List;

@Configuration
public class ChatbotAgent {

    private static final String INSTRUCTION = """
            You are a helpful assistant named SAA.
            You have access to tools that can help you execute shell commands and view text files.
            Use these tools to assist users with their tasks.
            """;

    @Bean
    public ReactAgent chatbotReactAgent(ChatModel chatModel,
                                        ToolCallback executeShellCommand,
                                        ToolCallback viewTextFile,
                                        MemorySaver memorySaver) {
        return ReactAgent.builder()
                .name("SAA")
                .model(chatModel)
                .instruction(INSTRUCTION)
                .enableLogging(true)
                .saver(memorySaver)
                // Hook required for ShellTool lifecycle management
                .hooks(ShellToolAgentHook.builder()
                        .shellToolName(executeShellCommand.getToolDefinition().name())
                        .build())
                .tools(executeShellCommand, viewTextFile)
                .build();
    }

    @Bean
    public MemorySaver memorySaver() {
        return new MemorySaver();
    }

    // --- Tool Definitions ---

    @Bean
    public ToolCallback executeShellCommand() {
        String workspaceRoot = System.getProperty("java.io.tmpdir") + File.separator + "agent-workspace";
        return ShellTool.builder(workspaceRoot)
                .withName("execute_shell_command")
                .withDescription("Execute shell commands. Use absolute paths.")
                .build();
    }

    @Bean
    public ToolCallback viewTextFile() {
        return FunctionToolCallback.builder("view_text_file", new ReadFileTool())
                .description("View the contents of a text file.")
                .inputType(ReadFileTool.ReadFileRequest.class)
                .build();
    }
}
```

### Key Components Explained

1.  **`ReactAgent`**: Implements the "Reason-Act" pattern. It loops through:
    -   **Thought**: The LLM decides what to do based on the input and available tools.
    -   **Action**: It calls a tool (if needed).
    -   **Observation**: It sees the output of the tool.
    -   **Repeat**: It continues until it can answer the user's request.

2.  **`MemorySaver`**: Persists the conversation state (short-term memory) so the agent remembers previous turns in the conversation.

3.  **Tools**:
    -   `ShellTool`: Allows the agent to run system commands (sandboxed to a workspace).
    -   `ReadFileTool`: Allows the agent to read file contents.

## 4. Running the Application

Set your API key as an environment variable:

```bash
export AI_DASHSCOPE_API_KEY=sk-your-api-key
```

Run the application:

```bash
mvn spring-boot:run
```

The application will start the agent. Spring AI Alibaba Studio (if enabled or deployed) can connect to this agent, or you can interact with it via its exposed REST endpoints or a built-in UI if you've included the necessary UI components.

## 5. What's Next?

-   **Add Python Support**: Incorporate the `PythonTool` (as seen in `spring-boot-starters/spring-ai-alibaba-starter-builtin-nodes`) to allow your agent to run data analysis scripts.
-   **Persistent Memory**: Switch from `MemorySaver` (in-memory) to a Redis-based saver for production persistence.
-   **Custom Tools**: Write your own Java functions and expose them as `ToolCallback` beans.
