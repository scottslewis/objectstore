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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.composent.objectstore.IObjectStore.CollectionValue;
import com.composent.objectstore.IObjectStore.ConsistencyLevel;
import com.composent.objectstore.IObjectStore.ObjectStoreItem;
import com.composent.objectstore.IObjectStore.Util;
import com.composent.objectstore.StoreObjectMetadata.FieldMetadata;

public abstract class StoreObject extends IdentifiedObject {

	public static final String ID_NAME = "id";
	public static final String LASTMODIFIEDDATE_NAME = "lastModifiedAt";

	private final IObjectStore store;
	private final StoreObjectMetadata metadata;

	private ConsistencyLevel consistencyLevel;
	private Collection<ObjectStoreItem> ifNotExistsItems;

	protected StoreObject(IObjectStore store, StoreObjectMetadata metadata,
			UUID id) {
		Util.checkArgNotNull(store, "store");
		this.store = store;
		Util.checkArgNotNull(metadata, "metadata");
		this.metadata = metadata;
		setId(id);
	}

	protected StoreObject(IObjectStore store, StoreObjectMetadata otm) {
		this(store, otm, null);
	}

	public IObjectStore getStore() {
		return store;
	}

	public void setConsistencyLevel(ConsistencyLevel level) {
		this.consistencyLevel = level;
	}

	public ConsistencyLevel getConsistencyLevel() {
		return this.consistencyLevel;
	}

	public String getClassName() {
		return this.metadata.getClassName();
	}

	public synchronized StoreObjectMetadata getMetadata() {
		return this.metadata;
	}

	protected Map<String, Map<String, ?>> allFieldsMap = new HashMap<String, Map<String, ?>>();

	protected Map<String, String> stringFieldMap;
	protected Map<String, ByteBuffer> bytesFieldMap;
	protected Map<String, Long> longFieldMap;
	protected Map<String, Integer> integerFieldMap;
	protected Map<String, Date> dateFieldMap;
	protected Map<String, Boolean> booleanFieldMap;
	protected Map<String, Float> floatFieldMap;
	protected Map<String, Double> doubleFieldMap;

	protected Map<String, BigDecimal> bigDecimalFieldMap;
	protected Map<String, UUID> uuidFieldMap;
	protected Map<String, BigInteger> bigIntegerFieldMap;
	protected Map<String, InetAddress> inetAddressFieldMap;

	protected Map<String, CollectionValue> mapFieldMap;
	protected Map<String, CollectionValue> listFieldMap;
	protected Map<String, CollectionValue> setFieldMap;

	public synchronized Collection<String> getKeys() {
		return allFieldsMap.keySet();
	}

	protected FieldMetadata checkField(String key) {
		FieldMetadata field = getMetadata().getField(key);
		if (field == null)
			throw new IllegalArgumentException("key=" + key
					+ " not found as field");
		return field;
	}

	protected void validatePrimitive(String key, Object value) {
		Util.checkArgNotNull(key, "key");
		FieldMetadata field = checkField(key);
		if (value != null)
			Util.checkTypesCompatible(field.getType(), value.getClass());
	}

	@SuppressWarnings("rawtypes")
	protected void validateMap(String key, Class<?> keyType,
			Class<?> valueType, Map value) {
		Util.checkArgNotNull(key, "key");
		Util.checkArgNotNull(value, "value");
		FieldMetadata field = checkField(key);
		if (!field.isMap())
			throw new IllegalArgumentException("key=" + key + " not Map.  Is "
					+ field.getType());
		Util.checkTypesCompatible(field.getFirstElementType(), keyType);
		Util.checkTypesCompatible(field.getSecondElementType(), valueType);
	}

	@SuppressWarnings("rawtypes")
	protected void validateList(String key, Class<?> elementType, List value) {
		Util.checkArgNotNull(key, "key");
		Util.checkArgNotNull(value, "value");
		FieldMetadata field = checkField(key);
		if (!field.isList())
			throw new IllegalArgumentException("key=" + key + " not List.  Is "
					+ field.getType());
		Util.checkTypesCompatible(field.getFirstElementType(), elementType);
	}

