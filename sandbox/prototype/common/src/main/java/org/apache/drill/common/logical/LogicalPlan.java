/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.apache.drill.common.logical;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.drill.common.logical.OperatorGraph.OpNode;
import org.apache.drill.common.logical.data.LogicalOperator;
import org.apache.drill.common.logical.graph.GraphAlgos;
import org.apache.drill.common.logical.sources.DataSource;
import org.apache.drill.common.logical.sources.record.RecordMaker;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.io.Files;

@JsonPropertyOrder({"head", "sources", "query"})
public class LogicalPlan {
	private final PlanProperties properties;
	private final Map<String, DataSource> dataSources;
	private final OperatorGraph graph;

	public static void main(String[] args) throws Exception {
		
		
		ObjectMapper mapper = new ObjectMapper();
		
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		mapper.configure(Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
		mapper.configure(Feature.ALLOW_COMMENTS, true);

		mapper.registerSubtypes(LogicalOperator.SUB_TYPES);
		mapper.registerSubtypes(DataSource.SUB_TYPES);
		mapper.registerSubtypes(RecordMaker.SUB_TYPES);

    String externalPlan = Files.toString(new File("src/test/resources/simple_plan.json"), Charsets.UTF_8);
    LogicalPlan plan = mapper.readValue(externalPlan, LogicalPlan.class);
    System.out.println(mapper.writeValueAsString(plan));	
	}

	
	@JsonCreator
	public LogicalPlan(@JsonProperty("head") PlanProperties head, @JsonProperty("sources") List<DataSource> sources, @JsonProperty("query") List<LogicalOperator> operators){
	  this.properties = head;
	  this.dataSources = new HashMap<String, DataSource>(sources.size());
    for(DataSource ds: sources){
      DataSource old = dataSources.put(ds.getName(), ds);
      if(old != null) throw new IllegalArgumentException("Each data source must have a unique name.  You provided more than one data source with the same name of '" + ds.getName() + "'");
    }
    
    this.graph = new OperatorGraph(operators);
	}
	
	@JsonProperty("query")
	public List<LogicalOperator> getOperators(){
	  List<OpNode> nodes = GraphAlgos.TopoSorter.sort(graph.getAdjList());
	  Iterable<LogicalOperator> i = Iterables.transform(nodes, new Function<OpNode, LogicalOperator>(){
	    public LogicalOperator apply(OpNode o){
	      return o.getNodeValue();
	    }
	  });
	  return Lists.newArrayList(i);
	}


	@JsonProperty("head")
  public PlanProperties getProperties() {
    return properties;
  }


	@JsonProperty("sources") 
  public List<DataSource> getDataSources() {
    return new ArrayList<DataSource>(dataSources.values());
  }
	
	
	
	
}
