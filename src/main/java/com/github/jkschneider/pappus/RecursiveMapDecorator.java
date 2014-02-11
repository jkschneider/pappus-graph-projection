package com.github.jkschneider.pappus;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

public class RecursiveMapDecorator {
	private final HashFunction md5 = Hashing.md5();
	private final Type[] objectMapTypes = new Type[] { Object.class, Object.class };
	
	public long hash(Map<Object,Object> map, Class<?> trueMapType) {
		return hash(map, trueMapType, objectMapTypes);
	}
	
	@SuppressWarnings("unchecked")
	protected long hash(Map<Object,Object> map, Class<?> fieldType, Type[] mapGenerics) {
		Hasher hasher = md5.newHasher();

		for(Entry<Object,Object> e : map.entrySet()) {
			Object val = e.getValue();
			
			if(isMap(fieldType)) {
				// 'map' is a field of type map whose values may need to be recursed as well
				for(Entry<Object, Object> e2 : map.entrySet()) {
					hasher.putUnencodedChars(e2.getKey().toString() + ":");
					
					if(isMap(e2.getValue().getClass())) {
						Map<Object, Object> valMap = (Map<Object, Object>) e2.getValue();
						hasher.putLong(hash(valMap, (Class<?>) mapGenerics[1], objectMapTypes));
					}
					else hasher.putUnencodedChars(e2.getValue().toString());
				}
			}
			else if(isMap(val.getClass())) {
				String key = e.getKey().toString();
				hasher.putLong(hash((Map<Object,Object>) val, getChildType(key, fieldType),
						getMapTypes(key, fieldType)));
			}
			else if(Collection.class.isAssignableFrom(val.getClass()))
				putHash((Collection<Object>) val, getChildType(e.getKey().toString(), fieldType), hasher);
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
		
		if(!isMap(fieldType)) {
			map.put("_type", fieldType);
			map.put("_hash", (Long) hash);
		}
		
		return hash;
	}
	
	@SuppressWarnings("unchecked")
	private void putHash(Collection<Object> list, Class<?> c, Hasher hasher) {
		for(Object e : list) {
			if(isMap(e.getClass()))
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
	private Type[] getMapTypes(String field, Class<?> c) {
		try {
			Field f = c.getDeclaredField(field);
			if(isMap(f.getType())) {
				ParameterizedType collectionType = (ParameterizedType) f.getGenericType();
				return collectionType.getActualTypeArguments();
			}
			return null;
		} catch (NoSuchFieldException | SecurityException e) {
			throw new RuntimeException(e);
		}
	}

	private boolean isMap(Type type) {
		return Map.class.isAssignableFrom((Class<?>) type);
	}
}
