package com.github.jkschneider.pappus

import java.util.Map

import com.tinkerpop.gremlin.groovy.console.ConsoleGroovy
import com.tinkerpop.gremlin.groovy.console.ConsoleIO
import com.tinkerpop.gremlin.groovy.console.ConsolePlugin

public class PappusConsolePlugin implements ConsolePlugin {
	public String getName() { return "pappus" }

	@Override
	public void pluginTo(final ConsoleGroovy groovy, final ConsoleIO io, final Map args) {
		groovy.execute("import com.github.jkschneider.pappus.GraphObjectMapper")
		groovy.execute("com.github.jkschneider.pappus.GremlinModelSteps.load()")
	}
}
