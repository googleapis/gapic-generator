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
import java.util.List;

/** The context for transforming the response handling of standalone samples. */
@AutoValue
public abstract class OutputContext {

  private static final TypeModel BYTES_TYPE =
      ProtoTypeRef.create(TypeRef.fromPrimitiveName("bytes"));
  private static final TypeModel STRING_TYPE =
      ProtoTypeRef.create(TypeRef.fromPrimitiveName("string"));

  /** Keeps track of all variables and their types defined in the response handling part. */
  public abstract OutputTransformer.ScopeTable scopeTable();

  /**
   * Used in Python. The enum module needs to be imported to use helper functions in it to convert a
   * protobuf enum type to a descriptive string.
   */
  public abstract List<TypeModel> stringFormattedVariableTypes();

  /** Used in Java and Node.js. */
  public abstract List<TypeModel> fileOutputTypes();

  /** In Java, `java.util.Map` needs to be imported if there are map specs. */
  public abstract List<OutputSpec.LoopStatement> mapSpecs();

  public boolean hasMaps() {
    return !mapSpecs().isEmpty();
  }

  /** Used in Node.js. The sample needs to import `util` and `fs` to write to local files. */
  public boolean hasFileOutput() {
    return !fileOutputTypes().isEmpty();
  }

  /*
   * Used in Java. `java.io.OutputStream` and `java.io.FileOutputStream` need to be imported
   * if writing a bytes field to a local file.
   */
  public boolean hasBytesFileOutput() {
    return fileOutputTypes().contains(BYTES_TYPE);
  }

  /**
   * Used in Java. `java.io.FileWriter` needs to be imported if writing a string field to a local
   * file.
   */
  public boolean hasStringFileOutput() {
    return fileOutputTypes().contains(STRING_TYPE);
  }

  /**
   * Used in Node.js. `writeFile` should be defined as `var` instead of `const` if used multiple
   * times.
   */
  public boolean hasMultipleFileOutputs() {
    return fileOutputTypes().size() > 1;
  }

  /** Creates a new OutputContext. */
  public static OutputContext create() {
    return new AutoValue_OutputContext(
        new OutputTransformer.ScopeTable(),
        new ArrayList<>(),
        new ArrayList<>(),
        new ArrayList<>());
  }

  /** Creates a new OutputContext, with a new child scope table. */
  public OutputContext createWithNewChildScope() {
    return new AutoValue_OutputContext(
        scopeTable().newChild(), stringFormattedVariableTypes(), fileOutputTypes(), mapSpecs());
  }
}
