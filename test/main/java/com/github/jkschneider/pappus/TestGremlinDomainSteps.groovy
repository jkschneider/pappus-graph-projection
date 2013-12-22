package com.github.jkschneider.pappus

import static org.junit.Assert.*

import org.junit.BeforeClass
import org.junit.Test

import com.carrotsearch.junitbenchmarks.AbstractBenchmark
import com.tinkerpop.blueprints.Graph
import com.tinkerpop.blueprints.Vertex
import com.tinkerpop.blueprints.impls.tg.TinkerGraph
import com.tinkerpop.gremlin.groovy.Gremlin


class TestGremlinDomainSteps extends AbstractBenchmark {
	static class A {
	}
	
	static Graph g
	static Vertex a1
	static Vertex a2
	
	@BeforeClass
	static void beforeClass() {
		Gremlin.load()
		GremlinModelSteps.load()
		
		g = new TinkerGraph()
		a1 = g.addVertex(null)
		a1['_type'] = A.class.getName()
		a1['_timestamp'] = 1
		a1['_id'] = 123
		
		a2 = g.addVertex(null)
		a2['_type'] = A.class.getName()
		a2['_timestamp'] = 2
		a2['_id'] = 123
		
		5000.times {
			def b = g.addVertex(null)
			b['name'] = 'b'
			a1.addEdge('followMe', b)
			a2.addEdge('followMe', b)
			
			def c = g.addVertex(null)
			c['name'] = 'c'
			b.addEdge('followMe', c)
		}
		
		5000.times {
			def c = g.addVertex(null)
			c['name'] = 'c'
			a1.addEdge('followMe', c)
			a2.addEdge('followMe', c)
		}
		
		def cIsolated = g.addVertex(null)
		cIsolated['name'] = 'c'
		
		// TODO add simplePath to prevent cycles
		// TODO add except pattern to not look at the same vertices multiple times on the way to domain roots
		
		def bIsolated = g.addVertex(null)
		bIsolated.addEdge('followMe', cIsolated)
	}
	
	@Test
	void domainWithMapReduce() {
		assert g.V('name', 'c').model(A.class)
			.groupBy{it['_id']}{it}{ it.max{ it['_timestamp'] } }
			.cap()
			.next() == [123:a2]
	}
}
