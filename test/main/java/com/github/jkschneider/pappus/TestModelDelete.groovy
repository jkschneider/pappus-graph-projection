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
		def bob = new Person(name: 'bob', cars: [ ford ])
		
		def jonV = mapper.toGraph(jon)
		def bobV = mapper.toGraph(bob)
		
		assert g.vertices.size() == 4

		def removable
		g.V('name', 'jon')
			.as('x')
			.sideEffect { v ->
				if(!v.in.hasNext()) {
					println v.propertyKeys.collect { k -> "$k -> ${v.getProperty(k)}" }.join(", ")
					removable = v
				}
			}
			.out
			.sideEffect {
				if(removable) {
					removable.remove()
					removable = null
				}
			}
			.loop('x') { true }
			.sideEffect {
				// FIXME this last step is not being called...
				if(removable)
					removable.remove()
			}
			.iterate()

		println "\nEnding vertices:"
		assert g.vertices.each { v ->
			println v.propertyKeys.collect { k -> "$k -> ${v.getProperty(k)}" }.join(", ")
		}		
		assert g.vertices.size() == 2
	}
}
