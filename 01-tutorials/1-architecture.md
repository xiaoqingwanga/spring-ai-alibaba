# Spring AI Alibaba - Complete Architecture Guide

This guide provides a comprehensive overview of the Spring AI Alibaba architecture, including detailed PlantUML visualizations.

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [High-Level Architecture](#high-level-architecture)
3. [Module Dependencies](#module-dependencies)
4. [Agent Framework Classes](#agent-framework-classes)
5. [Execution Flow](#execution-flow)
6. [Graph Structure](#graph-structure)

## Architecture Overview

Spring AI Alibaba is a comprehensive framework for building AI-powered applications using Spring Boot. It integrates Alibaba's DashScope AI services and provides a declarative, configuration-driven approach to creating intelligent agents, workflows, and conversational applications.

### Core Modules

1. **spring-ai-alibaba-graph-core**: The foundational graph-based execution engine
2. **spring-ai-alibaba-agent-framework**: High-level agent abstractions and patterns
3. **spring-ai-alibaba-studio**: Web-based development and management interface
4. **Spring Boot Starters**: Convenient auto-configuration modules

## High-Level Architecture

```plantuml
@startuml Spring AI Alibaba - High Level Architecture
!theme plain
skinparam monochrome true
skinparam shadowing false
skinparam defaultFontName Arial
skinparam rectangle {
    BorderColor #333
    BackgroundColor #f9f9f9
}
skinparam package {
    BorderColor #333
    BackgroundColor #e6e6e6
    FontColor #333
}

package "Spring AI Alibaba" {

    rectangle "Studio Module\n(Web UI & REST API)" as studio
    rectangle "Agent Framework\n(High-level Abstractions)" as agent
    rectangle "Graph Core\n(Execution Engine)" as core

    studio --> agent
    agent --> core
}

package "External Integrations" {
    rectangle "DashScope SDK\n(Alibaba AI Services)" as dashscope
    rectangle "Spring AI\n(Model Abstraction)" as springai
    rectangle "Nacos\n(Service Discovery & Config)" as nacos
    rectangle "Redis\n(Caching & Checkpoints)" as redis
    rectangle "A2A SDK\n(Agent Communication)" as a2a
}

package "Spring Boot Ecosystem" {
    rectangle "Spring Boot\n(Application Framework)" as boot
    rectangle "Spring WebFlux\n(Reactive Web)" as webflux
    rectangle "Spring Configuration\n(Properties & @Configuration)" as config
}

core --> dashscope
core --> springai
agent --> nacos
agent --> redis
studio --> a2a

studio --> boot
agent --> boot
core --> boot

studio --> webflux
agent --> config
core --> config

note right of studio
  • REST Controllers
  • Agent Management UI
  • Execution Control
  • Thread/Conversation Mgmt
end note

note right of agent
  • ReactAgent
  • Flow Agents
  • Tool Framework
  • Filesystem Extensions
  • Interceptors
end note

note right of core
  • StateGraph
  • CompiledGraph
  • Node System
  • State Management
  • Checkpointing
end note

@enduml
```

**Key Points:**
- Layered architecture with clear separation of concerns
- Studio provides web interface for agent management
- Agent Framework builds on top of Graph Core
- Deep integration with Spring Boot ecosystem
- Extensive external integrations for AI, storage, and communication

## Module Dependencies

```plantuml
@startuml Spring AI Alibaba - Module Dependencies
!theme plain
skinparam monochrome true
skinparam shadowing false
skinparam defaultFontName Arial

package "Main Modules" {
  [spring-ai-alibaba-bom] as bom
  [spring-ai-alibaba-graph-core] as graphcore
  [spring-ai-alibaba-agent-framework] as agentfw
  [spring-ai-alibaba-studio] as studio
}

package "Spring Boot Starters" {
  [starter-a2a-nacos] as a2astarter
  [starter-config-nacos] as configstarter
  [starter-graph-observation] as obsstarter
  [starter-builtin-nodes] as nodesstarter
}

package "External Dependencies" {
  [Spring Boot 3.4.8] as springboot
  [Spring AI 1.1.0] as springai
  [DashScope SDK] as dashscope
  [Nacos 3.1.0] as nacos
  [Redis/Redisson] as redis
  [A2A SDK] as a2asdk
  [MCP SDK] as mcp
}

bom --> graphcore : manages
bom --> agentfw : manages
bom --> studio : manages
bom --> a2astarter : manages
bom --> configstarter : manages
bom --> obsstarter : manages
bom --> nodesstarter : manages

graphcore --> springboot : depends on
graphcore --> springai : depends on
graphcore --> dashscope : uses
graphcore --> redis : uses

agentfw --> graphcore : builds on
agentfw --> dashscope : uses
agentfw --> a2asdk : uses
agentfw --> mcp : supports

studio --> agentfw : uses
studio --> graphcore : uses
studio --> springboot : depends on

a2astarter --> nacos : uses
a2astarter --> a2asdk : uses
configstarter --> nacos : uses
obsstarter --> springboot : extends
nodesstarter --> graphcore : extends

note right of bom
  Bill of Materials
  Manages all versions
end note

note right of graphcore
  • State management
  • Graph execution
  • Checkpointing
  • Node framework
end note

note right of agentfw
  • Agent abstractions
  • Tool framework
  • Flow control
  • Extensions
end note

note right of studio
  • REST APIs
  • Web UI
  • Agent runtime
end note

@enduml
```

**Module Relationships:**
- BOM centrally manages all module versions
- Graph Core is the foundational layer
- Agent Framework builds upon Graph Core
- Studio uses both Core and Agent Framework
- Starters provide convenient Spring Boot integration

## Agent Framework Classes

```plantuml
@startuml Spring AI Alibaba - Agent Framework Classes
!theme plain
skinparam monochrome true
skinparam shadowing false
skinparam defaultFontName Arial

package "Core Agent Classes" {
    abstract class Agent {
        -String name
        -List<AgentTool> tools
        -ChatModel chatModel
        -Memory memory
        -List<Hook> hooks
        +invoke(String input): Flux<Message>
        +stream(String input): Flux<Message>
        #execute(): NodeOutput
    }

    abstract class BaseAgent {
        -Builder builder
        +Builder builder()
    }

    class ReactAgent {
        +ReactAgent(Builder builder)
        +static Builder builder(): Builder
    }

    Agent <|-- BaseAgent
    BaseAgent <|-- ReactAgent

    interface Builder {
        +name(String): Builder
        +tools(List<Tool>): Builder
        +chatModel(ChatModel): Builder
        +memory(Memory): Builder
        +hooks(List<Hook>): Builder
        +build(): Agent
    }

    Agent ..> Builder : uses
}

package "Flow Agents" {
    abstract class FlowAgent {
        -List<Agent> agents
        -FlowStrategy strategy
        +execute(): NodeOutput
    }

    class SequentialAgent {
        +execute(): NodeOutput
    }

    class ParallelAgent {
        +execute(): NodeOutput
    }

    class LoopAgent {
        -LoopStrategy loopStrategy
        +execute(): NodeOutput
    }

    class SupervisorAgent {
        -Map<String, Agent> agentMap
        -RouterFunction router
        +execute(): NodeOutput
    }

    class LlmRoutingAgent {
        -RoutingPromptTemplate promptTemplate
        +execute(): NodeOutput
    }

    FlowAgent <|-- SequentialAgent
    FlowAgent <|-- ParallelAgent
    FlowAgent <|-- LoopAgent
    FlowAgent <|-- SupervisorAgent
    FlowAgent <|-- LlmRoutingAgent
}

package "Agent Tools" {
    interface Tool {
        +String getName()
        +String getDescription()
        +Object apply(Object input)
    }

    class AgentTool {
        -String name
        -String description
        -Function function
        +execute(Object input): Object
    }

    Tool <|.. AgentTool
    Agent --> Tool : has many
}

package "Extensions & Interceptors" {
    interface Interceptor {
        +intercept(Context context): Result
    }

    class FilesystemInterceptor {
        -FilesystemBackend backend
        +intercept(Context): Result
    }

    class LargeResultEvictionInterceptor {
        -int maxSize
        +intercept(Context): Result
    }

    class PatchToolCallsInterceptor {
        -Map<String, Object> patches
        +intercept(Context): Result
    }

    class SubAgentInterceptor {
        -SubAgentSpec spec
        +intercept(Context): Result
    }

    Interceptor <|.. FilesystemInterceptor
    Interceptor <|.. LargeResultEvictionInterceptor
    Interceptor <|.. PatchToolCallsInterceptor
    Interceptor <|.. SubAgentInterceptor

    Agent --> Interceptor : has many
}

package "Filesystem Tools" {
    class ReadFileTool {
        +apply(ReadRequest): String
    }

    class WriteFileTool {
        +apply(WriteRequest): WriteResult
    }

    class EditFileTool {
        +apply(EditRequest): EditResult
    }

    class GrepTool {
        +apply(GrepRequest): List<GrepMatch>
    }

    class GlobTool {
        +apply(GlobRequest): List<String>
    }

    AgentTool <|-- ReadFileTool
    AgentTool <|-- WriteFileTool
    AgentTool <|-- EditFileTool
    AgentTool <|-- GrepTool
    AgentTool <|-- GlobTool
}

package "A2A Support" {
    class A2aRemoteAgent {
        -String agentId
        -AgentCardProvider cardProvider
        +execute(): NodeOutput
    }

    interface AgentCardProvider {
        +getAgentCard(String id): AgentCard
    }

    class RemoteAgentCardProvider {
        -A2AClient client
        +getAgentCard(String id): AgentCard
    }

    Agent <|-- A2aRemoteAgent
    AgentCardProvider <|.. RemoteAgentCardProvider
    A2aRemoteAgent --> AgentCardProvider : uses
}

note right of ReactAgent
  Implements ReAct pattern:
  Reason -> Act -> Observe
end note

note right of FlowAgent
  Orchestrates multiple
  agents in various patterns
end note

note right of Interceptor
  Cross-cutting concerns
  applied to agent execution
end note

@enduml
```

**Class Hierarchy Highlights:**
- Agent is the base abstract class for all agents
- ReactAgent implements the ReAct (Reason-Act-Observe) pattern
- Flow Agents provide orchestration patterns (Sequential, Parallel, Loop, etc.)
- Tools extend agent capabilities (Filesystem, API calls, etc.)
- Interceptors handle cross-cutting concerns

## Execution Flow

```plantuml
@startuml Spring AI Alibaba - Execution Flow
!theme plain
skinparam monochrome true
skinparam shadowing false
skinparam defaultFontName Arial

participant "Client" as client
participant "Studio\nController" as controller
participant "Agent\nLoader" as loader
participant "Agent" as agent
participant "Graph\nExecutor" as executor
participant "Nodes" as nodes
participant "State\nManager" as state
participant "External\nServices" as external
participant "Response\nHandler" as response

== Agent Definition Phase ==

client -> controller: POST /agents/define
note right: Agent definition in YAML/JSON
controller -> loader: loadAgent(config)
loader -> loader: parse configuration
loader -> agent: createAgent(builder)
agent -> agent: initialize tools & hooks
loader --> controller: Agent instance
controller --> client: Agent ID

== Execution Phase ==

client -> controller: POST /agents/{id}/run
controller -> loader: getAgent(id)
loader --> controller: Agent instance
controller -> agent: invoke(input)

group Agent Execution
    agent -> agent: prepare context
    agent -> executor: createGraph()
    executor -> executor: compile()

    loop Graph Execution
        executor -> nodes: execute(node)

        group Node Execution
            nodes -> state: getCurrentState()
            state --> nodes: State snapshot

            alt LLM Node
                nodes -> external: callDashScope()
                external --> nodes: LLM response
            else Tool Node
                nodes -> external: invokeTool()
                external --> nodes: Tool result
            else Condition Node
                nodes -> nodes: evaluateCondition()
            else Subgraph Node
                nodes -> executor: executeSubgraph()
                executor --> nodes: Subgraph result
            end

            nodes -> state: updateState()
            nodes --> executor: NodeOutput
        end

        executor -> executor: determineNextEdge()

        alt Checkpoint
            executor -> state: saveCheckpoint()
        end

        alt Streaming Response
            executor -> response: sendChunk()
            response --> client: SSE/WebSocket
        end
    end

    executor --> agent: FinalState
end

agent --> controller: Execution result
controller --> client: Final response (or continue streaming)

== State Persistence ==

state -> state: saveToRedis()
note right: For resume functionality

== Error Handling ==

alt Error During Execution
    nodes -> executor: Exception
    executor -> state: rollbackToCheckpoint()
    executor --> agent: ErrorResult
    agent --> controller: Error response
    controller --> client: Error details
end

== Human-in-the-Loop ==

group HIL Scenario
    nodes -> executor: Requires human input
    executor -> state: pauseExecution()
    state --> controller: Paused state
    controller --> client: Prompt for input

    client -> controller: POST /resume
    controller -> state: resumeWithInput()
    state -> executor: continueExecution()
end

note over client, external
    The entire execution is stateful
    and can be resumed from any
    checkpoint point
end note

@enduml
```

**Execution Characteristics:**
- Stateful execution with checkpointing
- Streaming responses for real-time interaction
- Error recovery with rollback capabilities
- Human-in-the-loop support
- Resumable execution from any checkpoint

## Graph Structure

```plantuml
@startuml Spring AI Alibaba - Graph Structure
!theme plain
skinparam monochrome true
skinparam shadowing false
skinparam defaultFontName Arial

rectangle "Input State" as input

rectangle "START" as start

rectangle "Router Node\n(Conditional)" as router

rectangle "Agent A\n(ReAct Pattern)" as agentA {
    rectangle "Think\n(LLM Call)" as a1_think
    rectangle "Act\n(Tool Call)" as a1_act
    rectangle "Observe\n(Result)" as a1_obs
}

rectangle "Agent B\n(Web Search)" as agentB {
    rectangle "Search\n(Tool)" as b_search
    rectangle "Process\n(LLM)" as b_process
}

rectangle "Parallel Nodes" as parallel {
    rectangle "Node P1\n(API Call)" as p1
    rectangle "Node P2\n(Database)" as p2
    rectangle "Node P3\n(LLM)" as p3
}

rectangle "Loop Node" as loop {
    rectangle "Initialize" as l_init
    rectangle "Process" as l_process
    rectangle "Check\nCondition" as l_check
}

rectangle "Subgraph\n(Checkpoint)" as subgraph

rectangle "Aggregator\n(Combine Results)" as aggregator

rectangle "Output Node\n(Transform)" as output

rectangle "END" as end

input --> start
start --> router

router --> agentA : route: "analysis"
router --> agentB : route: "search"
router --> parallel : route: "parallel"
router --> loop : route: "iterate"

a1_think --> a1_act
a1_act --> a1_obs
a1_obs --> aggregator

b_search --> b_process
b_process --> aggregator

parallel --> aggregator : join all

l_init --> l_process
l_process --> l_check
l_check --> l_process : continue
l_check --> aggregator : break

subgraph --> aggregator

aggregator --> output
output --> end

note right of router
  Uses LLM or rules
  to determine path
end note

note right of agentA
  ReAct Pattern:
  1. Think about problem
  2. Act with tools
  3. Observe results
  4. Repeat as needed
end note

note right of parallel
  Executes concurrently
  waits for all to complete
end note

note right of loop
  Iterative processing
  with configurable exit
  conditions
end note

note right of subgraph
  Can be resumed from
  any checkpoint
end note

note bottom of aggregator
  Combines all incoming
  states into single
  result state
end note

@enduml
```

**Graph Execution Model:**
- Directed Acyclic Graph (DAG) structure
- State flows through nodes and edges
- Support for various execution patterns
- Conditional routing based on state
- Aggregation of multiple execution paths

## Key Architecture Patterns

### 1. Graph-Based Execution
- Nodes represent computational units
- Edges define flow and routing
- State transformation at each step
- Support for parallel and conditional execution

### 2. Agent-Oriented Design
- High-level abstractions for AI interactions
- Encapsulation of model, tools, and memory
- Composable agent patterns

### 3. Reactive Programming
- Non-blocking I/O with Project Reactor
- Streaming responses via Flux/Mono
- Backpressure handling

### 4. Configuration-Driven
- Declarative agent definitions
- YAML/JSON configuration support
- Dynamic configuration updates

### 5. Plugin Architecture
- Extensible tool framework
- Interceptor chain for cross-cutting concerns
- Custom node implementations

## Integration Summary

| Integration | Purpose | Module |
|------------|---------|--------|
| DashScope SDK | AI model access | Graph Core |
| Spring AI | Model abstraction | Graph Core |
| Nacos | Service discovery & config | Agent Framework |
| Redis | Caching & checkpoints | All modules |
| A2A SDK | Agent communication | Agent Framework |
| MCP | Tool integration | Agent Framework |

## Conclusion

Spring AI Alibaba provides a comprehensive, production-ready framework for building AI applications with:

- **Scalability**: Reactive architecture supporting high concurrency
- **Flexibility**: Modular design allowing custom extensions
- **Observability**: Built-in tracing, metrics, and state inspection
- **Developer Experience**: Rich tooling and configuration options
- **Production Ready**: Checkpointing, error recovery, and monitoring