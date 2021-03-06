/**
 * ModelSerializationPolicy.java
 *
 * Copyright (c) 2008-2014 Joy Aether Limited. All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * 
 * This unpublished material is proprietary to Joy Aether Limited.
 * All rights reserved. The methods and
 * techniques described herein are considered trade secrets
 * and/or confidential. Reproduction or distribution, in whole
 * or in part, is forbidden except by express written permission
 * of Joy Aether Limited.
 */
package com.datastore_android_sdk.serialization;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.FieldNamingStrategy;
import com.google.gson.annotations.SerializedName;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.datastore_android_sdk.schema.BlobDatabaseField;


/**
 * A policy that defines how a data model should be serialized and select the
 * fields that should be expanded during serialization.
 * 
 * @author Stanley Lam
 */
public final class ModelSerializationPolicy implements ModelSerializationStrategy, Cloneable {
	
	public static final ModelSerializationPolicy DEFAULT = new ModelSerializationPolicy(FieldNamingPolicy.IDENTITY);
	
	private FieldNamingStrategy fieldNamingPolicy;
	private boolean serializeIdFieldOnly = true;
	private FieldExclusionStrategy exclusionStrategy;
	private Map<String, ModelSerializationStrategy> fieldSerializationStrategies;
	private boolean refreshField = false;
	private boolean serializeBlobFields = false;
	
	private ModelSerializationPolicy(FieldNamingStrategy fieldNamingPolicy) {
		this.fieldNamingPolicy = fieldNamingPolicy;
		fieldSerializationStrategies = new HashMap<String, ModelSerializationStrategy>();
	}
	
	@Override
	protected ModelSerializationPolicy clone() {
		try {
			return (ModelSerializationPolicy) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new AssertionError(e);
		}
	}
	
	/**
	 * Returns a serialization policy that serialize identity data fields as an
	 * object rather than its underlying raw value.
	 * 
	 * @return The new serialization policy
	 */
	public ModelSerializationPolicy disableIdFieldOnlySerialization() {
		ModelSerializationPolicy result = clone();
		result.serializeIdFieldOnly = false;
		return result;
	}
	
	/**
	 * Returns a serialization policy that serialize BLOB data fields as Base64 string.
	 * 
	 * @return The new serialization policy
	 */
	public ModelSerializationPolicy serializeBlobFields() {		
		ModelSerializationPolicy result = clone();
		return result.serializeBlobFields(true);
	}
	
	/**
	 * Returns a serialization policy that uses the given naming policy.
	 * 
	 * @param namingPolicy The naming policy to use
	 * @return The new serialization policy
	 */
	public ModelSerializationPolicy withFieldNamingPolicy(FieldNamingStrategy namingPolicy) {
		ModelSerializationPolicy result = clone();
		result.fieldNamingPolicy = namingPolicy;
		return result;
	}
	
	/**
	 * Determines whether BLOB data fields should be serialized as Base64 string
	 * rather than in its underlying raw value.
	 * 
	 * @param shouldSerialize {@code true} to serialize as Base64 string, {@code false} otherwise
	 * @return This serialization policy
	 */
	private ModelSerializationPolicy serializeBlobFields(boolean shouldSerialize) {
		if (serializeBlobFields != shouldSerialize) {			
			serializeBlobFields = shouldSerialize;
			if (fieldSerializationStrategies != null) {
				for (String field : fieldSerializationStrategies.keySet()) {
					ModelSerializationPolicy policy = (ModelSerializationPolicy) fieldSerializationStrategies.get(field);
					if (policy != null) {
						policy.serializeBlobFields(shouldSerialize);
					}
				}
			}
		}
		return this;
	}
	
	/**
	 * Returns a serialization policy that only serialize the given {@code fields}.
	 * 
	 * @param fields The data fields to serialize
	 * @return The new serialization policy
	 */
	@SuppressWarnings("unchecked")
	public ModelSerializationPolicy withSelectingFields(Map<String, ?> fields) {
		ModelSerializationPolicy result = clone();
		
		if (fields != null) {
			result.exclusionStrategy = new ModelFieldExclusionStrategy(fields.keySet());
			for (String field : fields.keySet()) {
				Map<String, ?> selections = (Map<String, ?>) fields.get(field);
				if (selections != null) {
					ModelSerializationPolicy policy = (ModelSerializationPolicy) result.fieldSerializationStrategies.get(field);
					if (policy == null) {
						policy = ModelSerializationPolicy
							.DEFAULT
							.disableIdFieldOnlySerialization()
							.serializeBlobFields(serializeBlobFields);
					}
					result.fieldSerializationStrategies.put(field, policy.withSelectingFields(selections));
				}
			}
		}
		
		return result;
	}
	
