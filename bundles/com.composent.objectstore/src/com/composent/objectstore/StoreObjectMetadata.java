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

import java.util.Collection;

public interface StoreObjectMetadata {

	public String getClassName();

	public Collection<FieldMetadata> getFields();

	public FieldMetadata getField(String fieldName);

	public Collection<FieldMetadata> getPrimaryKey();

	public static interface FieldMetadata {
		public String getName();

		public Class<?> getType();

		public boolean isCollectionType();

		public boolean isMap();

		public boolean isSet();

		public boolean isList();

		public Class<?> getFirstElementType();

		public Class<?> getSecondElementType();

		public boolean isValid(Class<?> type);

		public boolean isValid(Class<?> collectionType,
				Class<?> firstElementType, Class<?> secondElementType);

	}
}
