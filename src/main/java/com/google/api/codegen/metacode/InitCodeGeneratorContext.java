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

/**
 * Context class used by InitCodeGenerator.
 *
 * Contains context information related to init code generation stage which includes:
 * 1. SymbolTable to produce unique variable names.
 * 2. TestValueGenerator to populate unique values.
 * 3. Initialized fields structure configuration map.
 * 4. Initialized object name and type. The object is usually a request/response.
 * 5. (Optional)Flattened fields. If the flattened fields is set, The object itself will not be
 *    created, instead only the fields of the object will be initialized. This is usually used
 *    to create init code for flattened methods.
 */
@AutoValue
public abstract class InitCodeGeneratorContext {
  @Nullable
  public abstract TestValueGenerator valueGenerator();

  public abstract SymbolTable symbolTable();

  public abstract Map<String, Object> initStructure();

  public boolean isFlattened() {
    return flattenedFields() != null;
  }

  @Nullable
  public abstract List<Field> flattenedFields();

  public abstract Name initObjectName();

  public abstract TypeRef initObjectType();

  public boolean shouldGenerateTestValue() {
    return valueGenerator() != null;
  }

  public static Builder newBuilder() {
    return new AutoValue_InitCodeGeneratorContext.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder valueGenerator(TestValueGenerator val);

    public abstract Builder symbolTable(SymbolTable val);

    public abstract Builder initStructure(Map<String, Object> val);

    public abstract Builder initObjectName(Name val);

    public abstract Builder initObjectType(TypeRef val);

    public abstract Builder flattenedFields(List<Field> val);

    public abstract InitCodeGeneratorContext build();
  }
}
