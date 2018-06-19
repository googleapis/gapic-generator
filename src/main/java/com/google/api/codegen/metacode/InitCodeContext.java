/* Copyright 2016 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.api.codegen.metacode;

import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.config.FieldModel;
import com.google.api.codegen.config.TypeModel;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.SymbolTable;
import com.google.api.codegen.util.testing.TestValueGenerator;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

/** The context used for generating init code views */
@AutoValue
public abstract class InitCodeContext {
  public enum InitCodeOutputType {
    SingleObject,
    FieldList,
  }

  /** The type of the output object. */
  public abstract TypeModel initObjectType();

  /** The suggested name for the output object */
  public abstract Name suggestedName();

  /**
   * The symbol table used to store unique symbols used in the init code. Default to empty table.
   */
  public abstract SymbolTable symbolTable();

  /**
   * Contains the fields that require initialization. Must be set if the output type is FieldList.
   */
  @Nullable
  public abstract Iterable<FieldModel> initFields();

  /** Returns the output type of the init code. Default to SingleObject. */
  public abstract InitCodeOutputType outputType();

  /**
   * Returns the value generator which is used to produce deterministically random unique values for
   * testing purposes.
   */
  @Nullable
  public abstract TestValueGenerator valueGenerator();

  /** Returns init config strings. */
  @Nullable
  public abstract List<String> initFieldConfigStrings();

  /**
   * Allows additional InitCodeNode objects which will be placed into the generated subtrees. This
   * is currently used by smoke testing only.
   */
  @Nullable
  public abstract Iterable<InitCodeNode> additionalInitCodeNodes();

  /** The map which stores init value config data. Default to empty map. */
  public abstract ImmutableMap<String, InitValueConfig> initValueConfigMap();

  /** A map of field names to field configs. Defaults to the empty map. */
  public abstract ImmutableMap<String, FieldConfig> fieldConfigMap();

  public static Builder newBuilder() {
    return new AutoValue_InitCodeContext.Builder()
        .symbolTable(new SymbolTable())
        .initValueConfigMap(ImmutableMap.<String, InitValueConfig>of())
        .fieldConfigMap(ImmutableMap.<String, FieldConfig>of())
        .outputType(InitCodeOutputType.SingleObject);
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder initObjectType(TypeModel type);

    public abstract Builder suggestedName(Name name);

    public abstract Builder symbolTable(SymbolTable table);

    public abstract Builder initFields(Iterable<FieldModel> fields);

    public abstract Builder outputType(InitCodeOutputType val);

    public abstract Builder valueGenerator(TestValueGenerator generator);

    public abstract Builder initFieldConfigStrings(List<String> configStrings);

    public abstract Builder initValueConfigMap(Map<String, InitValueConfig> configMap);

    public abstract Builder fieldConfigMap(ImmutableMap<String, FieldConfig> fieldConfigMap);

    public abstract Builder additionalInitCodeNodes(Iterable<InitCodeNode> additionalNodes);

    public abstract InitCodeContext build();
  }
}
