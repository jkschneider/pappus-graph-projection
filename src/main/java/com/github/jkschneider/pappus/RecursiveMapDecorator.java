package com.github.jkschneider.pappus;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

public class RecursiveMapDecorator {
	HashFunction md5 = Hashing.md5();
	
	@SuppressWarnings("unchecked")
	public long hash(Map<Object,Object> map, Class<?> c) {
		Hasher hasher = md5.newHasher();
		
		for(Entry<Object,Object> e : map.entrySet()) {
			Object val = e.getValue();
			
			if(Map.class.isAssignableFrom(val.getClass())) {
				hasher.putLong(hash((Map<Object,Object>) val, getChildType(e.getKey().toString(), c)));
			}
			else if(Collection.class.isAssignableFrom(val.getClass()))
				putHash((Collection<Object>) val, e.getKey().toString(), c, hasher);
			else if(Integer.class.isAssignableFrom(e.getClass()))
				hasher.putInt((Integer) val);
			else if(Long.class.isAssignableFrom(e.getClass()))
				hasher.putLong((Long) val);
			else if(Double.class.isAssignableFrom(e.getClass()))
				hasher.putDouble((Double) val);
			else if(Float.class.isAssignableFrom(e.getClass()))
				hasher.putFloat((Float) val);
			else
				hasher.putUnencodedChars(e.getValue().toString());
		}
		
		long hash = hasher.hash().asLong();
		map.put("_type", c);
		
		if(!Map.class.isAssignableFrom(c))
			map.put("_hash", (Long) hash);
		
		return hash;
	}
	
	@SuppressWarnings("unchecked")
	private void putHash(Collection<Object> list, String key, Class<?> c, Hasher hasher) {
		for(Object e : list) {
			if(Map.class.isAssignableFrom(e.getClass()))
				hasher.putLong(hash((Map<Object,Object>) e, getChildType(key, c)));
			
			// we don't support nested collections right now because of the hypergraph problem...
//			else if(Collection.class.isAssignableFrom(e.getClass())) {
//				putHash((Collection<Object>) e, hasher);
//			}
			
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
	
	// TODO cache the results of these lookups so there isn't so much reflection...
	private Class<?> getChildType(String field, Class<?> c) {
		try {
			Field f = c.getDeclaredField(field);
			if(f.getType().isArray())
				return f.getType().getComponentType();
			else if(Collection.class.isAssignableFrom(f.getType())) {
				ParameterizedType collectionType = (ParameterizedType) f.getGenericType();
				return (Class<?>) collectionType.getActualTypeArguments()[0];
			}
			return f.getType();
		} catch (NoSuchFieldException | SecurityException e) {
			throw new RuntimeException(e);
		}
	}
}
