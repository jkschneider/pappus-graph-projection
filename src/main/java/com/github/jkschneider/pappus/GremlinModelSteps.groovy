package com.github.jkschneider.pappus

import com.tinkerpop.blueprints.Vertex
import com.tinkerpop.gremlin.groovy.Gremlin
import com.tinkerpop.pipes.Pipe

class GremlinModelSteps {
	static GremlinModelSteps load() {
		def steps = new GremlinModelSteps()
		steps.loadNonStatic()
		return steps
	}
	
	private void loadNonStatic() {
		Gremlin.defineStep('model', [Vertex,Pipe], { Class<?> clazz -> 
			def start = _()
			start
				.copySplit(
					_().has('_type', clazz.getName()), 
					_()
						.as('_domainX')
						.in
						.loop('_domainX'){ it.object['_type'] != clazz.getName() }
						.dedup()
				)
				.fairMerge
		})
	}
}