	@SuppressWarnings("rawtypes")
	protected void validateSet(String key, Class<?> elementType, Set value) {
		Util.checkArgNotNull(key, "key");
		Util.checkArgNotNull(value, "value");
		FieldMetadata field = checkField(key);
		if (!field.isSet())
			throw new IllegalArgumentException("key=" + key + " not List.  Is "
					+ field.getType());
		Util.checkTypesCompatible(field.getFirstElementType(), elementType);
	}

	@SuppressWarnings("rawtypes")
	private void checkMapType(String key, Map typeMap, Map fieldMap) {
		if (!typeMap.equals(fieldMap))
			throw new IllegalArgumentException("key=" + key
					+ " has already been used to store value of different type");
	}

	private String put0(String key, String value, boolean raw) {
		@SuppressWarnings("rawtypes")
		Map typeMap = allFieldsMap.get(key);
		if (typeMap == null) {
			if (stringFieldMap == null)
				stringFieldMap = new HashMap<String, String>();
			allFieldsMap.put(key, stringFieldMap);
		} else
			checkMapType(key, typeMap, this.stringFieldMap);
		return stringFieldMap.put(key, value);
	}

	public synchronized String put(String key, String value) {
		validatePrimitive(key, value);
		return put0(key, value, false);
	}

	public synchronized String getString(String key) {
		if (stringFieldMap == null)
			return null;
		return stringFieldMap.get(key);
	}

	private ByteBuffer put0(String key, ByteBuffer value, boolean raw) {
		@SuppressWarnings("rawtypes")
		Map typeMap = allFieldsMap.get(key);
		if (typeMap == null) {
			if (bytesFieldMap == null)
				bytesFieldMap = new HashMap<String, ByteBuffer>();
			allFieldsMap.put(key, bytesFieldMap);
		} else
			checkMapType(key, typeMap, this.bytesFieldMap);
		return bytesFieldMap.put(key, value);
	}

	public synchronized ByteBuffer put(String key, ByteBuffer value) {
		validatePrimitive(key, value);
		return put0(key, value, false);
	}

	public synchronized ByteBuffer getBytes(String key) {
		if (bytesFieldMap == null)
			return null;
		return bytesFieldMap.get(key);
	}

	private Long put0(String key, Long value, boolean raw) {
		@SuppressWarnings("rawtypes")
		Map typeMap = allFieldsMap.get(key);
		if (typeMap == null) {
			if (longFieldMap == null)
				longFieldMap = new HashMap<String, Long>();
			allFieldsMap.put(key, longFieldMap);
		} else
			checkMapType(key, typeMap, this.longFieldMap);
		return longFieldMap.put(key, value);
	}

	public synchronized Long put(String key, Long value) {
		validatePrimitive(key, value);
		return put0(key, value, false);
	}

	public synchronized Long getLong(String key) {
		if (longFieldMap == null)
			return 0L;
		return longFieldMap.get(key);
	}

	private Integer put0(String key, Integer value, boolean raw) {
		@SuppressWarnings("rawtypes")
		Map typeMap = allFieldsMap.get(key);
		if (typeMap == null) {
			if (integerFieldMap == null)
				integerFieldMap = new HashMap<String, Integer>();
			allFieldsMap.put(key, integerFieldMap);
		} else
			checkMapType(key, typeMap, this.integerFieldMap);
		return integerFieldMap.put(key, value);
	}

	public synchronized Integer put(String key, Integer value) {
		validatePrimitive(key, value);
		return put0(key, value, false);
	}

	public synchronized Integer getInt(String key) {
		if (integerFieldMap == null)
			return 0;
		return integerFieldMap.get(key);
	}

	private Date put0(String key, Date value, boolean raw) {
		@SuppressWarnings("rawtypes")
		Map typeMap = allFieldsMap.get(key);
		if (typeMap == null) {
			if (dateFieldMap == null)
				dateFieldMap = new HashMap<String, Date>();
			allFieldsMap.put(key, dateFieldMap);
		} else
			checkMapType(key, typeMap, this.dateFieldMap);
		return dateFieldMap.put(key, value);
	}

	public synchronized Date put(String key, Date value) {
		validatePrimitive(key, value);
		return put0(key, value, false);
	}

	public synchronized Date getDate(String key) {
		if (dateFieldMap == null)
			return null;
		return dateFieldMap.get(key);
	}

