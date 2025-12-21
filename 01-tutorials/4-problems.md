# Common Challenges & Solutions

## 1. Defining Workflows without Code

**Challenge**: [Issue #3174](https://github.com/alibaba/spring-ai-alibaba/issues/3174) - Without a DSL or configuration mechanism, constructing `StateGraph` requires writing Java code, which restricts the ability to change workflows at runtime.

**Solution**: The **Agent Config** feature, integrated via the `spring-ai-alibaba-starter-config-nacos` starter, solves this. It allows you to define agent workflows using YAML configuration stored in Nacos. This enables a "no-code" or "low-code" approach where the graph structure is defined externally and loaded by the application.