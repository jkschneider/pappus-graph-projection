package com.github.jkschneider.pappus

import static org.junit.Assert.*

import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

import com.carrotsearch.junitbenchmarks.AbstractBenchmark
import com.tinkerpop.blueprints.Edge
import com.tinkerpop.blueprints.Graph
import com.tinkerpop.blueprints.Vertex
import com.tinkerpop.blueprints.impls.tg.TinkerGraph
import com.tinkerpop.gremlin.groovy.Gremlin

class TestMapToVertexMapper /*extends AbstractBenchmark*/ {
	class A {
		B b
		List<B> objects = []
		Long num
		String name
		List<String> strings = []
		B[] objectArrs = []
	}

	class B {
		String name
		B b
	}
	
	class C {
		Map<String, Object> keyValues = new HashMap<String, Object>()
	}
	
	Graph g
	MapToVertexMapper mapper
		
	@BeforeClass
	static void beforeClass() {
		Gremlin.load()
	}
	
	@Before
	void before() {
		g = new TinkerGraph()
		mapper = new MapToVertexMapper(g)
	}
	
	@Test
	void fromGraphExcludesHash() {
		Vertex a = g.addVertex(null)
		a.name = 'a1'
		a['_hash'] = 123
		assert mapper.fromGraph(a) == [name: 'a1']
	}
	
	@Test
	void toGraphTypeDecorations() {
		Vertex v = mapper.toGraph([b: [name: 'b'], objects: [[name:'b2']], objectArrs: [[name:'b2']]], A.class)
		assert v['_type'] == A.class.getName()
		['b', 'object', 'objectArr'].each {
			def bIter = v.out(it).iterator()
			assert bIter.hasNext() && bIter.next()['_type'] == B.class.getName()
		}
	}
	
	@Test
	void toGraphPrimitives() {
		Vertex v = mapper.toGraph([name: 'a1', num: 123], A.class)
		assert v['name'] == 'a1'
		assert v['num'] == 123
	}
	
	@Test
	void toGraphNested() {
		Vertex v = mapper.toGraph([b: [name: 'b1']], A.class)
		def iter = v.out('b').iterator()
		assert iter.hasNext() && iter.next().name == 'b1'
	}
	
	@Test
	void toGraphPrimitiveArray() {
		Vertex v = mapper.toGraph([strings : ['a','b','c']], A.class)
		assert v['strings'] == ['a','b','c']
	}
	
	@Test
	void toGraphObjectArray() {
		Vertex v = mapper.toGraph([objects: [[name: 'b1'], [name: 'b2']]], A.class)
		def iter = v.out('object').iterator()
		assert iter.hasNext() && iter.next().name == 'b1'
		assert iter.hasNext() && iter.next().name == 'b2'
	}
	
	@Test
	void toGraphMap() {
		Vertex v = mapper.toGraph([keyValues: new HashMap<String, Integer>() {{ put("a", 1) }} ], C.class)
		assert v.keyValues == ["a":1]
	}
	
	@Test
	void fromGraphPrimitives() {
		Vertex a = g.addVertex(null)
		a.name = 'a1'
		a.num = 123
		assert mapper.fromGraph(a) == [name: 'a1', num: 123]
	}
	
	@Test
	void fromGraphNested() {
		Vertex a = g.addVertex(null)
		Vertex b = g.addVertex(null)
		b.name = 'b1'
		Edge e = a.addEdge('b', b)
		e['_type'] = 'undefined'
		assert mapper.fromGraph(a) == [b: [name: 'b1']]
	}
	
	@Test
	void fromGraphPrimitiveArray() {
		Vertex a = g.addVertex(null)
		a.strings = ['a','b','c']
		assert mapper.fromGraph(a) == [strings : ['a','b','c']]
	}
	
	@Test
	void fromGraphObjectArray() {
		Vertex a = g.addVertex(null)
		Vertex b1 = g.addVertex(null)
		b1.name = 'b1'
		Vertex b2 = g.addVertex(null)
		b2.name = 'b2'
		Edge eb1 = a.addEdge("object", b1)
		Edge eb2 = a.addEdge("object", b2)
		eb1['_index'] = 0
		eb1['_type'] = 'undefined'
		eb2['_index'] = 1
		eb2['_type'] = 'undefined'
		assert mapper.fromGraph(a) == [objects: [[name: 'b1'], [name: 'b2']]]
		
		// preserves order
		eb1['_index'] = 2
		assert mapper.fromGraph(a) == [objects: [[name: 'b2'], [name: 'b1']]]
	}
	
	@Test
	void identicalChildrenResultInASingleVertex() {
		Vertex a1 = mapper.toGraph([name:'a1', b:[name:'b1']], A.class)
		Vertex a2 = mapper.toGraph([name:'a2', b:[name:'b1']], A.class)
		assert g.vertices.count { 1 } == 3
		assert g.edges.count { 1 } == 2
	}
	
	@Test
	void identicalChildrenExceptOrderingResultInOneVertexPerChild() {
		Vertex a1 = mapper.toGraph([name:'a1', objects: [[name: 'b2'], [name: 'b1']]], A.class)
		Vertex a2 = mapper.toGraph([name:'a1', objects: [[name: 'b1'], [name: 'b2']]], A.class)
		assert g.vertices.count { 1 } == 4
		assert g.edges.count { 1 } == 4
	}
	
	@Test
	void classType() {
		assert mapper.getChildType("b", A.class) == B.class
		assert mapper.getChildType("objects", A.class) == B.class
		assert mapper.getChildType("objectArrs", A.class) == B.class
	}
}
