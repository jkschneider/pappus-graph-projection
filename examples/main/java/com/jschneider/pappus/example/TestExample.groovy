package com.jschneider.pappus.example;

import static org.junit.Assert.*

import org.joda.time.LocalDate
import org.junit.BeforeClass
import org.junit.Test

import com.fasterxml.jackson.datatype.joda.JodaModule
import com.jschneider.pappus.GraphObjectMapper
import com.jschneider.pappus.GremlinModelSteps
import com.tinkerpop.blueprints.Vertex
import com.tinkerpop.blueprints.impls.tg.TinkerGraph
import com.tinkerpop.gremlin.groovy.Gremlin

class TestExample {
	@BeforeClass
	static void before() {
		Gremlin.load()
		GremlinModelSteps.load()
	}
	
	@Test
	void simpleGraph() {
		def address = new Address(address1: '123 main st')
		def jon = new Person(name: 'jon', birthDate: new LocalDate(1983, 12, 5), address: address)
		def escape = new Car(make: 'ford', model: 'escape', year: 2011)
		jon.cars.add(escape)
		
		def g = new TinkerGraph()
		def mapper = new GraphObjectMapper(g)
		mapper.getObjectMapper().registerModule(new JodaModule())
		
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
	
	@Test
	void jacksonModules() {
		def g = new TinkerGraph()
		def mapper = new GraphObjectMapper(g)
		
		// augments Pappus to deal with Joda LocalDates 
		mapper.getObjectMapper().registerModule(new JodaModule())
		
		def birthDate = new LocalDate(1983, 12, 5)
		def jon = new Person(name: 'jon', birthDate: new LocalDate(1983,12,5))
		
		Vertex personV = mapper.toGraph(jon)
		def jonUnmapped = mapper.fromGraph(personV, Person.class)
		assert jonUnmapped.birthDate == birthDate
	} 
	
	@Test
	void hashEquals() {
		def g = new TinkerGraph()
		def mapper = new GraphObjectMapper(g)
		
		def jon = new Person(name: 'jon')
		def bob = new Person(name: 'bob')
		
		def ford = new Car(make: 'ford')
		jon.cars.add(ford)
		bob.cars.add(ford)
		
		mapper.toGraph(jon)
		mapper.toGraph(bob)
		
		assert g.vertices.count { 1 } == 3
	}
	
	@Test
	void hashNotEquals() {
		def g = new TinkerGraph()
		def mapper = new GraphObjectMapper(g)
		
		def jon = new Person(name: 'jon')
		def bob = new Person(name: 'bob')
		
		def ford1 = new Car(make: 'ford', garagingLocation: new Address(city: 'columbia'))
		def ford2 = new Car(make: 'ford', garagingLocation: new Address(city: 'st. louis'))
		jon.cars.add(ford1)
		bob.cars.add(ford2)
		
		mapper.toGraph(jon)
		mapper.toGraph(bob)
		
		assert g.vertices.count { 1 } == 6
	}
	
	@Test
	void testHistory() {
		def g = new TinkerGraph()
		def mapper = new GraphObjectMapper(g)
		
		def jon = new Person(name: 'jon', version: 1)
		def bob = new Person(name: 'bob', version: 1)
		
		def ford = new Car(make: 'ford')
		jon.cars.add(ford)
		bob.cars.add(ford)
		
		mapper.toGraph(jon)
		mapper.toGraph(bob)
		
		def dodge = new Car(make: 'dodge')
		jon.cars.add(dodge)
		jon.version = 2
		
		mapper.toGraph(jon)
		assert g.vertices.count { 1 } == 5

		def ownersMap = g.V('make', 'ford').model(Person.class)
			.groupBy{it['name']}{it}{ it.max{ it['version'] } }.cap.next()
		def persons = mapper.fromGraph(ownersMap, Person.class)
		assert persons.size == 2 && persons.collect { it.name }.sort() == ['bob', 'jon']
	}
}
