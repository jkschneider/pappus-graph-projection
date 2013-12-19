package com.jschneider.gom

import com.tinkerpop.blueprints.Vertex
import com.tinkerpop.gremlin.groovy.Gremlin
import com.tinkerpop.pipes.Pipe

class GremlinDomainSteps {
	static void load() {
		new GremlinDomainSteps().loadNonStatic()
	}
	
	private void loadNonStatic() {
		Gremlin.defineStep('domain', [Vertex,Pipe], { Class<?> clazz -> _().as('_domainX').in.loop('_domainX'){it.object['_type'] != clazz.getName()}.dedup() })
	}
}
