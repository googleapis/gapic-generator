/* Copyright 2019 Google LLC
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
package com.google.api.codegen.config;

import com.google.api.codegen.OutputSpec;
import com.google.api.codegen.transformer.OutputTransformer;
import com.google.api.tools.framework.model.TypeRef;
import com.google.auto.value.AutoValue;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@AutoValue
public abstract class OutputContext {

  private static final TypeModel BYTES_TYPE =
      ProtoTypeRef.create(TypeRef.fromPrimitiveName("bytes"));
  private static final TypeModel STRING_TYPE =
      ProtoTypeRef.create(TypeRef.fromPrimitiveName("string"));

  public abstract OutputTransformer.ScopeTable scopeTable();

  /**
   * In Python, the enum module should be imported to use helper functions in it to format an enum
   * type.
   */
  public abstract List<TypeModel> stringFormattedVariableTypes();

  /**
   * In Java, `java.io.OutputStream` and `java.io.FileOutputStream` need to be imported if writing a
   * bytes field to a local file. `java.io.FileWriter` needs to be imported if writing a string
   * field to a local file.
   *
   * <p>In Node.js, `writeFile` should be defined as `var` instead of `const` if used multiple
   * times.
   */
  public abstract Set<TypeModel> fileOutputTypes();

  /** In Java, `java.util.Map` needs to be imported if there are map specs. */
  public abstract List<OutputSpec.LoopStatement> mapSpecs();

  public boolean hasMaps() {
    return !mapSpecs().isEmpty();
  }

  public boolean hasBytesFileOutput() {
    return fileOutputTypes().contains(BYTES_TYPE);
  }

  public boolean hasStringFileOutput() {
    return fileOutputTypes().contains(STRING_TYPE);
  }

  public static OutputContext create() {
    return new AutoValue_OutputContext(
        new OutputTransformer.ScopeTable(), new ArrayList<>(), new HashSet<>(), new ArrayList<>());
  }

  public OutputContext createWithNewChildScope() {
    return new AutoValue_OutputContext(
        scopeTable().newChild(), stringFormattedVariableTypes(), fileOutputTypes(), mapSpecs());
  }
}
