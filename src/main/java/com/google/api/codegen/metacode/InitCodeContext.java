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
import com.google.api.codegen.transformer.MethodTransformerContext;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.SymbolTable;
import com.google.api.codegen.util.testing.TestValueGenerator;
import com.google.api.tools.framework.model.*;
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
  public enum InitCodeParamType {
    RequestParam,
    FlattenedParam,
  }

  public abstract TypeRef initObjectType();

  public abstract Name suggestedName();

  public abstract SymbolTable symbolTable();

  @Nullable
  public abstract MethodTransformerContext methodContext();

  @Nullable
  public abstract Iterable<Field> fields();

  // Returns the param type of the init code. Default to RequestParam.
  public abstract InitCodeParamType paramType();

  @Nullable
  public abstract TestValueGenerator valueGenerator();

  @Nullable
  public abstract Iterable<String> initFieldConfigStrings();

  @Nullable
  public abstract Iterable<InitCodeNode> additionalInitCodeNodes();

  public abstract ImmutableMap<String, InitValueConfig> initValueConfigMap();

  public static Builder newBuilder() {
    return new AutoValue_InitCodeContext.Builder()
        .symbolTable(new SymbolTable())
        .paramType(InitCodeParamType.RequestParam);
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder initObjectType(TypeRef type);

    public abstract Builder suggestedName(Name name);

    public abstract Builder symbolTable(SymbolTable table);

    public abstract Builder methodContext(MethodTransformerContext name);

    public abstract Builder fields(Iterable<Field> fields);

    public abstract Builder paramType(InitCodeParamType val);

    public abstract Builder valueGenerator(TestValueGenerator generator);

    public abstract Builder initFieldConfigStrings(Iterable<String> configStrings);

    public abstract Builder initValueConfigMap(Map<String, InitValueConfig> configMap);

    public abstract Builder additionalInitCodeNodes(Iterable<InitCodeNode> additionalNodes);

    public abstract InitCodeContext build();
  }
}
