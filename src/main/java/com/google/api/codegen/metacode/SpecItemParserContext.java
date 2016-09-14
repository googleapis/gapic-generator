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

import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.SymbolTable;
import com.google.api.codegen.util.testing.TestValueGenerator;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.TypeRef;
import com.google.auto.value.AutoValue;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

@AutoValue
public abstract class SpecItemParserContext {

  public abstract SymbolTable table();

  @Nullable
  public abstract TestValueGenerator valueGenerator();

  public abstract TypeRef rootObjectType();

  public abstract Name suggestedName();

  @Nullable
  public abstract Iterable<Field> initFieldSet();

  public abstract Map<String, InitValueConfig> initValueConfigMap();

  @Nullable
  public abstract List<String> sampleCodeInitFields();

  @Nullable
  public abstract List<SpecItemNode> subTrees();

  public static SpecItemParserContext.Builder newBuilder() {
    return new AutoValue_SpecItemParserContext.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder table(SymbolTable val);

    public abstract Builder valueGenerator(TestValueGenerator val);

    public abstract Builder rootObjectType(TypeRef val);

    public abstract Builder suggestedName(Name val);

    public abstract Builder initFieldSet(Iterable<Field> val);

    public abstract Builder initValueConfigMap(Map<String, InitValueConfig> val);

    public abstract Builder sampleCodeInitFields(List<String> val);

    public abstract Builder subTrees(List<SpecItemNode> val);

    public abstract SpecItemParserContext build();
  }
}
