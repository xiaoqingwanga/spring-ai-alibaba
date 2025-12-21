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

import java.util.Map;

/**
 * Definition of a graph node, typically loaded from a database or configuration.
 *
 * @author xiweng.yy
 */
public class NodeDefinition {

	private String id;

	private NodeType type;

	/**
	 * The resource identifier for the node's logic.
	 * For STANDARD nodes, this is the Bean Name.
	 * For SUB_GRAPH nodes, this is the Graph ID.
	 */
	private String resource;

	private Map<String, Object> properties;

	public NodeDefinition() {
	}

	public NodeDefinition(String id, NodeType type, String resource, Map<String, Object> properties) {
		this.id = id;
		this.type = type;
		this.resource = resource;
		this.properties = properties;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public NodeType getType() {
		return type;
	}

	public void setType(NodeType type) {
		this.type = type;
	}

	public String getResource() {
		return resource;
	}

	public void setResource(String resource) {
		this.resource = resource;
	}

	public Map<String, Object> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, Object> properties) {
		this.properties = properties;
	}
}
