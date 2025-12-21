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

/**
 * Definition of a graph edge, typically loaded from a database or configuration.
 *
 * @author xiweng.yy
 */
public class EdgeDefinition {

	private String source;

	private String target;

	/**
	 * Optional condition value for conditional edges.
	 * If present, this edge represents a specific path from a Router node.
	 */
	private String condition;

	public EdgeDefinition() {
	}

	public EdgeDefinition(String source, String target, String condition) {
		this.source = source;
		this.target = target;
		this.condition = condition;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
	}

	public String getCondition() {
		return condition;
	}

	public void setCondition(String condition) {
		this.condition = condition;
	}
}
