/* Copyright 2016 Google Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.api.codegen.metacode;

import com.google.api.codegen.InterfaceView;
import com.google.api.codegen.config.ApiConfig;
import com.google.api.codegen.config.CollectionConfig;
import com.google.api.codegen.config.InterfaceConfig;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.metacode.InitCodeNode;
import com.google.api.codegen.metacode.InitValueConfig;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.SymbolTable;
import com.google.api.codegen.util.testing.TestValueGenerator;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.TypeRef;
import com.google.auto.value.AutoValue;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** The context used for generating init code views */
@AutoValue
public abstract class InitCodeContext {
  public enum InitCodeOutputType {
    SingleObject,
    FieldList,
  }

  /** The type of the output object. */
  public abstract TypeRef initObjectType();

  /** The suggested name for the output object */
  public abstract Name suggestedName();

  /**
   * The symbol table used to store unique symbols used in the init code.
   * Default to empty table.
   */
  public abstract SymbolTable symbolTable();

  @Nullable
  /**
   * Contains the fields that requires init.
   * Must be set if the output type is FieldList.
   * */
  public abstract Iterable<Field> initFields();

  /**
   * Returns the output type of the init code. Default to SingleObject.
   */
  public abstract InitCodeOutputType outputType();

  @Nullable
  /**
   *  Returns the value generator which is used to produce deterministically random unique
   *  values for testing purpose.
   */
  public abstract TestValueGenerator valueGenerator();

  @Nullable
  /** Returns init config strings from user config file.*/
  public abstract Iterable<String> initFieldConfigStrings();

  @Nullable
  /**
   * Allows additional InitCodeNode objects which will be placed into the generated subtrees.
   * This is currently used by smoke testing only.
   */
  public abstract Iterable<InitCodeNode> additionalInitCodeNodes();

  /**
   * The map which stores init value config data.
   * Default to empty map.
   */
  public abstract ImmutableMap<String, InitValueConfig> initValueConfigMap();

  public static Builder newBuilder() {
    ImmutableMap.Builder<String, InitValueConfig> emptyConfigMap = new ImmutableMap.Builder<>();
    return new AutoValue_InitCodeContext.Builder()
        .symbolTable(new SymbolTable())
        .initValueConfigMap(emptyConfigMap.build())
        .outputType(InitCodeOutputType.SingleObject);
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder initObjectType(TypeRef type);

    public abstract Builder suggestedName(Name name);

    public abstract Builder symbolTable(SymbolTable table);

    public abstract Builder initFields(Iterable<Field> fields);

    public abstract Builder outputType(InitCodeOutputType val);

    public abstract Builder valueGenerator(TestValueGenerator generator);

    public abstract Builder initFieldConfigStrings(Iterable<String> configStrings);

    public abstract Builder initValueConfigMap(Map<String, InitValueConfig> configMap);

    public abstract Builder additionalInitCodeNodes(Iterable<InitCodeNode> additionalNodes);

    public abstract InitCodeContext build();
  }
}
