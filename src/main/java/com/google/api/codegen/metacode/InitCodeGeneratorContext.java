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

import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.util.SymbolTable;
import com.google.api.codegen.util.testing.TestValueGenerator;
import com.google.api.tools.framework.model.Method;
import com.google.auto.value.AutoValue;

import java.util.Map;

import javax.annotation.Nullable;

/**
 * Context class used by InitCodeGenerator.
 *
 * Contains context information related to init code generation stage which includes:
 * 1. SymbolTable to produce unique variable names.
 * 2. TestValueGenerator to populate unique values.
 * 3. Initialized fields structure configuration map.
 * 4. The method to generate.
 * 5. A ModelTypeTable to hold information about the types used for type aliasing in samples.
 */
@AutoValue
public abstract class InitCodeGeneratorContext {
  @Nullable
  public abstract TestValueGenerator valueGenerator();

  public abstract SymbolTable symbolTable();

  public abstract Map<String, Object> initStructure();

  public abstract Method method();

  @Nullable
  public abstract ModelTypeTable typeTable();

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

    public abstract Builder method(Method val);

    public abstract Builder typeTable(ModelTypeTable typeTable);

    public abstract InitCodeGeneratorContext build();
  }
}
