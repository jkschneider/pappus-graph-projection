package com.github.jkschneider.pappus;

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
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.Vertex;

public class MapToVertexMapper {
	String typeProperty = "_type";
	String indexProperty = "_index";
	String hashProperty = "_hash";
	String keyProperty = "_key";
	
	Cache<Long, Object> hashToVertexId = CacheBuilder.newBuilder().maximumSize(8192).build();
	
	NameConverter nameTools = new DefaultNameConverter();
	RecursiveMapDecorator hasher = new RecursiveMapDecorator();
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
				return (Class<?>) collectionType.getActualTypeArguments()[0];
			}
			return f.getType();
		} catch (NoSuchFieldException | SecurityException e) {
			throw new RuntimeException(e);
		}
	}
	
	@SuppressWarnings("unchecked")
	public Vertex toGraph(Map<Object, Object> map, Class<?> c) {
		Long hash = hasher.hash(map, c);
		Vertex v;
		
		Object id = hashToVertexId.getIfPresent(hash);
		if(id != null)
			return g.getVertex(id);
		Iterator<Vertex> vIter = g.query().has(hashProperty, map.get(hashProperty)).vertices().iterator();
		if(vIter.hasNext()) {
			v = vIter.next();
			hashToVertexId.put(hash, v.getId());
			commitIfNecessary();
			return v;
		}
		v = g.addVertex(null);
		v.setProperty(typeProperty, c.getName());
		
		for(Entry<Object, Object> e : ((Map<Object, Object>) map).entrySet()) {
			String fieldName = e.getKey().toString();
			
			if(Map.class.isAssignableFrom(e.getValue().getClass())) {
				// this field is a complex type, which will be mapped to a subgraph
				Class<?> fieldType = getChildType(fieldName, c);
				Map<Object, Object> e2 = (Map<Object, Object>) e.getValue();
				
				if(Map.class.isAssignableFrom(fieldType)) {
					// the field type itself is a map
					try {
						Field f = c.getDeclaredField(fieldName);
						ParameterizedType mapGenericTypes = (ParameterizedType) f.getGenericType();
						Class<?> mapValueType = (Class<?>) mapGenericTypes.getActualTypeArguments()[1];
						
//						if(mapValueType is not a primitive) {
//							for(Entry<Object, Object> entry : e2.entrySet()) {
//								Vertex v2 = toGraph((Map<Object, Object>) entry.getValue(), mapValueType);
//							}
//						}
//						else {
							v.setProperty(fieldName, e.getValue());
//						}
					} catch (NoSuchFieldException | SecurityException ex) {
						throw new RuntimeException(ex);
					}
				}
				else {
					Vertex v2 = toGraph(e2, fieldType);
					v.addEdge(fieldName, v2);
				}
			}
			else if(Collection.class.isAssignableFrom(e.getValue().getClass())) {
				// this field represents a collection of objects
				Collection<Object> e2 = (Collection<Object>) e.getValue();
				if(e2.isEmpty())
					continue;
				Class<?> collType = e2.iterator().next().getClass();
				
				if(Map.class.isAssignableFrom(collType)) {
					// the collection contains complex types that will be mapped to individual subgraphs
					int i = 0;
					for(Object e3 : e2) {
						Vertex v2 = toGraph((Map<Object, Object>) e3, getChildType(fieldName, c));
						Edge edge = v.addEdge(nameTools.depluralize(fieldName), v2);
						edge.setProperty(indexProperty, i++);
					}
				}
				else {
					// the collection contains primitive types... we will store the whole collection on a single property
					v.setProperty(fieldName, e.getValue());
				}
			}
			else
				v.setProperty(fieldName, e.getValue());
		}
		
		commitIfNecessary();
		return v;
	}
	
	private void commitIfNecessary() {
		if(TransactionalGraph.class.isAssignableFrom(g.getClass()))
			((TransactionalGraph) g).commit();
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
				// descending order by indexLabel
				return (int) edge2.getProperty(indexProperty) - (int) edge1.getProperty(indexProperty);
			}
			return edge1.getLabel().compareTo(edge2.getLabel());
		}
	};
	
	protected void fromGraph(Vertex v, Map<Object, Object> map) {
		for(String key : v.getPropertyKeys())
			if(!hashProperty.equals(key) && !typeProperty.equals(key)) map.put(key, v.getProperty(key));

		PriorityQueue<Edge> edgeQueue = new PriorityQueue<Edge>(10, edgeSorter);
		for(Iterator<Edge> edgeIter = v.query().direction(Direction.OUT).edges().iterator(); edgeIter.hasNext();)
			edgeQueue.add(edgeIter.next());

		String collectionLabel = null;
		List<Map<Object, Object>> collection = null;
		
		Edge e;
		while((e = edgeQueue.poll()) != null) {
			if(e.getProperty(indexProperty) != null) {
				if(!e.getLabel().equals(collectionLabel)) {
					collection = new ArrayList<Map<Object, Object>>((int) e.getProperty(indexProperty));
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
		
		commitIfNecessary();
	}
}
