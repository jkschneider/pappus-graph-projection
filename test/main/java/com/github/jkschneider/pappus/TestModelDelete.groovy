package com.github.jkschneider.pappus
import org.junit.Test

import com.github.jkschneider.pappus.GraphObjectMapper
import com.github.jkschneider.pappus.GremlinModelSteps
import com.github.jkschneider.pappus.example.Car
import com.github.jkschneider.pappus.example.Person
import com.tinkerpop.blueprints.Direction
import com.tinkerpop.blueprints.Vertex
import com.tinkerpop.blueprints.impls.tg.TinkerGraph
import com.tinkerpop.gremlin.groovy.Gremlin


class TestModelDelete {
	@Test
	void testRemove() {
		Gremlin.load()
		GremlinModelSteps.load()
		
		def g = new TinkerGraph()
		def mapper = new GraphObjectMapper(g)

		def ford = new Car(make: 'ford')
		def jon = new Person(name: 'jon', cars: [ ford, new Car('make' : 'dodge') ])
		def bob = new Person(name: 'bob', cars: [ ford, new Car('make' : 'chevy') ])
		
		def jonV = mapper.toGraph(jon)
		def bobV = mapper.toGraph(bob)
		
		assert g.vertices.size() == 5 // bob, jon, and their three cars
		jonV.cascadeDelete().iterate()
		assert g.vertices.size() == 3 // bob and his two cars
	}
}