	/**
	 * Returns a serialization policy that serialize the given {@code fields} as JSON object.
	 * 
	 * @param fields The fields to serialize as JSON object
	 * @return The new serialization policy
	 */
	@SuppressWarnings("unchecked")
	public ModelSerializationPolicy withExpansionTree(Map<String, ?> fields) {
		ModelSerializationPolicy result = clone();
		// serialization strategies for the fields should not be cloned with an expansion tree
		result.fieldSerializationStrategies = new HashMap<String, ModelSerializationStrategy>();
		
		if (fields != null) {
			for (String field : fields.keySet()) {
				Map<String, ?> expansionTreeForField = null;
				if (fields.get(field) instanceof Map) {					
					expansionTreeForField = (Map<String, ?>) fields.get(field);
				}
				ModelSerializationPolicy policy = (ModelSerializationPolicy) result.fieldSerializationStrategies.get(field);
				if (policy == null) {
					policy = ModelSerializationPolicy
						.DEFAULT
						.disableIdFieldOnlySerialization()
						.serializeBlobFields(serializeBlobFields);
				}
				result.fieldSerializationStrategies.put(field, policy.withExpansionTree(expansionTreeForField));
			}
		}
		
		return result;
	}
	
	/**
	 * Determines whether or not a given field is a foreign object.
	 * 
	 * @param field the field to determine whether or not it is a foreign object
	 * @return true if the field represents a foreign object, false otherwise
	 */
	private boolean isForeignField(Field field) {
		DatabaseField dbField = field == null ? null : field.getAnnotation(DatabaseField.class);
		return dbField == null ? false : dbField.foreign();
	}
	
	/**
	 * Determines whether or not a given field is a foreign collection.
	 * 
	 * @param field the field to determine whether or not it is a foreign collection
	 * @return true if the field represents a foreign collection, false otherwise
	 */
	public boolean isForeignCollection(Field field) {
		ForeignCollectionField foreignCollectionField = field == null ? null : field.getAnnotation(ForeignCollectionField.class);
		return foreignCollectionField != null;
	}
	
	/**
	 * Determines whether or not a given field is an eager foreign collection.
	 * 
	 * @param field the field to determine whether or not it is a foreign collection
	 * @return true if the field represents a eager foreign collection, false otherwise
	 */
	private boolean isEagerForeignCollection(Field field) {
		ForeignCollectionField foreignCollectionField = field == null ? null : field.getAnnotation(ForeignCollectionField.class);
		return foreignCollectionField == null ? false : foreignCollectionField.eager();
	}
	
	/**
	 * Retrieves the serialized name of a given field.
	 * 
	 * @param field the field to retrieve the serialized name
	 * @return A String that represents the serialize name of a given Field
	 */
	private String getFieldSerializedName(Field field) {
		SerializedName serializedName = field == null ? null : field.getAnnotation(SerializedName.class);
		return serializedName == null ? fieldNamingPolicy.translateName(field) : serializedName.value();
	}
	
	/**
	 * Returns whether the given {@code field} is a BLOB data.
	 * 
	 * @param field the field to determine whether it is a BLOB data
	 * @return {@code true} if the {@code field} is a BLOB data, {@code false} otherwise
	 */
	private boolean isBlobDatabaseField(Field field) {
		BlobDatabaseField blob = field == null ? null : field.getAnnotation(BlobDatabaseField.class);
		return blob != null;
	}

	/* (non-Javadoc)
	 * @see ModelSerializationStrategy#shouldExpandField(java.lang.reflect.Field)
	 */
	@Override
	public boolean shouldExpandField(Field field) {
		String fieldName = getFieldSerializedName(field);
		
		// Refreshes children DAO if the collection was a lazy foreign collection
		if (!isEagerForeignCollection(field)) {
			for (ModelSerializationStrategy strategy : fieldSerializationStrategies.values()) {
				if (strategy instanceof ModelSerializationPolicy) {
					((ModelSerializationPolicy) strategy).refreshField = true;
				}
			}
		}
		
		return fieldSerializationStrategies.containsKey(fieldName) 
			&& (isForeignField(field) || isForeignCollection(field));
	}
	
	/* (non-Javadoc)
	 * @see ModelSerializationStrategy#shouldRefreshField(java.lang.reflect.Field)
	 */
	@Override
	public boolean shouldRefreshField(Field field) {
		return shouldExpandField(field) || refreshField;
	}

	/* (non-Javadoc)
	 * @see ModelSerializationStrategy#getFieldSerializationStrategy(java.lang.reflect.Field)
	 */
	@Override
	public ModelSerializationStrategy getFieldSerializationStrategy(Field field) {
		return fieldSerializationStrategies.get(getFieldSerializedName(field));
	}

	@Override
	public boolean shouldSerializeIdFieldOnly() {
		return serializeIdFieldOnly;
	}
	
	@Override 
	public boolean shouldSkipField(Field field) {
		String fieldName = getFieldSerializedName(field);
		return exclusionStrategy == null ? false : exclusionStrategy.shouldSkipField(fieldName);
	}
	
	@Override
	public boolean shouldSerializeBlobField(Field field) {
		return isBlobDatabaseField(field) && serializeBlobFields;
	}

}
