package com.github.jkschneider.pappus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;

/**
 * Recursively maps arbitrary Java object models onto a Tinkerpop compatible
 * graph.
 * 
 * @author Jon Schneider
 */
public class GraphObjectMapper {
	ObjectMapper oMapper = new ObjectMapper();
	MapToVertexMapper vMapper; 
	
	public GraphObjectMapper(Graph g) {
		oMapper.setSerializationInclusion(Include.NON_NULL);
		vMapper = new MapToVertexMapper(g);
	}
	
	/**
	 * Commits object to the graph model. As each field is projected to a
	 * subgraph, the subgraph is hashed to determine if it already exists in the
	 * graph. If so, a new edge is drawn to the existing subgraph to prevent
	 * data duplication in the graph.
	 * 
	 * @param o
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public Vertex toGraph(Object o) {
		Map<Object, Object> m = oMapper.convertValue(o, HashMap.class);
		return vMapper.toGraph(m, o.getClass());
	}
	
	/**
	 * Projects a subgraph rooted at <code>v</code> onto a Java object model
	 * whose root is of type <code>clazz</code>.
	 * 
	 * @param v
	 * @param clazz
	 * @return
	 */
	public <E> E fromGraph(Vertex v, Class<E> clazz) {
		if(v == null)
			return null;
		Map<Object, Object> m = vMapper.fromGraph(v);
		return oMapper.convertValue(m, clazz);
	}
	
	/**
	 * Projects subgraphs rooted at each vertex in <code>vIterable</code> onto a list of Java object models
	 * whose roots are of type <code>clazz</code>.
	 * 
	 * @param v
	 * @param clazz
	 * @return
	 */
	public <E> List<E> fromGraph(Iterable<Vertex> vIterable, Class<E> clazz) {
		return fromGraph(vIterable.iterator(), clazz);
	}
	
	/**
	 * Projects subgraphs rooted at each vertex in <code>vIterable</code> onto a list of Java object models
	 * whose roots are of type <code>clazz</code>.
	 * 
	 * @param v
	 * @param clazz
	 * @return
	 */
	public <E> List<E> fromGraph(Iterator<Vertex> vIterator, Class<E> clazz) {
		List<E> all = new ArrayList<>();
		for(Iterator<Vertex> vIter = vIterator; vIter.hasNext();)
			all.add(fromGraph(vIter.next(), clazz));
		return all;
	}
	
	/**
	 * Projects subgraphs rooted at each vertex found in the values of
	 * <code>mapWithVertexValues</code> onto a list of Java object models whose
	 * roots are of type <code>clazz</code>.
	 * 
	 * @param v
	 * @param clazz
	 * @return
	 */
	public <E> List<E> fromGraph(Map<?, Vertex> mapWithVertexValues, Class<E> clazz) {
		return fromGraph(mapWithVertexValues.values().iterator(), clazz);
	}
	
	public ObjectMapper getObjectMapper() {
		return oMapper;
	}

	/**
	 * Sets the special property used to store type information on vertices.
	 * 
	 * @param typeKey
	 * @return
	 */
	public GraphObjectMapper typeKey(String typeKey) {
		vMapper.typeKey = typeKey;
		return this;
	}
	
	/**
	 * Sets the special property used to store list or array index information
	 * on edges so that a Java object model unwrapped from the graph containing
	 * lists or arrays have the order of these elements preserved.
	 * 
	 * @param indexKey
	 * @return
	 */
	public GraphObjectMapper indexKey(String indexKey) {
		vMapper.indexKey = indexKey;
		return this;
	}
	
	/**
	 * Special property used to store the subgraph hash. This hash is used in
	 * subsequent storage operations to determine if a portion of the model
	 * being stored already has an equivalent subgraph. If so, a new edge is
	 * drawn to the existing subgraph to preserve space.
	 * 
	 * @param hashKey
	 * @return
	 */
	public GraphObjectMapper hashKey(String hashKey) {
		vMapper.hashKey = hashKey;
		return this;
	}
}