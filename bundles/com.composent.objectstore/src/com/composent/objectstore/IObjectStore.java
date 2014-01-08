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

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface IObjectStore {

	public enum ConsistencyLevel {
		ANY, ONE, TWO, THREE, QUORUM, ALL, LOCAL_QUORUM, EACH_QUORUM, SERIAL, LOCAL_SERIAL
	}

	/**
	 * Available primitive types.
	 */
	public static final List<Class<?>> PRIMITIVE_TYPES = Arrays
			.asList(new Class<?>[] { String.class, ByteBuffer.class,
					Long.class, Integer.class, Date.class, Boolean.class,
					Float.class, Double.class, BigDecimal.class, UUID.class,
					BigInteger.class, InetAddress.class });

	/**
	 * Raw collection types...i.e. <code>java.util.Map.class</code>,
	 * <code>java.util.List</code>, or <code>java.util.Set</code>.
	 */
	public static final List<Class<?>> RAW_COLLECTION_TYPES = Arrays
			.asList(new Class<?>[] { Map.class, List.class, Set.class });

	public static class Util {
		/**
		 * Return <code>true</code> if given type is a primitive type,
		 * <code>false</code> otherwise.
		 * 
		 * @param type
		 *            the type to check. If <code>null</code> an
		 *            {@link IllegalArgumentException} is thrown.
		 * @return <code>true</code> if the type is a primitive type,
		 *         <code>false</code> otherwise.
		 */
		public static boolean isPrimitiveType(Class<?> type) {
			return IObjectStore.PRIMITIVE_TYPES.contains(type);
		}

		public static boolean isCollectionValueType(Class<?> type) {
			return type.equals(CollectionValue.class);
		}

		public static boolean isRawCollectionType(Class<?> type) {
			return RAW_COLLECTION_TYPES.contains(type);
		}

		public static void checkPrimitiveType(Class<?> type)
				throws IllegalArgumentException {
			if (!isPrimitiveType(type))
				throw new IllegalArgumentException("class=" + type
						+ " is not CollectionValue type");
		}

		public static boolean isValidType(Class<?> type) {
			return isCollectionValueType(type) || isPrimitiveType(type);
		}

		public static boolean isTypesCompatible(Class<?> type1, Class<?> type2) {
			return type1.isAssignableFrom(type2);
		}

		public static boolean isTypesEqual(Class<?> type1, Class<?> type2) {
			return type1.equals(type2);
		}

		public static void checkObjectType(Class<?> type, Object value) {
			checkArgNotNull(value, "value");
			checkTypesCompatible(type, value.getClass());
		}

		public static void checkTypesCompatible(Class<?> type1, Class<?> type2) {
			checkArgNotNull(type1, "type1");
			checkArgNotNull(type2, "type2");
			if (!isTypesCompatible(type1, type2))
				throw new IllegalArgumentException("type1=" + type1
						+ " not assignable to type2=" + type2);
		}

		public static void checkArgNotNull(Object arg,
				String errorMessageObjectName) throws IllegalArgumentException {
			if (arg == null)
				throw new IllegalArgumentException(errorMessageObjectName
						+ " cannot be null");
		}
	}

	public static class CollectionValue implements Serializable {
		private static final long serialVersionUID = 4181123183334087153L;
		private int collectionType;
		private List<Class<?>> elementTypes = new ArrayList<Class<?>>();
		private Object value;

		public static CollectionValue map(Class<?> keyType, Class<?> valueType,
				@SuppressWarnings("rawtypes") Map m) {
			Util.checkArgNotNull(m, "map");
			for (Object key : m.keySet()) {
				Util.checkTypesCompatible(keyType, key.getClass());
				Util.checkTypesCompatible(valueType, m.get(key).getClass());
			}
			return new CollectionValue(0, keyType, valueType, m);
		}

		@SuppressWarnings("rawtypes")
		public static final CollectionValue emptyMap(Class<?> keyType,
				Class<?> valueType) {
			return map(keyType, valueType, new HashMap());
		}

		public static CollectionValue list(Class<?> elementType,
				@SuppressWarnings("rawtypes") List l) {
			Util.checkArgNotNull(l, "list");
			for (Object element : l)
				Util.checkTypesCompatible(elementType, element.getClass());
			return new CollectionValue(1, elementType, null, l);
		}

		@SuppressWarnings("rawtypes")
		public static final CollectionValue emptyList(Class<?> elementType) {
			return list(elementType, new ArrayList());
		}

		public static CollectionValue set(Class<?> elementType,
				@SuppressWarnings("rawtypes") Set s) {
			for (Object element : s)
				Util.checkTypesCompatible(elementType, element.getClass());
			return new CollectionValue(2, elementType, null, s);
		}

		@SuppressWarnings("rawtypes")
		public static final CollectionValue emptySet(Class<?> elementType) {
			return set(elementType, new HashSet());
		}

		public boolean isMap() {
			return this.collectionType == 0;
		}

		public boolean isList() {
			return this.collectionType == 1;
		}

		public boolean isSet() {
			return this.collectionType == 2;
		}

		public Class<?> getFirstType() {
			return (this.elementTypes.size() < 1) ? null : this.elementTypes
					.get(0);
		}

		public Class<?> getSecondType() {
			return (this.elementTypes.size() < 2) ? null : this.elementTypes
					.get(1);
		}

		public Object getValue() {
			return this.value;
		}

		public boolean isCollection() {
			return Util.isCollectionValueType(this.getClass());
		}

		private CollectionValue(int cType, Class<?> first, Class<?> second,
				Object value) {
			this.collectionType = cType;
			if (first != null)
				this.elementTypes.add(first);
			if (second != null)
				this.elementTypes.add(second);
			this.value = value;
		}

		public String toString() {
			StringBuffer buf = new StringBuffer();
			if (isCollection()) {
				if (isMap())
					buf.append("Map<" + getFirstType().getSimpleName() + ","
							+ getSecondType().getSimpleName() + ">");
				else if (isList())
					buf.append("List<" + getFirstType().getSimpleName() + ">");
				else if (isSet())
					buf.append("Set<" + getFirstType().getSimpleName() + ">");
			}
			Object value = getValue();
			if (value != null)
				buf.append(value);
			return buf.toString();
		}
	}

	public static class Value {
		private Class<?> type;
		private Object value;

		public Value(String s) {
			this(String.class, s);
		}

		public Value(ByteBuffer b) {
			this(ByteBuffer.class, b);
		}

		public Value(Long l) {
			this(Long.class, l);
		}

		public Value(Integer i) {
			this(Integer.class, i);
		}

		public Value(Date d) {
			this(Date.class, d);
		}

		public Value(Boolean b) {
			this(Boolean.class, b);
		}

		public Value(Float f) {
			this(Float.class, f);
		}

		public Value(Double d) {
			this(Double.class, d);
		}

		public Value(BigDecimal d) {
			this(BigDecimal.class, d);
		}

		public Value(UUID u) {
			this(UUID.class, u);
		}

		public Value(BigInteger i) {
			this(BigInteger.class, i);
		}

		public Value(InetAddress a) {
			this(InetAddress.class, a);
		}

		public Value(CollectionValue cv) {
			this(CollectionValue.class, cv);
		}

		public Value(Class<?> type, Object value) {
			if (!Util.isCollectionValueType(type))
				Util.checkPrimitiveType(type);
			if (value != null)
				Util.checkObjectType(type, value);
			this.type = type;
			this.value = value;
		}

		public Class<?> getType() {
			return this.type;
		}

		public Object getValue() {
			return this.value;
		}

		public boolean isCollection() {
			return Util.isCollectionValueType(getType());
		}

		public CollectionValue getCollectionValue() {
			if (isCollection())
				return (CollectionValue) getValue();
			else
				return null;
		}

	}

	public static class ObjectStoreItem {

		private String key;
		private Value value;

		public ObjectStoreItem(String key, Class<?> type, Object value) {
			Util.checkArgNotNull(key, "key");
			this.key = key;
			this.value = new Value(type, value);
		}

		public String getKey() {
			return this.key;
		}

		public Value getTypeValue() {
			return this.value;
		}

		public Class<?> getType() {
			return this.value.getType();
		}

		public Object getValue() {
			return this.value.getValue();
		}

		public boolean isCollection() {
			return this.value.isCollection();
		}

		public CollectionValue getCollectionValue() {
			if (isCollection())
				return (CollectionValue) getValue();
			else
				return null;
		}
	}

	/**
	 * Create an object table. This method creates an object table with the
	 * given className.
	 * 
	 * @param className
	 *            the unique className/identifier for the new object table. Must
	 *            follow the rules for identifiers described in the
	 *            <b>Identifiers and keywords</b> section of the <a
	 *            href="http://cassandra.apache.org/doc/cql3/CQL.html">CQL3
	 *            specification</a>. As stated in that section, an identifier is
	 *            a token matching the regular expression <b>[a-zA-Z0-9_]*</b>.
	 *            If <code>null</code>, an {@link IllegalArgumentException} is
	 *            thrown. The identifier also must be 48 characters or less.
	 * @param ifNotExists
	 *            if <code>true</code> then the table is created only if it does
	 *            not exist. If it does exist (and ifNotExists is
	 *            <code>true</code>), then this method does nothing. If
	 *            <code>false</code> and the table already exists, Then a
	 *            StoreException is thrown.
	 * @return StoreObjectMetadata the object table metadata associated with the
	 *         newly created object table.
	 * @throws StoreException
	 *             thrown if the object table cannot be created. See isNotExists
	 *             parameter documentation above for more information.
	 */
	public StoreObjectMetadata createObjectTable(String className,
			boolean ifNotExists) throws StoreException;

	/**
	 * Drop/destroy an object table. Given a className for an object table, drop
	 * that object table. <b>NOTE:</b> should be used with caution, as dropping
	 * the object table will delete/destroy any objects/data that exists.
	 * 
	 * @param className
	 *            the name of the class for the objectTable. Must be a valid
	 *            table name resulting from calling the via
	 *            {@link #createObjectTable(String, boolean)} method. If
	 *            <code>null</code> an {@link IllegalArgumentException} is
	 *            thrown.
	 * @param ifExists
	 *            if <code>true</code> then the table is dropped only if it
	 *            actually does exist. If it does not exist, then this method
	 *            does nothing. If <code>false</code> and the table does not
	 *            exist, Then a StoreException is thrown.
	 * @return StoreObjectMetadata the metadata associated with previously
	 *         existing table. Returns <code>null</code> if ifExists is true,
	 *         and the table does not exist. the no-longer-existing object table
	 * @throws StoreException
	 *             if the object table cannot be dropped. See ifExists parameter
	 *             documentation above for more information.
	 */
	public StoreObjectMetadata dropObjectTable(String className,
			boolean ifExists) throws StoreException;

	/**
	 * Add a primitive field to an existing object table. The new field is
	 * identified by the fieldName parameter and the object table is identified
	 * by the className parameter.
	 * 
	 * @param className
	 *            the name of the class/object table for the new field. Must be
	 *            a valid identifier, and follow the rules for identifiers
	 *            described in the <b>Identifiers and keywords</b> section of
	 *            the <a
	 *            href="http://cassandra.apache.org/doc/cql3/CQL.html">CQL3
	 *            specification</a>. As stated in that section, an identifier is
	 *            a token matching the regular expression <b>[a-zA-Z0-9_]*</b>.
	 *            If <code>null</code>, an {@link IllegalArgumentException} is
	 *            thrown. The identifier also must be 48 characters or less.
	 * @param fieldName
	 *            Must be a valid identifier, and follow the rules for
	 *            identifiers described in the <b>Identifiers and keywords</b>
	 *            section of the <a
	 *            href="http://cassandra.apache.org/doc/cql3/CQL.html">CQL3
	 *            specification</a>. As stated in that section, an identifier is
	 *            a token matching the regular expression <b>[a-zA-Z0-9_]*</b>.
	 *            If <code>null</code>, an {@link IllegalArgumentException} is
	 *            thrown. The identifier also must be 48 characters or less.
	 * @param fieldType
	 *            the java class corresponding to the supported field type. Must
	 *            be present in Util#PRIMITIVE_TYPES collection and/or return
	 *            <code>true></code> when validated via
	 *            {@link Util#isPrimitiveType(Class)}
	 * @param ifNotExists
	 *            if <code>true</code> then the field is added only if it does
	 *            not already exist. If it does exist (and ifNotExists is
	 *            <code>true</code>), then this method does nothing. If
	 *            <code>false</code> and the table already exists, Then a
	 *            StoreException is thrown.
	 * @return StoreObjectMetadata the className/object table metadata
	 *         corresponding to the table after the successful addition of the
	 *         new field.
	 * @throws StoreException
	 *             thrown if the field cannot be added. See isNotExists
	 *             parameter documentation above for more information.
	 */
	public StoreObjectMetadata addPrimitiveField(String className,
			String fieldName, Class<?> fieldType, boolean ifNotExists)
			throws StoreException;

	/**
	 * Add a collection field (Map, List, or Set) to an existing object table.
	 * The new field is identified byt the fieldName parameter and the object
	 * table is identified by the className parameter.
	 * 
	 * @param className
	 *            the name of the class/object table for the new field. Must be
	 *            a valid identifier, and follow the rules for identifiers
	 *            described in the <b>Identifiers and keywords</b> section of
	 *            the <a
	 *            href="http://cassandra.apache.org/doc/cql3/CQL.html">CQL3
	 *            specification</a>. As stated in that section, an identifier is
	 *            a token matching the regular expression <b>[a-zA-Z0-9_]*</b>.
	 *            If <code>null</code>, an {@link IllegalArgumentException} is
	 *            thrown. The identifier also must be 48 characters or less.
	 * @param fieldName
	 *            Must be a valid identifier, and follow the rules for
	 *            identifiers described in the <b>Identifiers and keywords</b>
	 *            section of the <a
	 *            href="http://cassandra.apache.org/doc/cql3/CQL.html">CQL3
	 *            specification</a>. As stated in that section, an identifier is
	 *            a token matching the regular expression <b>[a-zA-Z0-9_]*</b>.
	 *            If <code>null</code>, an {@link IllegalArgumentException} is
	 *            thrown. The identifier also must be 48 characters or less.
	 * @param collectionFieldType
	 *            the java class corresponding to the desired collection type.
	 *            Must be one of <code>java.util.Map</code>,
	 *            <code>java.util.List</code>, or <code>java.util.Set</code>. If
	 *            not one of these three types, an
	 *            {@link IllegalArgumentException} will be thrown.
	 * @param firstElementType
	 *            If the collectionFieldType is <code>java.util.Map</code> then
	 *            this element is the <b>type of the map key</b>...e.g.
	 *            String.class in Map<String,UUID>. If a
	 *            <code>java.util.List</code>, or <code>java.util.Set</code>
	 *            this type specifies the type of the List or Set
	 *            elements...e.g. the UUID.class in List<UUID>. This Class must
	 *            be a primitive type and so must be present in
	 *            Util#PRIMITIVE_TYPES collection and/or return
	 *            <code>true></code> when validated via
	 *            {@link Util#isPrimitiveType(Class)}.
	 * @param secondElementType
	 *            If the collectionFieldType is <code>java.util.Map</code> then
	 *            this element is the <b>type of the map value</b>...e.g.
	 *            UUID.class in Map<String,UUID>. If a
	 *            <code>java.util.List</code>, or <code>java.util.Set</code>
	 *            this type is ignored and should be <code>null</code>. For
	 *            Maps, this Class must be a primitive type and so must be
	 *            present in Util#PRIMITIVE_TYPES collection and/or return
	 *            <code>true></code> when validated via
	 *            {@link Util#isPrimitiveType(Class)}.
	 * @param ifNotExists
	 *            if <code>true</code> then the field is added only if it does
	 *            not already exist. If it does exist (and ifNotExists is
	 *            <code>true</code>), then this method does nothing. If
	 *            <code>false</code> and the table already exists, Then a
	 *            StoreException is thrown.
	 * @return StoreObjectMetadata the className/object table metadata
	 *         corresponding to the table after the successful addition of the
	 *         new field.
	 * @throws StoreException
	 *             thrown if the field cannot be added. See isNotExists
	 *             parameter documentation above for more information.
	 */
	public StoreObjectMetadata addCollectionField(String className,
			String fieldName, Class<?> collectionFieldType,
			Class<?> firstElementType, Class<?> secondElementType,
			boolean ifNotExists) throws StoreException;

	/**
	 * Drop a field from a an object table. The object table/class name is given
	 * by the className parameter, and the fieldName to drop is given via the
	 * fieldName. <b>NOTE:</b> should be used with caution, as dropping the
	 * field will delete/destroy any data in the table that exists for the given
	 * fieldName.
	 * 
	 * @param className
	 *            the name of the class/object table for the new field. Must be
	 *            a valid identifier, and follow the rules for identifiers
	 *            described in the <b>Identifiers and keywords</b> section of
	 *            the <a
	 *            href="http://cassandra.apache.org/doc/cql3/CQL.html">CQL3
	 *            specification</a>. As stated in that section, an identifier is
	 *            a token matching the regular expression <b>[a-zA-Z0-9_]*</b>.
	 *            If <code>null</code>, an {@link IllegalArgumentException} is
	 *            thrown. The identifier also must be 48 characters or less.
	 * @param fieldName
	 *            Must be a valid identifier, and follow the rules for
	 *            identifiers described in the <b>Identifiers and keywords</b>
	 *            section of the <a
	 *            href="http://cassandra.apache.org/doc/cql3/CQL.html">CQL3
	 *            specification</a>. As stated in that section, an identifier is
	 *            a token matching the regular expression <b>[a-zA-Z0-9_]*</b>.
	 *            If <code>null</code>, an {@link IllegalArgumentException} is
	 *            thrown. The identifier also must be 48 characters or less.
	 * @return StoreObjectMetadata the object store/className metadata after the
	 *         successful drop of a given field.
	 * @throws StoreException
	 *             thrown if the object table or field does not exist, or cannot
	 *             be successfully dropped.
	 */
	public StoreObjectMetadata dropField(String className, String fieldName)
			throws StoreException;

	/**
	 * Change the type of an existing field in the object table. The desired
	 * object table to change is specified via the className parameter, and the
	 * desired field to change is identified via the fieldName parameter.
	 * <b>NOTE:</b> should be used with caution, as changing the type of a field
	 * may make it impossible to read/retrive existing data within the changed
	 * field. This may only be used to change from an existing primitive type to
	 * a new primitive type.
	 * 
	 * @param className
	 *            the name of the class/object table for the new field. Must be
	 *            a valid identifier, and follow the rules for identifiers
	 *            described in the <b>Identifiers and keywords</b> section of
	 *            the <a
	 *            href="http://cassandra.apache.org/doc/cql3/CQL.html">CQL3
	 *            specification</a>. As stated in that section, an identifier is
	 *            a token matching the regular expression <b>[a-zA-Z0-9_]*</b>.
	 *            If <code>null</code>, an {@link IllegalArgumentException} is
	 *            thrown. The identifier also must be 48 characters or less.
	 * @param fieldName
	 *            Must be a valid identifier, and follow the rules for
	 *            identifiers described in the <b>Identifiers and keywords</b>
	 *            section of the <a
	 *            href="http://cassandra.apache.org/doc/cql3/CQL.html">CQL3
	 *            specification</a>. As stated in that section, an identifier is
	 *            a token matching the regular expression <b>[a-zA-Z0-9_]*</b>.
	 *            If <code>null</code>, an {@link IllegalArgumentException} is
	 *            thrown. The identifier also must be 48 characters or less.
	 * @param newFieldType
	 *            the java class corresponding to the new field type. Must be
	 *            present in Util#PRIMITIVE_TYPES collection and/or return
	 *            <code>true></code> when validated via
	 *            {@link Util#isPrimitiveType(Class)}
	 * @return StoreObjectMetadata the object store/className metadata after the
	 *         successful type change of the fielName.
	 * @throws StoreException
	 */
	public StoreObjectMetadata changeFieldType(String className,
			String fieldName, Class<?> newFieldType) throws StoreException;

	/**
	 * Create a StoreObject instance for clients to use to subsequently store
	 * object data via a call to {@link #storeTo(StoreObject)}.
	 * 
	 * @param className
	 *            the name of the class/object table. Must be a valid
	 *            identifier, and follow the rules for identifiers described in
	 *            the <b>Identifiers and keywords</b> section of the <a
	 *            href="http://cassandra.apache.org/doc/cql3/CQL.html">CQL3
	 *            specification</a>. As stated in that section, an identifier is
	 *            a token matching the regular expression <b>[a-zA-Z0-9_]*</b>.
	 *            If <code>null</code>, an {@link IllegalArgumentException} is
	 *            thrown. The identifier also must be 48 characters or less.
	 * @return StoreObject instance that may be used to store object data. Will
	 *         not be <code>null</code>.
	 * @throws StoreException
	 *             thrown if StoreObject instance cannot be created for the
	 *             given className (e.g. because the object table identified by
	 *             className does not yet exist).
	 */
	public StoreObject createStoreObject(String className)
			throws StoreException;

	/**
	 * @param className
	 *            the name of the class/object table. Must be a valid
	 *            identifier, and follow the rules for identifiers described in
	 *            the <b>Identifiers and keywords</b> section of the <a
	 *            href="http://cassandra.apache.org/doc/cql3/CQL.html">CQL3
	 *            specification</a>. As stated in that section, an identifier is
	 *            a token matching the regular expression <b>[a-zA-Z0-9_]*</b>.
	 *            If <code>null</code>, an {@link IllegalArgumentException} is
	 *            thrown. The identifier also must be 48 characters or less.
	 * @param id
	 *            a UUID that is the id associated with the returned
	 *            StoreObject. If <code>null</code>, an
	 *            {@link IllegalArgumentException} will be thrown.
	 * @return StoreObject instance that may be used to store object data. Will
	 *         not be <code>null</code>.
	 * @throws StoreException
	 *             thrown if StoreObject instance cannot be created for the
	 *             given className (e.g. because the object table identified by
	 *             className does not yet exist).
	 * @see {@link #createStoreObject(String)}
	 */
	public StoreObject createStoreObject(String className, UUID id)
			throws StoreException;

	/**
	 * Delete an object from the object table. The object table to delete the
	 * object from is identified by the className parameter, and the object to
	 * delete is identified by the storeObjectId parameter.
	 * 
	 * @param className
	 *            the name of the class/object table with the object to be
	 *            deleted. Must be a valid identifier, and follow the rules for
	 *            identifiers described in the <b>Identifiers and keywords</b>
	 *            section of the <a
	 *            href="http://cassandra.apache.org/doc/cql3/CQL.html">CQL3
	 *            specification</a>. As stated in that section, an identifier is
	 *            a token matching the regular expression <b>[a-zA-Z0-9_]*</b>.
	 *            If <code>null</code>, an {@link IllegalArgumentException} is
	 *            thrown. The identifier also must be 48 characters or less.
	 * @param storeObjectId
	 *            the id of the object to delete. Must not be <code>null</code>.
	 *            If <code>null</code> an {@link IllegalArgumentException} is
	 *            thrown.
	 * @param level
	 *            the ConsistencyLevel for the delete. May be <code>null</code>.
	 *            If <code>null</code>, the default is used.
	 * @throws StoreException
	 *             thrown if the className associated with the storeObject to be
	 *             deleted no longer exists, or data in the storeObject is of
	 *             wrong type, or the underlying store is no longer available.
	 */
	public void delete(String className, UUID storeObjectId,
			ConsistencyLevel level) throws StoreException;

	/**
	 * Create a StoreObjectQuery for subsequent usage to retrieve StoreObject
	 * instances via {@link #retrieve(StoreObjectQuery)}.
	 * 
	 * @param className
	 *            the name of the class/object table for the query. Must be a
	 *            valid identifier, and follow the rules for identifiers
	 *            described in the <b>Identifiers and keywords</b> section of
	 *            the <a
	 *            href="http://cassandra.apache.org/doc/cql3/CQL.html">CQL3
	 *            specification</a>. As stated in that section, an identifier is
	 *            a token matching the regular expression <b>[a-zA-Z0-9_]*</b>.
	 *            If <code>null</code>, an {@link IllegalArgumentException} is
	 *            thrown. The identifier also must be 48 characters or less.
	 * @return StoreObjectQuery instance. Will not be <code>null</code>.
	 * @throws StoreException
	 *             thrown if the className associated with the storeObject to be
	 *             deleted no longer exists, or data in the storeObject is of
	 *             wrong type, or the underlying store is no longer available.
	 */
	public StoreObjectQuery createQuery(String className) throws StoreException;

	/**
	 * Create a StoreObjectQuery for subsequent usage to retrieve StoreObject
	 * instances via {@link #retrieve(StoreObjectQuery)}.
	 * 
	 * @param className
	 *            the name of the class/object table for the query. Must be a
	 *            valid identifier, and follow the rules for identifiers
	 *            described in the <b>Identifiers and keywords</b> section of
	 *            the <a
	 *            href="http://cassandra.apache.org/doc/cql3/CQL.html">CQL3
	 *            specification</a>. As stated in that section, an identifier is
	 *            a token matching the regular expression <b>[a-zA-Z0-9_]*</b>.
	 *            If <code>null</code>, an {@link IllegalArgumentException} is
	 *            thrown. The identifier also must be 48 characters or less.
	 * @param id
	 *            the id to use for the query
	 * @return StoreObjectQuery instance. Will not be <code>null</code>.
	 * @throws StoreException
	 *             thrown if the className associated with the storeObject to be
	 *             deleted no longer exists, or data in the storeObject is of
	 *             wrong type, or the underlying store is no longer available.
	 */
	public StoreObjectQuery createQuery(String className, UUID id)
			throws StoreException;

	/**
	 * Retrieve all object IDs (UUID type) for a given className.
	 * 
	 * @param className
	 *            the className to use. Must not be <code>null</code>
	 * @param limit
	 *            the maximum number of objectIDs to return. If
	 *            <code>null</code>, then no maximum.
	 * @param level
	 *            the consistency level to use for the retrieve. If
	 *            <code>null</code>, then the default read consistency will be
	 *            used.
	 * @return UUID[] an array of UUID instances that provide the key for
	 *         accessing a given StoreObject instance within the given
	 *         className. Will not be <code>null</code>, but may be of length 0.
	 * @throws StoreException
	 *             if the className associated with the query no longer exists,
	 *             or the underlying store is not available.
	 */
	public Collection<UUID> retrieveIds(String className, Integer limit,
			ConsistencyLevel level) throws StoreException;

}
