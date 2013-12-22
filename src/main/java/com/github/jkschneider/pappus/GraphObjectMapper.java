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

public class GraphObjectMapper {
	ObjectMapper oMapper = new ObjectMapper();
	MapToVertexMapper vMapper; 
	
	public GraphObjectMapper(Graph g) {
		oMapper.setSerializationInclusion(Include.NON_NULL);
		vMapper = new MapToVertexMapper(g);
	}
	
	@SuppressWarnings("unchecked")
	public Vertex toGraph(Object o) {
		Map<Object, Object> m = oMapper.convertValue(o, HashMap.class);
		return vMapper.toGraph(m, o.getClass());
	}
	
	public <E> E fromGraph(Vertex v, Class<E> clazz) {
		if(v == null)
			return null;
		Map<Object, Object> m = vMapper.fromGraph(v);
		return oMapper.convertValue(m, clazz);
	}
	
	public <E> List<E> fromGraph(Iterable<Vertex> vIterable, Class<E> clazz) {
		return fromGraph(vIterable.iterator(), clazz);
	}
	
	public <E> List<E> fromGraph(Iterator<Vertex> vIterator, Class<E> clazz) {
		List<E> all = new ArrayList<>();
		for(Iterator<Vertex> vIter = vIterator; vIter.hasNext();)
			all.add(fromGraph(vIter.next(), clazz));
		return all;
	}
	
	public <E> List<E> fromGraph(Map<?, Vertex> mapWithVertexValues, Class<E> clazz) {
		return fromGraph(mapWithVertexValues.values().iterator(), clazz);
	}
	
	public ObjectMapper getObjectMapper() {
		return oMapper;
	}

	public GraphObjectMapper typeKey(String typeKey) {
		vMapper.typeKey = typeKey;
		return this;
	}

	public GraphObjectMapper indexKey(String indexKey) {
		vMapper.indexKey = indexKey;
		return this;
	}
	
	public GraphObjectMapper hashKey(String hashKey) {
		vMapper.hashKey = hashKey;
		return this;
	}
}