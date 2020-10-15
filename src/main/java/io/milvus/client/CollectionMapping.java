/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.milvus.client;

import com.google.common.collect.ImmutableMap;
import io.milvus.grpc.FieldParam;
import io.milvus.grpc.KeyValuePair;
import io.milvus.grpc.Mapping;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Represents a collection mapping */
public class CollectionMapping {
  private final Mapping.Builder builder;

  public static CollectionMapping create(String collectionName) {
    return new CollectionMapping(collectionName);
  }

  CollectionMapping(Mapping mapping) {
    this.builder = mapping.toBuilder();
  }

  private CollectionMapping(String collectionName) {
    this.builder = Mapping.newBuilder();
    builder.setCollectionName(collectionName);
  }

  /**
   * add a scalar field
   *
   * @param name the field name
   * @param type the field data type
   * @return this CollectionMapping
   */
  public CollectionMapping addField(String name, DataType type) {
    builder.addFields(FieldParam.newBuilder().setName(name).setTypeValue(type.getVal()).build());
    return this;
  }

  /**
   * add a vector field
   *
   * @param name the field name
   * @param type the field data type
   * @param dimension the vector dimension
   * @return this CollectionMapping
   */
  public CollectionMapping addVectorField(String name, DataType type, int dimension) {
    FieldParam field = FieldParam.newBuilder()
        .setName(name)
        .setTypeValue(type.getVal())
        .addExtraParams(KeyValuePair.newBuilder()
            .setKey(MilvusClient.extraParamKey)
            .setValue(new JSONObject().put("dim", dimension).toString())
            .build())
        .build();
    builder.addFields(field);
    return this;
  }

  public List<Map<String, Object>> getFields() {
    return builder.getFieldsList().stream()
        .map(f -> {
          ImmutableMap.Builder<String, Object> builder = ImmutableMap
              .<String, Object>builder()
              .put("name", f.getName())
              .put("type", DataType.valueOf(f.getType().getNumber()));
          String paramsInJson = getParamsInJson(f.getExtraParamsList());
          if (paramsInJson != null) {
            builder.put(MilvusClient.extraParamKey, paramsInJson);
          }
          return builder.build();
        })
        .collect(Collectors.toList());
  }

  /**
   * Set extra params in json string
   *
   * @param paramsInJson Two optional parameters can be included. "segment_row_limit" is default
   *                     to 100,000. Merge will be triggered if more than this number of entities
   *                     are inserted into collection. "auto_id" is default to <code>true</code>.
   *                     Entity ids will be auto-generated by Milvus if set to true.
   * @return this CollectionMapping
   */
  public CollectionMapping setParamsInJson(String paramsInJson) {
    builder.addExtraParams(KeyValuePair.newBuilder()
        .setKey(MilvusClient.extraParamKey)
        .setValue(paramsInJson)
        .build());
    return this;
  }

  public String getParamsInJson() {
    return getParamsInJson(builder.getExtraParamsList());
  }

  public String getCollectionName() {
    return builder.getCollectionName();
  }

  Mapping grpc() {
    if (builder.getFieldsCount() == 0) {
      throw new IllegalArgumentException("Fields must not be empty.");
    }
    return builder.build();
  }

  @Override
  public String toString() {
    return String.format(
        "CollectionMapping = {collectionName = %s, fields = %s, params = %s}",
        getCollectionName(), getFields(), getParamsInJson());
  }

  private String getParamsInJson(List<KeyValuePair> extraParams) {
    return extraParams.stream()
        .filter(kv -> MilvusClient.extraParamKey.equals(kv.getKey()))
        .map(KeyValuePair::getValue)
        .findFirst()
        .orElse(null);
  }
}
