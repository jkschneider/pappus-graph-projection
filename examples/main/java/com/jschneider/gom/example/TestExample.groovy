package com.jschneider.gom.example;

import static org.junit.Assert.*

import org.junit.Test

import com.jschneider.gom.GraphObjectMapper
import com.jschneider.gom.GremlinModelSteps
import com.tinkerpop.blueprints.Vertex
import com.tinkerpop.blueprints.impls.tg.TinkerGraph
import com.tinkerpop.gremlin.groovy.Gremlin

class TestExample {
	@Test
	public void simpleGraph() {
		Gremlin.load()
		GremlinModelSteps.load();

		def address = new Address(address1: '123 main st')
		def jon = new Person(name: 'jon', age: 30, address: address)
		def escape = new Car(make: 'ford', model: 'escape', year: 2011)
		jon.cars.add(escape)
		
		def g = new TinkerGraph()
		def mapper = new GraphObjectMapper(g)
		mapper.toGraph(jon)
		
		
		Vertex personV
		
		personV = g.V('name', 'jon').model(Person.class).iterator().next()
		assert personV.name == 'jon'
		
		personV = g.V('make', 'ford').model(Person.class).iterator().next()
		assert personV.name == 'jon'
		
		Person person = mapper.fromGraph(personV, Person.class)
		assert person.name == 'jon'
		assert person.cars[0].make == 'ford'
		
		
		def carV = g.V('model', 'escape').model(Car.class).iterator().next()
		Car car = mapper.fromGraph(carV, Car.class)
		assert car.make == 'ford'
	}
}
