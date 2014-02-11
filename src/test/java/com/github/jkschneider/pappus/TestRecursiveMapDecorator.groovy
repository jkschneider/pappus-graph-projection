package com.github.jkschneider.pappus

import org.junit.Test

class TestRecursiveMapDecorator {
	RecursiveMapDecorator hasher = new RecursiveMapDecorator()
	
	class A {
		String field
		String field2
		Map<String, Integer> map
		Map<String, B> bMap
		B nested
	}
	
	class B {
		String nestedField
	}
	
	@Test
	void nesting() {
		def m1 = [field: 'f', nested: [nestedField: 'f2']]
		def m2 = [field: 'f']
		
		assert hasher.hash(m1, A.class) != hasher.hash(m2, A.class)
		
		def m3 = [field: 'f', field2: 'f2']

		// strategy does not just simply flatten values and hash them all together
		assert hasher.hash(m1, A.class) != hasher.hash(m3, A.class)
	}
	
	@Test
	void type() {
		def m1 = [field: 'f', nested: [nestedField: 'f2']]
		
		hasher.hash(m1, A.class)
		assert m1['_type'] == A.class
		assert m1['nested']['_type'] == B.class
	}
	
	@Test
	void hash() {
		def m1 = [field: 'f', nested: [nestedField: 'f2']]
		
		hasher.hash(m1, A.class)
		assert m1['_hash']
		assert m1['nested']['_hash']
	}
	
	@Test
	void mapsDoNotGetAssignedAHash() {
		def m1 = [map : ['a' : 1]]
		
		hasher.hash(m1, A.class)
		assert m1['map']['_hash'] == null
	}
	
	@Test
	void objectsThatDifferOnlyByMapContentsHaveDifferentHashes() {
		def m1 = [map : ['a' : 1]]
		def m2 = [map : ['a' : 2]]
		
		assert hasher.hash(m1, A.class) != hasher.hash(m2, A.class)
		
		m1 = [map: ['a', 1]]
		m2 = [map: ['a', 1]]
		assert hasher.hash(m1, A.class) == hasher.hash(m2, A.class)
	}
	
	@Test
	void mapValueTypesAreInferred() {
		def m1 = [bMap : ['b1' : [nestedField : 'b']]]
		assert m1.bMap['b1']['_type'] == B.class
	}
}