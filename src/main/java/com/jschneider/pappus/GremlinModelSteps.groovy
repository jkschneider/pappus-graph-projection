package com.jschneider.pappus

import com.tinkerpop.blueprints.Vertex
import com.tinkerpop.gremlin.groovy.Gremlin
import com.tinkerpop.pipes.Pipe

class GremlinModelSteps {
	static void load() {
		new GremlinModelSteps().loadNonStatic()
	}
	
	private void loadNonStatic() {
		Gremlin.defineStep('model', [Vertex,Pipe], { Class<?> clazz -> _().copySplit(_().has('_type', clazz.getName()), _().as('_domainX').in.loop('_domainX'){it.object['_type'] != clazz.getName()}.dedup()).fairMerge })
	}
}
