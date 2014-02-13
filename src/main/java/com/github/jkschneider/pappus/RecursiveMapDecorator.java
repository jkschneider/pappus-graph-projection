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
	
	public long hash(Map<Object,Object> map, Class<?> trueMapType) {
		return hash(map, trueMapType, Object.class);
	}
	
	@SuppressWarnings("unchecked")
	protected long hash(Map<Object,Object> map, Class<?> trueMapType, Class<?> valueClass) {
		Hasher hasher = md5.newHasher();
		
		for(Entry<Object,Object> e : map.entrySet()) {
			Object val = e.getValue();
			
			if(Map.class.isAssignableFrom(trueMapType)) {
				// 'map' is a field of type map whose values may need to be recursed as well
				if(Map.class.isAssignableFrom(val.getClass()))
					hasher.putLong(hash((Map<Object,Object>) val, valueClass, Object.class));
				else
					hasher.putUnencodedChars(val.toString());
			}
			else if(Map.class.isAssignableFrom(val.getClass())) {
				String key = e.getKey().toString();
				hasher.putLong(hash((Map<Object,Object>) val, getChildType(key, trueMapType),
						getMapValueType(key, trueMapType)));
			}
			else if(Collection.class.isAssignableFrom(val.getClass()))
				putHash((Collection<Object>) val, getChildType(e.getKey().toString(), trueMapType), hasher);
			else if(Integer.class.isAssignableFrom(e.getClass()))
				hasher.putInt((Integer) val);
			else if(Long.class.isAssignableFrom(e.getClass()))
				hasher.putLong((Long) val);
			else if(Double.class.isAssignableFrom(e.getClass()))
				hasher.putDouble((Double) val);
			else if(Float.class.isAssignableFrom(e.getClass()))
				hasher.putFloat((Float) val);
			else
				hasher.putUnencodedChars(val.toString());
		}
		
		long hash = hasher.hash().asLong();
		
		if(!Map.class.isAssignableFrom(trueMapType)) {
			map.put("_type", trueMapType);
			map.put("_hash", (Long) hash);
		}
		
		return hash;
	}
	
	@SuppressWarnings("unchecked")
	private void putHash(Collection<Object> list, Class<?> c, Hasher hasher) {
		for(Object e : list) {
			if(Map.class.isAssignableFrom(e.getClass()))
				hasher.putLong(hash((Map<Object,Object>) e, c));
			
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
	
	// TODO cache the results of these lookups so there isn't so much reflection...
	private Class<?> getMapValueType(String field, Class<?> c) {
		try {
			Field f = c.getDeclaredField(field);
			if(Map.class.isAssignableFrom(f.getType())) {
				ParameterizedType collectionType = (ParameterizedType) f.getGenericType();
				return (Class<?>) collectionType.getActualTypeArguments()[1];
			}
			return null;
		} catch (NoSuchFieldException | SecurityException e) {
			throw new RuntimeException(e);
		}
	}
}