	private Boolean put0(String key, Boolean value, boolean raw) {
		@SuppressWarnings("rawtypes")
		Map typeMap = allFieldsMap.get(key);
		if (typeMap == null) {
			if (booleanFieldMap == null)
				booleanFieldMap = new HashMap<String, Boolean>();
			allFieldsMap.put(key, booleanFieldMap);
		} else
			checkMapType(key, typeMap, this.booleanFieldMap);
		return booleanFieldMap.put(key, value);
	}

	public synchronized Boolean put(String key, Boolean value) {
		validatePrimitive(key, value);
		return put0(key, value, false);
	}

	public synchronized Boolean getBoolean(String key) {
		if (booleanFieldMap == null)
			return false;
		return booleanFieldMap.get(key);
	}

	private Float put0(String key, Float value, boolean raw) {
		@SuppressWarnings("rawtypes")
		Map typeMap = allFieldsMap.get(key);
		if (typeMap == null) {
			if (floatFieldMap == null)
				floatFieldMap = new HashMap<String, Float>();
			allFieldsMap.put(key, floatFieldMap);
		} else
			checkMapType(key, typeMap, this.floatFieldMap);
		return floatFieldMap.put(key, value);
	}

	public synchronized Float put(String key, Float value) {
		validatePrimitive(key, value);
		return put0(key, value, false);
	}

	public synchronized Float getFloat(String key) {
		if (floatFieldMap == null)
			return 0f;
		return floatFieldMap.get(key);
	}

	private Double put0(String key, Double value, boolean raw) {
		@SuppressWarnings("rawtypes")
		Map typeMap = allFieldsMap.get(key);
		if (typeMap == null) {
			if (doubleFieldMap == null)
				doubleFieldMap = new HashMap<String, Double>();
			allFieldsMap.put(key, doubleFieldMap);
		} else
			checkMapType(key, typeMap, this.doubleFieldMap);
		return doubleFieldMap.put(key, value);

	}

	public synchronized Double put(String key, Double value) {
		validatePrimitive(key, value);
		return put0(key, value, false);
	}

	public synchronized Double getDouble(String key) {
		if (doubleFieldMap == null)
			return 0d;
		return doubleFieldMap.get(key);
	}

	private UUID put0(String key, UUID value, boolean raw) {
		@SuppressWarnings("rawtypes")
		Map typeMap = allFieldsMap.get(key);
		if (typeMap == null) {
			if (uuidFieldMap == null)
				uuidFieldMap = new HashMap<String, UUID>();
			allFieldsMap.put(key, uuidFieldMap);
		} else
			checkMapType(key, typeMap, this.uuidFieldMap);
		return uuidFieldMap.put(key, value);
	}

	public synchronized UUID put(String key, UUID value) {
		validatePrimitive(key, value);
		return put0(key, value, false);
	}

	public synchronized UUID getUUID(String key) {
		if (uuidFieldMap == null)
			return null;
		return uuidFieldMap.get(key);
	}

	public synchronized Object get(String key) {
		Map<?, ?> typeMap = allFieldsMap.get(key);
		if (typeMap == null)
			return null;
		return typeMap.get(key);
	}

	private BigInteger put0(String key, BigInteger value, boolean raw) {
		@SuppressWarnings("rawtypes")
		Map typeMap = allFieldsMap.get(key);
		if (typeMap == null) {
			if (bigIntegerFieldMap == null)
				bigIntegerFieldMap = new HashMap<String, BigInteger>();
			allFieldsMap.put(key, bigIntegerFieldMap);
		} else
			checkMapType(key, typeMap, this.bigIntegerFieldMap);
		return bigIntegerFieldMap.put(key, value);
	}

	public synchronized BigInteger put(String key, BigInteger value) {
		validatePrimitive(key, value);
		return put0(key, value, false);
	}

	public synchronized BigInteger getBigInteger(String key) {
		if (bigIntegerFieldMap == null)
			return BigInteger.ZERO;
		return bigIntegerFieldMap.get(key);
	}

