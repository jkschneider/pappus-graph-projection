package com.github.jkschneider.pappus;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

public class RecursiveMapHasher {
	HashFunction md5 = Hashing.md5();
	
	@SuppressWarnings("unchecked")
	public long hash(Map<Object,Object> map) {
		Hasher hasher = md5.newHasher();
		
		for(Entry<Object,Object> e : map.entrySet()) {
			if(Map.class.isAssignableFrom(e.getValue().getClass()))
				hasher.putLong(hash((Map<Object,Object>) e.getValue()));
			else if(Collection.class.isAssignableFrom(e.getValue().getClass()))
				putHash((Collection<Object>) e.getValue(), hasher);
			else if(Number.class.isAssignableFrom(e.getClass()))
				hasher.putLong((Long) e.getValue());
			else
				hasher.putUnencodedChars(e.getValue().toString());
		}
		
		long hash = hasher.hash().asLong();
		map.put("_hash", (Long) hash);
		return hash;
	}
	
	@SuppressWarnings("unchecked")
	private void putHash(Collection<Object> list, Hasher hasher) {
		for(Object e : list) {
			if(Map.class.isAssignableFrom(e.getClass()))
				hasher.putLong(hash((Map<Object,Object>) e));
			else if(Collection.class.isAssignableFrom(e.getClass()))
				putHash((Collection<Object>) e, hasher);
			else if(Integer.class.isAssignableFrom(e.getClass()))
				hasher.putInt((Integer) e);
			else if(Long.class.isAssignableFrom(e.getClass()))
				hasher.putLong((Long) e);
			else if(Double.class.isAssignableFrom(e.getClass()))
				hasher.putDouble((Double) e);
			else if(Float.class.isAssignableFrom(e.getClass()))
				hasher.putFloat((Float) e);
			else
				hasher.putUnencodedChars(e.toString());
		}
	}
}
