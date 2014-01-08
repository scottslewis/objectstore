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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.composent.objectstore.IObjectStore.ConsistencyLevel;
import com.composent.objectstore.IObjectStore.Util;
import com.composent.objectstore.IObjectStore.Value;
import com.composent.objectstore.StoreObjectQuery.Relation.Op;

public abstract class StoreObjectQuery {

	public static class Relation implements Serializable {
		private static final long serialVersionUID = -4881256442499206204L;

		public enum Op {
			EQ, GT, LT, GTE, LTE
		}

		private final String identifier;
		private final Op opType;
		private final List<Value> values = new ArrayList<Value>();

		public Relation(String identifier, Op opType, Value value) {
			Util.checkArgNotNull(identifier, "identifier");
			this.identifier = identifier;
			this.opType = opType;
			Util.checkArgNotNull(value, "typeValue");
			this.values.add(value);
		}

		public Relation(String identifier, Value... typeValues) {
			Util.checkArgNotNull(identifier, "identifier");
			this.identifier = identifier;
			this.opType = null;
			Util.checkArgNotNull(typeValues, "values");
			for (Value tv : typeValues)
				if (tv != null)
					this.values.add(tv);
		}

		public String getIdentifier() {
			return identifier;
		}

		public Op getOpType() {
			return opType;
		}

		public List<Value> getTypeValues() {
			return values;
		}

	}

	public static class Where implements Serializable {

		private static final long serialVersionUID = -8426547704921159194L;

		public static Where equalTo(String identifier, Value tv) {
			Util.checkArgNotNull(identifier, "identifier");
			return new Where(new Relation(identifier, Op.EQ, tv));
		}

		public static Where greaterThan(String identifier, Value tv) {
			Util.checkArgNotNull(identifier, "identifier");
			return new Where(new Relation(identifier, Op.GT, tv));
		}

		public static Where lessThan(String identifier, Value tv) {
			Util.checkArgNotNull(identifier, "identifier");
			return new Where(new Relation(identifier, Op.LT, tv));
		}

		public static Where greaterThanOrEqualTo(String identifier, Value tv) {
			Util.checkArgNotNull(identifier, "identifier");
			return new Where(new Relation(identifier, Op.GTE, tv));
		}

		public static Where lessThanOrEqualTo(String identifier, Value tv) {
			Util.checkArgNotNull(identifier, "identifier");
			return new Where(new Relation(identifier, Op.LTE, tv));
		}

		public static Where in(String identifier, Value... values) {
			Util.checkArgNotNull(identifier, "identifier");
			return new Where(new Relation(identifier, values));
		}

		private final List<Relation> relations = new ArrayList<Relation>();

		public Where() {
		}

		public Where(Relation... relations) {
			and(relations);
		}

		public Where and(Relation... relations) {
			Util.checkArgNotNull(relations, "relations");
			for (Relation r : relations)
				if (r != null)
					this.relations.add(r);
			return this;
		}

		public List<Relation> getRelations() {
			return relations;
		}
	}

	private final IObjectStore store;
	private final StoreObjectMetadata metadata;
	private String queryName;
	private Where where;
	private Integer limit;
	private boolean allowFiltering;
	private ConsistencyLevel consistencyLevel;

	protected StoreObjectQuery(IObjectStore store,
			StoreObjectMetadata metadata, String queryName) {
		IObjectStore.Util.checkArgNotNull(store, "store");
		this.store = store;
		IObjectStore.Util.checkArgNotNull(metadata, "className");
		this.metadata = metadata;
		this.queryName = queryName;
	}

	public IObjectStore getStore() {
		return this.store;
	}

	public String getQueryName() {
		return queryName;
	}

	public StoreObjectMetadata getMetadata() {
		return this.metadata;
	}

	public String getClassName() {
		return this.metadata.getClassName();
	}

	public Integer getLimit() {
		return this.limit;
	}

	public StoreObjectQuery setWhere(Where where) {
		IObjectStore.Util.checkArgNotNull(where, "where");
		this.where = where;
		return this;
	}

	public StoreObjectQuery setLimit(Integer limit) {
		this.limit = limit;
		return this;
	}

	public StoreObjectQuery setAllowFiltering(boolean allowFiltering) {
		this.allowFiltering = allowFiltering;
		return this;
	}

	public boolean getAllowFiltering() {
		return this.allowFiltering;
	}

	public Where getWhere() {
		return this.where;
	}

	public ConsistencyLevel getConsistencyLevel() {
		return this.consistencyLevel;
	}

	public void setConsistencyLevel(ConsistencyLevel level) {
		this.consistencyLevel = level;
	}

	public abstract Collection<StoreObject> execute() throws StoreException;
}
