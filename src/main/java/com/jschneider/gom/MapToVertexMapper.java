package com.jschneider.gom;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;

import org.jibx.schema.codegen.extend.DefaultNameConverter;
import org.jibx.schema.codegen.extend.NameConverter;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;

public class MapToVertexMapper {
	Cache<Long, Object> hashToVertexId = CacheBuilder.newBuilder().maximumSize(8192).build();
	
	NameConverter nameTools = new DefaultNameConverter();
	RecursiveMapHasher hasher = new RecursiveMapHasher();
	Graph g;
	
	public MapToVertexMapper(Graph g) {
		this.g = g;
	}

	protected Class<?> getChildType(String field, Class<?> c) {
		try {
			Field f = c.getDeclaredField(field);
			if(f.getType().isArray())
				return f.getType().getComponentType();
			else if(Collection.class.isAssignableFrom(f.getType())) {
				ParameterizedType collectionType = (ParameterizedType) f.getGenericType();
				return ((Class<?>) collectionType.getActualTypeArguments()[0]);
			}
			return f.getType();
		} catch (NoSuchFieldException | SecurityException e) {
			throw new RuntimeException(e);
		}
	}
	
	@SuppressWarnings("unchecked")
	public Vertex toGraph(Map<Object, Object> map, Class<?> c) {
		Long hash = hasher.hash(map);
		Vertex v;
		
		Object id = hashToVertexId.getIfPresent(hash);
		if(id != null)
			return g.getVertex(id);
		Iterator<Vertex> vIter = g.query().has("_hash", map.get("_hash")).vertices().iterator();
		if(vIter.hasNext()) {
			v = vIter.next();
			hashToVertexId.put(hash, v.getId());
			return v;
		}
		v = g.addVertex(null);
		v.setProperty("_type", c.getName());
		
		for(Entry<Object, Object> e : ((Map<Object, Object>) map).entrySet()) {
			if(Map.class.isAssignableFrom(e.getValue().getClass())) {
				Vertex v2 = toGraph((Map<Object, Object>) e.getValue(), getChildType(e.getKey().toString(), c));
				v.addEdge(e.getKey().toString(), v2);
			}
			else if(Collection.class.isAssignableFrom(e.getValue().getClass())) {
				Collection<Object> e2 = (Collection<Object>) e.getValue();
				if(e2.isEmpty())
					continue;
				Class<?> collType = e2.iterator().next().getClass();
				if(Map.class.isAssignableFrom(collType)) {
					int i = 0;
					for(Object e3 : e2) {
						Vertex v2 = toGraph((Map<Object, Object>) e3, getChildType(e.getKey().toString(), c));
						Edge edge = v.addEdge(nameTools.depluralize(e.getKey().toString()), v2);
						edge.setProperty("_index", i++);
					}
				}
				else
					v.setProperty(e.getKey().toString(), e.getValue());
			}
			else
				v.setProperty(e.getKey().toString(), e.getValue());
		}
		
		return v;
	}
	
	public Map<Object, Object> fromGraph(Vertex v) {
		Map<Object, Object> map = new HashMap<Object, Object>();
		fromGraph(v, map);
		return map;
	}
	
	Comparator<Edge> edgeSorter = new Comparator<Edge>() {
		@Override
		public int compare(Edge edge1, Edge edge2) {
			if(edge1.getLabel().equals(edge2.getLabel())) {
				// descending order by _index
				return (int) edge2.getProperty("_index") - (int) edge1.getProperty("_index");
			}
			return edge1.getLabel().compareTo(edge2.getLabel());
		}
	};
	
	protected void fromGraph(Vertex v, Map<Object, Object> map) {
		for(String key : v.getPropertyKeys())
			if(!"_hash".equals(key) && !"_type".equals(key)) map.put(key, v.getProperty(key));

		PriorityQueue<Edge> edgeQueue = new PriorityQueue<Edge>(10, edgeSorter);
		for(Iterator<Edge> edgeIter = v.query().direction(Direction.OUT).edges().iterator(); edgeIter.hasNext();)
			edgeQueue.add(edgeIter.next());

		String collectionLabel = null;
		List<Map<Object, Object>> collection = null;
		
		Edge e;
		while((e = edgeQueue.poll()) != null) {
			if(e.getProperty("_index") != null) {
				if(!e.getLabel().equals(collectionLabel)) {
					collection = new ArrayList<Map<Object, Object>>((int) e.getProperty("_index"));
					map.put(nameTools.pluralize(e.getLabel()), collection);
				}
				
				Map<Object, Object> child = new HashMap<Object, Object>();
				fromGraph(e.getVertex(Direction.IN), child);
				collection.add(0, child); // edges are sorted last index first
				
				collectionLabel = e.getLabel();
			}
			else {
				Map<Object, Object> child = new HashMap<Object, Object>();
				fromGraph(e.getVertex(Direction.IN), child);
				map.put(e.getLabel(), child);
			}
		}
	}
}
