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

import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncCommandAction;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import org.springframework.context.ApplicationContext;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Parser that converts graph definitions (Nodes and Edges) into a StateGraph.
 * It uses Spring ApplicationContext to resolve node logic (Beans) and
 * can recursively load sub-graphs.
 *
 * @author xiweng.yy
 */
public class GraphParser {

	private final ApplicationContext applicationContext;
	private final GraphDefinitionRepository repository;

	public GraphParser(ApplicationContext applicationContext, GraphDefinitionRepository repository) {
		this.applicationContext = applicationContext;
		this.repository = repository;
	}

	/**
	 * Parses a graph definition given a graph ID.
	 * @param graphId the ID of the graph to load
	 * @return the constructed StateGraph
	 */
	public StateGraph parse(String graphId) {
		List<NodeDefinition> nodes = repository.findNodesByGraphId(graphId);
		List<EdgeDefinition> edges = repository.findEdgesByGraphId(graphId);
		return parse(nodes, edges);
	}

	/**
	 * Parses a list of node and edge definitions into a StateGraph.
	 * @param nodes list of node definitions
	 * @param edges list of edge definitions
	 * @return the constructed StateGraph
	 */
	public StateGraph parse(List<NodeDefinition> nodes, List<EdgeDefinition> edges) {
		StateGraph stateGraph = new StateGraph();

		// Map sourceId -> List<EdgeDefinition>
		Map<String, List<EdgeDefinition>> edgesBySource = edges.stream()
				.collect(Collectors.groupingBy(EdgeDefinition::getSource));

		try {
			for (NodeDefinition node : nodes) {
				String id = node.getId();
				String resource = node.getResource();
				NodeType type = node.getType();

				List<EdgeDefinition> nodeEdges = edgesBySource.getOrDefault(id, List.of());
				boolean isConditional = nodeEdges.stream().anyMatch(e -> StringUtils.hasText(e.getCondition()));

				if (type == NodeType.SUB_GRAPH) {
					// Recursive load
					StateGraph subStateGraph = parse(resource);
					stateGraph.addNode(id, subStateGraph);

					if (isConditional) {
						throw new IllegalArgumentException("Conditional edges from SubGraph node '" + id + "' are not supported yet.");
					}

				} else {
					// STANDARD Node
					Object bean = applicationContext.getBean(resource);

					if (isConditional) {
						if (bean instanceof AsyncCommandAction commandAction) {
							Map<String, String> mappings = new HashMap<>();
							for (EdgeDefinition edge : nodeEdges) {
								if (StringUtils.hasText(edge.getCondition())) {
									mappings.put(edge.getCondition(), edge.getTarget());
								}
							}
							stateGraph.addNode(id, commandAction, mappings);
						} else {
							throw new IllegalArgumentException("Node '" + id + "' has conditional edges but bean '" + resource + "' is not an AsyncCommandAction.");
						}
					} else {
						if (bean instanceof AsyncNodeAction nodeAction) {
							stateGraph.addNode(id, nodeAction);
						} else if (bean instanceof AsyncCommandAction) {
							throw new IllegalArgumentException("Node '" + id + "' has no conditional edges but bean '" + resource + "' is an AsyncCommandAction. Use AsyncNodeAction for standard steps.");
						} else {
							throw new IllegalArgumentException("Bean '" + resource + "' for node '" + id + "' must implement AsyncNodeAction or AsyncCommandAction.");
						}
					}
				}
			}

			// Add Standard Edges (non-conditional)
			for (EdgeDefinition edge : edges) {
				if (!StringUtils.hasText(edge.getCondition())) {
					stateGraph.addEdge(edge.getSource(), edge.getTarget());
				}
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to parse graph definition", e);
		}

		return stateGraph;
	}
}
