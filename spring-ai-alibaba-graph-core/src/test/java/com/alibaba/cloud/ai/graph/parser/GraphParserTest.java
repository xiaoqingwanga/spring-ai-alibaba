/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.graph.parser;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncCommandAction;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.action.Command;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for GraphParser.
 */
public class GraphParserTest {

	private ApplicationContext applicationContext;
	private GraphDefinitionRepository repository;
	private GraphParser parser;

	@BeforeEach
	public void setup() {
		applicationContext = mock(ApplicationContext.class);
		repository = mock(GraphDefinitionRepository.class);
		parser = new GraphParser(applicationContext, repository);
	}

	@Test
	public void testParseSimpleGraph() throws Exception {
		// Define Nodes
		NodeDefinition nodeA = new NodeDefinition("nodeA", NodeType.STANDARD, "beanA", null);
		NodeDefinition nodeB = new NodeDefinition("nodeB", NodeType.STANDARD, "beanB", null);

		// Define Edges
		EdgeDefinition edge1 = new EdgeDefinition(StateGraph.START, "nodeA", null);
		EdgeDefinition edge2 = new EdgeDefinition("nodeA", "nodeB", null);
		EdgeDefinition edge3 = new EdgeDefinition("nodeB", StateGraph.END, null);

		// Mock Beans
		AsyncNodeAction actionA = state -> CompletableFuture.completedFuture(Map.of("step", "A"));
		AsyncNodeAction actionB = state -> CompletableFuture.completedFuture(Map.of("step", "B"));

		when(applicationContext.isTypeMatch("beanA", AsyncNodeAction.class)).thenReturn(true);
		when(applicationContext.isTypeMatch("beanB", AsyncNodeAction.class)).thenReturn(true);
		when(applicationContext.getBean("beanA")).thenReturn(actionA);
		when(applicationContext.getBean("beanB")).thenReturn(actionB);

		// Parse
		StateGraph graph = parser.parse(List.of(nodeA, nodeB), List.of(edge1, edge2, edge3));

		assertNotNull(graph);
		
		// Compile to verify structure validity
		CompiledGraph compiled = graph.compile();
		assertNotNull(compiled);
	}

	@Test
	public void testParseConditionalGraph() throws Exception {
		// Define Nodes
		NodeDefinition router = new NodeDefinition("router", NodeType.STANDARD, "routerBean", null);
		NodeDefinition nodeYes = new NodeDefinition("yes", NodeType.STANDARD, "actionBean", null);
		NodeDefinition nodeNo = new NodeDefinition("no", NodeType.STANDARD, "actionBean", null);

		// Define Edges
		EdgeDefinition edgeStart = new EdgeDefinition(StateGraph.START, "router", null);
		EdgeDefinition edgeYes = new EdgeDefinition("router", "yes", "YES");
		EdgeDefinition edgeNo = new EdgeDefinition("router", "no", "NO");
		EdgeDefinition edgeEnd1 = new EdgeDefinition("yes", StateGraph.END, null);
		EdgeDefinition edgeEnd2 = new EdgeDefinition("no", StateGraph.END, null);

		// Mock Beans
		AsyncCommandAction routerAction = (state, config) -> 
			CompletableFuture.completedFuture(new Command("router", Map.of()));
		
		AsyncNodeAction actionBean = state -> CompletableFuture.completedFuture(Map.of());

		when(applicationContext.isTypeMatch("routerBean", AsyncCommandAction.class)).thenReturn(true);
		when(applicationContext.isTypeMatch("actionBean", AsyncNodeAction.class)).thenReturn(true);
		when(applicationContext.getBean("routerBean")).thenReturn(routerAction);
		when(applicationContext.getBean("actionBean")).thenReturn(actionBean);

		// Parse
		StateGraph graph = parser.parse(
			List.of(router, nodeYes, nodeNo), 
			List.of(edgeStart, edgeYes, edgeNo, edgeEnd1, edgeEnd2)
		);

		assertNotNull(graph);
		CompiledGraph compiled = graph.compile();
		assertNotNull(compiled);
	}

