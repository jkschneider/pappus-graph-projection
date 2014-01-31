package com.github.jkschneider.pappus

import org.junit.Test

class TestRecursiveMapHasher {
	RecursiveMapHasher hasher = new RecursiveMapHasher()
	
	@Test
	void nesting() {
		def m1 = [ field : 'f', nested: [ nestedField : 'f2']]
		def m2 = [ field : 'f']
		
		assert hasher.hash(m1) != hasher.hash(m2)
		
		def m3 = [ field : 'f', field2 : 'f2']

		// strategy does not just simply flatten values and hash them all together
		assert hasher.hash(m1) != hasher.hash(m3)
	}
}