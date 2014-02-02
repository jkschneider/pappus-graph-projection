package com.github.jkschneider.pappus

import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

import com.tinkerpop.blueprints.Graph
import com.tinkerpop.blueprints.impls.tg.TinkerGraph
import com.tinkerpop.gremlin.groovy.Gremlin

class TestMapToVertexMapperAtVolume {
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
	void test10000UniqueVertices() {
		(1..10000).each { n -> mapper.toGraph([num: n], TestMapToVertexMapperAtVolume.class) }
		(1..100).each { n -> mapper.toGraph([num: n], TestMapToVertexMapperAtVolume.class) }
		assert g.V.count() == 10000
	}
}
