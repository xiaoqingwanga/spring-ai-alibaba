# Launching Spring AI Alibaba Studio Application

This tutorial covers how to launch the Spring AI Alibaba Studio application.

## Prerequisites

1. Java 17 or higher installed
2. Maven 3.6+ installed
3. Valid API keys:
   - **DashScope API Key** - For AI model access
   - **Jina API Key** - For Model Context Protocol (MCP) services

## Getting API Keys

### DashScope API Key
1. Visit [Alibaba Cloud DashScope](https://dashscope.aliyun.com/)
2. Sign up and create an API key
3. Keep it secure as `AI_DASHSCOPE_API_KEY`

### Jina API Key
1. Visit [Jina AI](https://jina.ai/)
2. Sign up and obtain an API key
3. Use it as `JINA_API_KEY`

## Launch Methods

### Method 1: Using Maven Spring Boot Plugin (Recommended)

From the project root directory:
```bash
./mvnw spring-boot:run -pl spring-ai-alibaba-studio -Dspring-boot.run.main-class=com.alibaba.cloud.ai.graph.StudioApplication
```

Or from the studio directory:
```bash
cd spring-ai-alibaba-studio
mvn spring-boot:run -Dspring-boot.run.main-class=com.alibaba.cloud.ai.graph.StudioApplication
```

### Method 2: Using Java Directly

First, compile the project:
```bash
./mvnw clean install -DskipTests
```

Then run with Java:
```bash
cd spring-ai-alibaba-studio
java -cp "target/classes:target/test-classes:$(mvn dependency:build-classpath -q -Dmdep.outputFile=/dev/stdout)" com.alibaba.cloud.ai.graph.StudioApplication
```

## Setting Environment Variables

### Option A: Export environment variables
```bash
export AI_DASHSCOPE_API_KEY=your_dashscope_api_key
export JINA_API_KEY=your_jina_api_key
```

### Option B: Inline with the command
```bash
AI_DASHSCOPE_API_KEY=your_dashscope_key JINA_API_KEY=your_jina_key ./mvnw spring-boot:run -pl spring-ai-alibaba-studio -Dspring-boot.run.main-class=com.alibaba.cloud.ai.graph.StudioApplication
```

### Option C: Create a .env file (for development)
Create a `.env` file in the project root:
```
AI_DASHSCOPE_API_KEY=your_dashscope_api_key
JINA_API_KEY=your_jina_api_key
```

Then source it before running:
```bash
source .env
./mvnw spring-boot:run -pl spring-ai-alibaba-studio -Dspring-boot.run.main-class=com.alibaba.cloud.ai.graph.StudioApplication
```

## Application Configuration

### Default Configuration
- **Server Port**: 8080
- **Application Name**: spring-ai-alibaba-studio
- **MCP Server**: https://mcp.jina.ai (automatically appends /sse)

### Custom Configuration
You can override configuration by creating `spring-ai-alibaba-studio/src/test/resources/application-custom.yml`:

```yaml
spring:
  application:
    name: spring-ai-alibaba-studio
  ai:
    dashscope:
      api-key: ${AI_DASHSCOPE_API_KEY}
    mcp:
      client:
        enabled: true
        request-timeout: 60000

server:
  port: 8080

logging:
  level:
    root: INFO
    com.alibaba.cloud.ai: DEBUG
```

Then run with the custom profile:
```bash
./mvnw spring-boot:run -pl spring-ai-alibaba-studio -Dspring-boot.run.main-class=com.alibaba.cloud.ai.graph.StudioApplication -Dspring.profiles.active=custom
```

## Common Issues and Solutions

### 1. Compilation Errors
If you encounter compilation errors about missing `getToolsAutomaticallyApproved()` method:
```bash
./mvnw clean install -DskipTests
```
This ensures all modules are built with the latest code.

### 2. MCP Connection Timeout
Error: `Client failed to initialize by explicit API call`

**Solution**: Ensure you have a valid `JINA_API_KEY` environment variable set.

### 3. Port Already in Use
If port 8080 is already in use:
```yaml
# In your application.yml
server:
  port: 8081  # or any other available port
```

### 4. Main Class Not Found
Error: `Could not find or load main class com.alibaba.cloud.ai.graph.StudioApplication`

**Solution**:
- Ensure you're running from the correct directory
- The StudioApplication is in `src/test/java`, not `src/main/java`
- Compile with `mvn test-compile` first

## Accessing the Application

Once successfully launched, the application will be available at:
- **Main Application**: http://localhost:8080
- **API Documentation** (if enabled): http://localhost:8080/v3/api-docs.yaml

## Development Tips

### Running with Debug Logging
```bash
./mvnw spring-boot:run -pl spring-ai-alibaba-studio -Dspring-boot.run.main-class=com.alibaba.cloud.ai.graph.StudioApplication -Dspring.profiles.active=debug
```

### Hot Reload during Development
Add Spring Boot DevTools to `pom.xml` for automatic restarts:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <optional>true</optional>
</dependency>
```

## Summary

To successfully launch the Studio application:
1. Obtain required API keys (DashScope and Jina)
2. Set environment variables
3. Use Maven Spring Boot plugin or Java directly to run
4. Ensure all dependencies are compiled
5. Access at http://localhost:8080

The application provides a web interface for interacting with AI agents and tools through the Spring AI Alibaba framework.