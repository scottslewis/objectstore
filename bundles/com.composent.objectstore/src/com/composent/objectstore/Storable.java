/*******************************************************************************
 * Copyright (c) 2014 Composent, Inc. and others. All rights reserved. 
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package com.composent.objectstore;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.composent.objectstore.IObjectStore.ConsistencyLevel;
import com.composent.objectstore.IObjectStore.Util;

public class Storable extends IdentifiedObject {

	private final String className;

	private ConsistencyLevel readConsistencyLevel;
	private ConsistencyLevel writeConsistencyLevel;

	private boolean ifNotExists;

	public Storable(String className) {
		Util.checkArgNotNull(className, "className");
		this.className = className;
	}

	public String getClassName() {
		return this.className;
	}

	public ConsistencyLevel getReadConsistencyLevel() {
		return this.readConsistencyLevel;
	}

	public void setReadConsistencyLevel(ConsistencyLevel level) {
		this.readConsistencyLevel = level;
	}

	public ConsistencyLevel getWriteConsistencyLevel() {
		return this.writeConsistencyLevel;
	}

	public void setWriteConsistencyLevel(ConsistencyLevel level) {
		this.writeConsistencyLevel = level;
	}

	public void createObjectTable(IObjectStore store, boolean ifNotExists)
			throws StoreException {
		// First get metadata for className
		store.createObjectTable(getClassName(), ifNotExists);
		createFields(store, ifNotExists);
	}

	public Storable storeTo(IObjectStore store) throws StoreException {
		StoreObject so = store.createStoreObject(getClassName());
		so.setId(getId());
		so.setLastModifiedAt(getLastModifiedAt());
		so.setIfNotExists(isIfNotExists());
		so.setConsistencyLevel(getWriteConsistencyLevel());
		storeFields(so);
		so.store();
		setId(so.getId());
		setLastModifiedAt(so.getLastModifiedAt());
		return this;
	}

	public void deleteFrom(IObjectStore store) throws StoreException {
		UUID id = getId();
		if (id == null)
			throw new StoreException("id cannot be null to delete");
		store.delete(getClassName(), id, getWriteConsistencyLevel());
	}

	public StoreObjectQuery createQuery(IObjectStore store)
			throws StoreException {
		return store.createQuery(getClassName());
	}

	public Collection<? extends Storable> reviveAll(IObjectStore store)
			throws StoreException {
		return revive(createQuery(store));
	}

	public Collection<? extends Storable> revive(StoreObjectQuery query)
			throws StoreException {
		query.setConsistencyLevel(getReadConsistencyLevel());
		Collection<StoreObject> sos = query.execute();
		List<Storable> results = new ArrayList<Storable>();
		for (StoreObject so : sos) {
			Storable storable = createInstanceOfType(getClass(), getClassName());
			storable.reviveFrom(so);
			results.add(storable);
		}
		return results;
	}

	public Storable revive(IObjectStore store, UUID id) throws StoreException {
		StoreObjectQuery query = store.createQuery(getClassName(), id);
		Collection<StoreObject> sos = query.execute();
		if (sos.isEmpty())
			throw new StoreException("no store objects found for id=" + id);
		return reviveFrom(sos.iterator().next());
	}

	protected void createFields(IObjectStore store, boolean ifNotExists)
			throws StoreException {
		// subclasses may override to create fields
	}

	protected void storeFields(StoreObject storeObject) throws StoreException {
		// subclasses may override to store fields
	}

	protected void reviveFields(StoreObject storeObject) throws StoreException {
		// subclasses may override to revive fields
	}

	protected <T extends Storable> T createAndReviveStorable(
			IObjectStore store, Class<T> type, UUID id) throws StoreException {
		T result = createInstanceOfType(type, null);
		result.revive(store, id);
		return result;
	}

	protected void createPrimitiveField(IObjectStore store, Class<?> fieldType,
			String fieldName, boolean ifNotExists) throws StoreException {
		store.addPrimitiveField(getClassName(), fieldName, fieldType,
				ifNotExists);
	}

	protected <T, S extends Storable> List<S> convertListItemsToStorables(
			List<T> items, Class<S> storableType) throws StoreException {
		if (items == null)
			return null;
		List<S> results = new ArrayList<S>();
		for (T i : items)
			results.add(createInstanceOfType(storableType, i));
		return results;
	}

	protected void createPrimitiveFields(IObjectStore store,
			Class<?>[] fieldTypes, String[] fieldNames, boolean ifNotExists)
			throws StoreException {
		if (fieldNames.length != fieldTypes.length)
			throw new StoreException(
					"fieldNames and fieldTypes must have same length");
		for (int i = 0; i < fieldNames.length; i++)
			createPrimitiveField(store, fieldTypes[i], fieldNames[i],
					ifNotExists);
	}

	@SuppressWarnings("unchecked")
	protected <T> boolean storePrimitiveField(StoreObject storeObject,
			String fieldName, T value) {
		if (value == null)
			return false;
		storeObject.putPrimitive(fieldName, (Class<T>) value.getClass(), value);
		return true;
	}

	protected <T> T revivePrimitiveField(StoreObject storeObject,
			Class<T> fieldType, String fieldName) {
		return storeObject.getPrimitive(fieldName, fieldType);
	}

	protected <T extends Storable> void createStorableField(IObjectStore store,
			String fieldName, boolean ifNotExists) throws StoreException {
		store.addPrimitiveField(getClassName(), fieldName, UUID.class,
				ifNotExists);
	}

	protected <T extends Storable> boolean storeStorableField(
			StoreObject storeObject, String fieldName, T fieldValue)
			throws StoreException {
		if (fieldValue == null)
			return false;
		storeObject.put(fieldName, fieldValue.storeTo(storeObject.getStore())
				.getId());
		return true;
	}

	protected <T extends Storable> T reviveStorableField(
			StoreObject storeObject, Class<T> fieldType, String fieldName)
			throws StoreException {
		UUID id = storeObject.getUUID(fieldName);
		return (id != null) ? createAndReviveStorable(storeObject.getStore(),
				fieldType, id) : null;
	}

	protected void createListField(IObjectStore store, Class<?> elementType,
			String fieldName, boolean ifNotExists) throws StoreException {
		store.addCollectionField(getClassName(), fieldName, List.class,
				getStorableOrPrimitiveType(elementType), null, ifNotExists);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected <T> boolean storeListField(StoreObject storeObject,
			Class<T> elementType, String fieldName, List<T> fieldValue)
			throws StoreException {
		if (fieldValue == null || fieldValue.isEmpty())
			return false;
		boolean storable = isStorable(elementType);
		List result = new ArrayList();
		for (T od : fieldValue) {
			Object item = (storable) ? ((Storable) od).storeTo(
					storeObject.getStore()).getId() : od;
			result.add(item);
		}
		storeObject.put(fieldName, getStorableOrPrimitiveType(elementType),
				result);
		return true;
	}

	@SuppressWarnings("unchecked")
	protected <T> List<T> reviveListField(StoreObject storeObject,
			Class<T> elementType, String fieldName) throws StoreException {
		@SuppressWarnings({ "rawtypes" })
		List items = storeObject.getList(fieldName);
		if (items == null)
			return null;
		List<T> results = new ArrayList<T>();
		boolean storable = isStorable(elementType);
		for (Object o : items) {
			T item = (T) ((storable) ? createAndReviveStorable(
					storeObject.getStore(),
					(Class<? extends Storable>) elementType, (UUID) o) : o);
			results.add(item);
		}
		return results.isEmpty() ? null : results;
	}

	protected void createSetField(IObjectStore store, Class<?> elementType,
			String fieldName, boolean ifNotExists) throws StoreException {
		store.addCollectionField(getClassName(), fieldName, Set.class,
				getStorableOrPrimitiveType(elementType), null, ifNotExists);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected <T> boolean storeSetField(StoreObject storeObject,
			Class<T> elementType, String fieldName, Set<T> fieldValue)
			throws StoreException {
		if (fieldValue == null || fieldValue.isEmpty())
			return false;
		boolean storable = isStorable(elementType);
		Set result = new HashSet();
		for (T od : fieldValue) {
			Object item = (storable) ? ((Storable) od).storeTo(
					storeObject.getStore()).getId() : od;
			result.add(item);
		}
		storeObject.put(fieldName, getStorableOrPrimitiveType(elementType),
				result);
		return true;
	}

	@SuppressWarnings("unchecked")
	protected <T> Set<T> reviveSetField(StoreObject storeObject,
			Class<T> elementType, String fieldName) throws StoreException {
		@SuppressWarnings({ "rawtypes" })
		Set items = storeObject.getSet(fieldName);
		if (items == null)
			return null;
		Set<T> results = new HashSet<T>();
		boolean storable = isStorable(elementType);
		for (Object o : items) {
			T i = (T) ((storable) ? createAndReviveStorable(
					storeObject.getStore(),
					(Class<? extends Storable>) elementType, (UUID) o) : o);
			results.add(i);
		}
		return results.isEmpty() ? null : results;
	}

	protected void createMapField(IObjectStore store, Class<?> keyType,
			Class<?> valueType, String fieldName, boolean ifNotExists)
			throws StoreException {
		store.addCollectionField(getClassName(), fieldName, Map.class,
				getStorableOrPrimitiveType(keyType),
				getStorableOrPrimitiveType(valueType), ifNotExists);
	}

	@SuppressWarnings("unchecked")
	protected <K, V> boolean storeMapField(StoreObject storeObject,
			Class<K> keyType, Class<V> valueType, String fieldName,
			Map<K, V> fieldValue) {
		if (fieldValue == null || fieldValue.isEmpty())
			return false;
		boolean storableKey = isStorable(keyType);
		boolean storableValue = isStorable(valueType);
		@SuppressWarnings("rawtypes")
		Map mapToStore = new HashMap<Object, Object>();
		for (K key : fieldValue.keySet()) {
			V value = fieldValue.get(key);
			mapToStore.put((storableKey) ? ((Storable) key).getId() : key,
					(storableValue) ? ((Storable) value).getId() : value);
		}
		storeObject.put(fieldName, getStorableOrPrimitiveType(keyType),
				getStorableOrPrimitiveType(valueType), mapToStore);
		return true;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected <K, V> Map<K, V> reviveMapField(StoreObject storeObject,
			Class<K> keyType, Class<V> valueType, String fieldName)
			throws StoreException {
		Map dataMap = storeObject.getMap(fieldName);
		if (dataMap == null)
			return null;
		Map<K, V> results = new HashMap<K, V>();
		boolean storableKey = isStorable(keyType);
		boolean storableValue = isStorable(valueType);
		IObjectStore store = storeObject.getStore();
		for (Object key : dataMap.keySet()) {
			Object value = dataMap.get(key);
			K k = (K) ((storableKey) ? createAndReviveStorable(store,
					(Class<? extends Storable>) keyType, (UUID) key) : key);
			V v = (V) ((storableValue) ? createAndReviveStorable(store,
					(Class<? extends Storable>) valueType, (UUID) key) : value);
			results.put(k, v);
		}
		return results.isEmpty() ? null : results;
	}

	protected Storable reviveFrom(StoreObject storeObject)
			throws StoreException {
		setId(storeObject.getId());
		setLastModifiedAt(storeObject.getLastModifiedAt());
		storeObject.setConsistencyLevel(getReadConsistencyLevel());
		reviveFields(storeObject);
		return this;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((className == null) ? 0 : className.hashCode());
		UUID id = getId();
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Storable other = (Storable) obj;
		if (className == null) {
			if (other.className != null)
				return false;
		} else if (!className.equals(other.className))
			return false;
		UUID id = getId();
		UUID otherId = other.getId();
		if (id == null) {
			if (otherId != null)
				return false;
		} else if (!id.equals(otherId))
			return false;
		return true;
	}

	private boolean isStorable(Class<?> clazz) {
		return Storable.class.isAssignableFrom(clazz);
	}

	private Class<?> getStorableOrPrimitiveType(Class<?> clazz) {
		if (isStorable(clazz))
			return UUID.class;
		return clazz;
	}

	public boolean isIfNotExists() {
		return ifNotExists;
	}

	public void setIfNotExists(boolean ifNotExists) {
		this.ifNotExists = ifNotExists;
	}

	public Collection<UUID> retrieveIds(IObjectStore store, Integer limit)
			throws StoreException {
		return store.retrieveIds(getClassName(), limit,
				getReadConsistencyLevel());
	}

	@SuppressWarnings("unchecked")
	protected <T extends Storable, A> T createInstanceOfType(Class<T> type,
			A arg) throws StoreException {
		try {
			Constructor<T> c = null;
			boolean nullConstructor = (arg == null);
			try {
				if (!nullConstructor)
					c = type.getConstructor((Class<A>) arg.getClass());
			} catch (Exception e) {
				nullConstructor = true;
				c = type.getConstructor();
			}
			if (c == null) {
				nullConstructor = true;
				c = type.getConstructor();
			}
			c.setAccessible(true);
			return (nullConstructor) ? c.newInstance() : c.newInstance(arg);
		} catch (Exception e) {
			throw new StoreException(
					"Could not create Storable instance of type=" + type, e);
		}
	}

}
