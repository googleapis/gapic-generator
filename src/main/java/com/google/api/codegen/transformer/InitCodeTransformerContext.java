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
package com.google.api.codegen.transformer;

import com.google.api.codegen.InterfaceView;
import com.google.api.codegen.config.ApiConfig;
import com.google.api.codegen.config.CollectionConfig;
import com.google.api.codegen.config.InterfaceConfig;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.metacode.InitCodeNode;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.SymbolTable;
import com.google.api.codegen.util.testing.TestValueGenerator;
import com.google.api.tools.framework.model.*;
import com.google.auto.value.AutoValue;
import com.google.common.base.Strings;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** The context used for generating init code views */
@AutoValue
public abstract class InitCodeTransformerContext {
  public abstract TypeRef initObjectType();

  public abstract Name suggestedName();

  public abstract SymbolTable symbolTable();

  public abstract MethodTransformerContext methodContext();

  @Nullable
  public abstract Iterable<Field> fields();

  public abstract boolean isFlattened();

  @Nullable
  public abstract TestValueGenerator valueGenerator();

  @Nullable
  public abstract Iterable<String> initFieldConfigStrings();

  @Nullable
  public abstract Iterable<InitCodeNode> additionalNodes();

  public static Builder newBuilder() {
    return new AutoValue_InitCodeTransformerContext.Builder().symbolTable(new SymbolTable());
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder initObjectType(TypeRef type);

    public abstract Builder suggestedName(Name name);

    public abstract Builder symbolTable(SymbolTable table);

    public abstract Builder methodContext(MethodTransformerContext name);

    public abstract Builder fields(Iterable<Field> fields);

    public abstract Builder isFlattened(boolean val);

    public abstract Builder valueGenerator(TestValueGenerator generator);

    public abstract Builder initFieldConfigStrings(Iterable<String> configStrings);

    public abstract Builder additionalNodes(Iterable<InitCodeNode> additionalNodes);

    public abstract InitCodeTransformerContext build();
  }
}