	private BigDecimal put0(String key, BigDecimal value, boolean raw) {
		@SuppressWarnings("rawtypes")
		Map typeMap = allFieldsMap.get(key);
		if (typeMap == null) {
			if (bigDecimalFieldMap == null)
				bigDecimalFieldMap = new HashMap<String, BigDecimal>();
			allFieldsMap.put(key, bigDecimalFieldMap);
		} else
			checkMapType(key, typeMap, this.bigDecimalFieldMap);
		return bigDecimalFieldMap.put(key, value);
	}

	public synchronized BigDecimal put(String key, BigDecimal value) {
		validatePrimitive(key, value);
		return put0(key, value, false);
	}

	public synchronized BigDecimal getDecimal(String key) {
		if (bigDecimalFieldMap == null)
			return BigDecimal.ZERO;
		return bigDecimalFieldMap.get(key);
	}

	private InetAddress put0(String key, InetAddress value, boolean raw) {
		@SuppressWarnings("rawtypes")
		Map typeMap = allFieldsMap.get(key);
		if (typeMap == null) {
			if (inetAddressFieldMap == null)
				inetAddressFieldMap = new HashMap<String, InetAddress>();
			allFieldsMap.put(key, inetAddressFieldMap);
		} else
			checkMapType(key, typeMap, this.inetAddressFieldMap);
		return inetAddressFieldMap.put(key, value);
	}

	public synchronized InetAddress put(String key, InetAddress value) {
		validatePrimitive(key, value);
		return put0(key, value, false);
	}