	@Test
	public void testParseSubGraph() throws Exception {
		// Main Graph: Start -> SubGraph -> End
		NodeDefinition subNode = new NodeDefinition("sub", NodeType.SUB_GRAPH, "subGraphId", null);
		EdgeDefinition edge1 = new EdgeDefinition(StateGraph.START, "sub", null);
		EdgeDefinition edge2 = new EdgeDefinition("sub", StateGraph.END, null);

		// Sub Graph: Start -> NodeA -> End
		NodeDefinition nodeA = new NodeDefinition("nodeA", NodeType.STANDARD, "beanA", null);
		EdgeDefinition subEdge1 = new EdgeDefinition(StateGraph.START, "nodeA", null);
		EdgeDefinition subEdge2 = new EdgeDefinition("nodeA", StateGraph.END, null);

		// Mock Repository
		when(repository.findNodesByGraphId("subGraphId")).thenReturn(List.of(nodeA));
		when(repository.findEdgesByGraphId("subGraphId")).thenReturn(List.of(subEdge1, subEdge2));

		// Mock Bean
		AsyncNodeAction actionA = state -> CompletableFuture.completedFuture(Map.of("step", "A"));
		// Security Check Mocks
		when(applicationContext.isTypeMatch("beanA", AsyncNodeAction.class)).thenReturn(true);
		when(applicationContext.getBean("beanA")).thenReturn(actionA);

		// Parse Main Graph
		StateGraph graph = parser.parse(List.of(subNode), List.of(edge1, edge2));

		assertNotNull(graph);
		CompiledGraph compiled = graph.compile();
		assertNotNull(compiled);
	}

	@Test
	public void testSecurityTypeCheck() {
		// Define Node pointing to a non-node bean (e.g., a DataSource)
		NodeDefinition maliciousNode = new NodeDefinition("malicious", NodeType.STANDARD, "dataSourceBean", null);
		EdgeDefinition edge = new EdgeDefinition(StateGraph.START, "malicious", null);

		// Mock Bean Type Check Failure
		when(applicationContext.isTypeMatch("dataSourceBean", AsyncNodeAction.class)).thenReturn(false);
		when(applicationContext.isTypeMatch("dataSourceBean", AsyncCommandAction.class)).thenReturn(false);

		// Parse should throw
		try {
			parser.parse(List.of(maliciousNode), List.of(edge));
			throw new RuntimeException("Should have thrown IllegalArgumentException");
		} catch (RuntimeException e) {
			// Expected
			if (e.getCause() instanceof IllegalArgumentException) {
				String msg = e.getCause().getMessage();
				if (!msg.contains("Security Error")) {
					throw new RuntimeException("Unexpected error message: " + msg);
				}
			} else {
				throw e;
			}
		}
	}

	@Test
	public void testCycleDetection() {
		// Graph A -> Graph B -> Graph A
		NodeDefinition nodeA = new NodeDefinition("nodeA", NodeType.SUB_GRAPH, "graphB", null);
		EdgeDefinition edgeA = new EdgeDefinition(StateGraph.START, "nodeA", null);

		NodeDefinition nodeB = new NodeDefinition("nodeB", NodeType.SUB_GRAPH, "graphA", null);
		EdgeDefinition edgeB = new EdgeDefinition(StateGraph.START, "nodeB", null);

		when(repository.findNodesByGraphId("graphA")).thenReturn(List.of(nodeA));
		when(repository.findEdgesByGraphId("graphA")).thenReturn(List.of(edgeA));

		when(repository.findNodesByGraphId("graphB")).thenReturn(List.of(nodeB));
		when(repository.findEdgesByGraphId("graphB")).thenReturn(List.of(edgeB));

		try {
			parser.parse("graphA");
			throw new RuntimeException("Should have thrown RuntimeException -> IllegalArgumentException for cycle");
		} catch (RuntimeException e) {
			Throwable current = e;
			boolean found = false;
			while (current != null) {
				if (current instanceof IllegalArgumentException && current.getMessage().contains("infinite recursion")) {
					found = true;
					break;
				}
				current = current.getCause();
			}
			
			if (!found) {
				throw new RuntimeException("Unexpected error chain: " + e.getMessage(), e);
			}
		}
	}
}