	public synchronized InetAddress getInetAddress(String key) {
		if (inetAddressFieldMap == null)
			return null;
		return inetAddressFieldMap.get(key);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private CollectionValue put0(String key, Class<?> keyType,
			Class<?> valueType, Map map, boolean raw) {
		CollectionValue cv = CollectionValue.map(keyType, valueType, map);
		Map typeMap = allFieldsMap.get(key);
		if (typeMap == null) {
			if (mapFieldMap == null)
				mapFieldMap = new HashMap();
			allFieldsMap.put(key, mapFieldMap);
		} else
			checkMapType(key, typeMap, this.mapFieldMap);
		return mapFieldMap.put(key, cv);
	}

	@SuppressWarnings({ "rawtypes" })
	public synchronized CollectionValue put(String key, Class<?> keyType,
			Class<?> valueType, Map map) {
		validateMap(key, keyType, valueType, map);
		return put0(key, keyType, valueType, map, false);
	}

	@SuppressWarnings("rawtypes")
	public synchronized Map getMap(String key) {
		if (mapFieldMap == null)
			return null;
		CollectionValue cv = mapFieldMap.get(key);
		return (cv == null) ? null : (Map) cv.getValue();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private CollectionValue put0(String key, Class<?> elementType, List list,
			boolean raw) {
		CollectionValue cv = CollectionValue.list(elementType, list);
		Map typeMap = allFieldsMap.get(key);
		if (typeMap == null) {
			if (listFieldMap == null)
				listFieldMap = new HashMap();
			allFieldsMap.put(key, listFieldMap);
		} else
			checkMapType(key, typeMap, this.listFieldMap);
		return listFieldMap.put(key, cv);
	}

	@SuppressWarnings({ "rawtypes" })
	public synchronized CollectionValue put(String key, Class<?> elementType,
			List list) {
		validateList(key, elementType, list);
		return put0(key, elementType, list, false);
	}

	@SuppressWarnings("rawtypes")
	public synchronized List getList(String key) {
		if (listFieldMap == null)
			return null;
		CollectionValue cv = listFieldMap.get(key);
		return (cv == null) ? null : (List) cv.getValue();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private CollectionValue put0(String key, Class<?> elementType, Set set,
			boolean raw) {
		CollectionValue cv = CollectionValue.set(elementType, set);
		Map typeMap = allFieldsMap.get(key);
		if (typeMap == null) {
			if (setFieldMap == null)
				setFieldMap = new HashMap();
			allFieldsMap.put(key, setFieldMap);
		} else
			checkMapType(key, typeMap, this.setFieldMap);
		return setFieldMap.put(key, cv);
	}

	@SuppressWarnings({ "rawtypes" })
	public synchronized CollectionValue put(String key, Class<?> elementType,
			Set set) {
		validateSet(key, elementType, set);
		return put0(key, elementType, set, false);
	}

	@SuppressWarnings("rawtypes")
	public synchronized Set getSet(String key) {
		if (setFieldMap == null)
			return null;
		CollectionValue cv = setFieldMap.get(key);
		return (cv == null) ? null : (Set) cv.getValue();
	}

	protected void putRaw(String key, Class<?> type, Object value, boolean raw) {
		if (String.class.equals(type)) {
			put0(key, (String) value, raw);
		} else if (ByteBuffer.class.equals(type)) {
			put0(key, (ByteBuffer) value, raw);
		} else if (Long.class.equals(type)) {
			put0(key, (Long) value, raw);
		} else if (Integer.class.equals(type)) {
			put0(key, (Integer) value, raw);
		} else if (Date.class.equals(type)) {
			put0(key, (Date) value, raw);
		} else if (Boolean.class.equals(type)) {
			put0(key, (Boolean) value, raw);
		} else if (Float.class.equals(type)) {
			put0(key, (Float) value, raw);
		} else if (Double.class.equals(type)) {
			put0(key, (Double) value, raw);
		} else if (BigDecimal.class.equals(type)) {
			put0(key, (BigDecimal) value, raw);
		} else if (UUID.class.equals(type)) {
			put0(key, (UUID) value, raw);
		} else if (BigInteger.class.equals(type)) {
			put0(key, (BigInteger) value, raw);
		} else if (InetAddress.class.equals(type)) {
			put0(key, (InetAddress) value, raw);
		}
	}

	public synchronized <T> T putPrimitive(String key, Class<T> type, T value) {
		putRaw(key, type, value, true);
		return value;
	}

	@SuppressWarnings("rawtypes")
	public synchronized <T> void reset(String key) {
		if (key == null)
			return;
		FieldMetadata fm = checkField(key);
		// if collection type
		if (fm.isCollectionType()) {
			if (fm.isList())
				put0(key, fm.getFirstElementType(), (List) null, true);
			else if (fm.isSet())
				put0(key, fm.getFirstElementType(), (Set) null, true);
			else if (fm.isMap())
				put0(key, fm.getFirstElementType(), fm.getSecondElementType(),
						null, true);
		} else
			// primitive
			putPrimitive(key, fm.getType(), null);
	}

	@SuppressWarnings("unchecked")
	public synchronized <T> T getPrimitive(String key, Class<T> type) {
		if (String.class.equals(type)) {
			return (T) getString(key);
		} else if (ByteBuffer.class.equals(type)) {
			return (T) getBytes(key);
		} else if (Long.class.equals(type)) {
			return (T) getLong(key);
		} else if (Integer.class.equals(type)) {
			return (T) getInt(key);
		} else if (Date.class.equals(type)) {
			return (T) getDate(key);
		} else if (Boolean.class.equals(type)) {
			return (T) getBoolean(key);
		} else if (Float.class.equals(type)) {
			return (T) getFloat(key);
		} else if (Double.class.equals(type)) {
			return (T) getDouble(key);
		} else if (BigDecimal.class.equals(type)) {
			return (T) getDecimal(key);
		} else if (UUID.class.equals(type)) {
			return (T) getUUID(key);
		} else if (BigInteger.class.equals(type)) {
			return (T) getBigInteger(key);
		} else if (InetAddress.class.equals(type)) {
			return (T) getInetAddress(key);
		}
		return null;
	}

	public abstract StoreResult store() throws StoreException;

	@SuppressWarnings("rawtypes")
	protected void doRevive(List<ObjectStoreItem> objectStoreItems) {
		// Get/set id
		for (ObjectStoreItem i : objectStoreItems) {
			String key = i.getKey();
			if (ID_NAME.equals(key)) {
				setId((UUID) i.getValue());
				continue;
			}
			if (LASTMODIFIEDDATE_NAME.equals(key)) {
				setLastModifiedAt((Date) i.getValue());
				continue;
			}
			String key1 = i.getKey();
			if (i.isCollection()) {
				CollectionValue cd = (CollectionValue) i.getValue();
				if (cd.isMap())
					put0(key1, cd.getFirstType(), cd.getSecondType(),
							(Map) cd.getValue(), true);
				else if (cd.isList())
					put0(key1, cd.getFirstType(), (List) cd.getValue(), true);
				else if (cd.isSet())
					put0(key1, cd.getFirstType(), (Set) cd.getValue(), true);
			} else
				putRaw(key1, i.getType(), i.getValue(), true);
		}
	}

	protected Class<?> getType(Map<String, ?> typeMap) {
		if (typeMap == null)
			return null;
		if (typeMap.equals(this.stringFieldMap))
			return String.class;
		else if (typeMap.equals(this.bytesFieldMap))
			return ByteBuffer.class;
		else if (typeMap.equals(this.longFieldMap))
			return Long.class;
		else if (typeMap.equals(this.integerFieldMap))
			return Integer.class;
		else if (typeMap.equals(this.dateFieldMap))
			return Date.class;
		else if (typeMap.equals(this.booleanFieldMap))
			return Boolean.class;
		else if (typeMap.equals(this.floatFieldMap))
			return Float.class;
		else if (typeMap.equals(this.doubleFieldMap))
			return Double.class;
		else if (typeMap.equals(this.bigDecimalFieldMap))
			return BigDecimal.class;
		else if (typeMap.equals(this.uuidFieldMap))
			return UUID.class;
		else if (typeMap.equals(this.bigIntegerFieldMap))
			return BigInteger.class;
		else if (typeMap.equals(this.inetAddressFieldMap))
			return InetAddress.class;
		else if (typeMap.equals(this.mapFieldMap))
			return Map.class;
		else if (typeMap.equals(this.listFieldMap))
			return List.class;
		else if (typeMap.equals(this.setFieldMap))
			return Set.class;
		else
			return null;
	}

	protected Collection<ObjectStoreItem> collectItems() throws StoreException {
		Collection<String> keys = getKeys();
		Collection<ObjectStoreItem> objectStoreItems = new ArrayList<ObjectStoreItem>();
		for (String key : keys) {
			Map<String, ?> typeMap = allFieldsMap.get(key);
			Class<?> type = getType(typeMap);
			if (type == null)
				throw new StoreException("Cannot get type for key=" + key);
			Object value = typeMap.get(key);
			if (value == null)
				throw new StoreException("Cannot get value for key=" + key);
			if (type.equals(Set.class) || type.equals(Map.class)
					|| type.equals(List.class))
				type = CollectionValue.class;
			// Add to items in any case
			objectStoreItems.add(new ObjectStoreItem(key, type, value));
		}
		return objectStoreItems;
	}

	protected StoreResult handleStoreResult(StoreResult storeResult)
			throws StoreException {
		if (storeResult.isSuccess()) {
			Collection<ObjectStoreItem> successItems = storeResult.getItems();
			for (Iterator<ObjectStoreItem> i = successItems.iterator(); i
					.hasNext();) {
				ObjectStoreItem item = i.next();
				String key = item.getKey();
				if (ID_NAME.equals(key)) {
					setId((UUID) item.getValue());
					i.remove();
				} else if (LASTMODIFIEDDATE_NAME.equals(key)) {
					setLastModifiedAt((Date) item.getValue());
					i.remove();
				}
			}
			return new StoreResult(true, null);
		} else
			return new StoreResult(false, storeResult.getItems());
	}

	public synchronized boolean isIfNotExists() {
		return (ifNotExistsItems != null);
	}

	public synchronized boolean setIfNotExists(boolean ifNotExists) {
		if (ifNotExists)
			return addIfNotExistsItem(new ObjectStoreItem(
					StoreObject.LASTMODIFIEDDATE_NAME, Date.class,
					getLastModifiedAt()));
		this.ifNotExistsItems = null;
		return true;
	}

	public synchronized boolean addIfNotExistsItem(
			ObjectStoreItem objectStoreItem) {
		if (objectStoreItem == null)
			return false;
		if (this.ifNotExistsItems == null)
			this.ifNotExistsItems = new ArrayList<ObjectStoreItem>();
		this.ifNotExistsItems.add(objectStoreItem);
		return true;
	}

	public synchronized Collection<ObjectStoreItem> getIfNotExistsItems() {
		return this.ifNotExistsItems;
	}

	public synchronized void setIfNotExistsItems(
			Collection<ObjectStoreItem> objectStoreItems) {
		this.ifNotExistsItems = objectStoreItems;
	}
}
