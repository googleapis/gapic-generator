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

@AutoValue
public abstract class OutputContext {

  // Experimenting Feature

  // We need to keep track of what we've done to variables to generate (or not generate) certain
  // code (e.g., imports, variable modifiers like `const`). So far this information has been
  // entangled inside `OutputView`, which makes both rendering `OutputView` and accessing this
  // information more and more hard. Let's try to split them up.
  //
  enum Status {
    // The variable is defined.
    // We need to know this for Java, C# and Node.js.
    // For Java and C#, variables cannot be redefined in the same scope. Variables that have been
    // defined can only be assigned values of the same type. Types of variables defined also
    // needs to be imported.
    //
    // For Node.js, variables defined with modifier `const` cannot be reassigned.
    DEFINED,

    // The the variable is defined previously, but reassigned to a different value.
    // We need to know this for Java, C# and Node.js
    //
    // For Java and C#, we need to check the variable is assigned value of the same
    // type, and make sure to not render the type again to avoid redeclaration.
    //
    // For Node.js, we need to change the modifier of this variable from `const` to `var`.
    REASSIGNED,

    // Whether a string formatting function has been called on this variable.
    // It does not feel good to put it here but so far it's all we need.
    // Python and PHP need to know this to correctly import the helper class or module
    // for protobuf enum types.
    STRING_FORMATTED
  }

  class ScopeTable {

    ScopeTable output;
    @Nullable ScopeTable parent;
    Set<Variable, VariableType> definedVariables;
    
  }

  public class VariableTypeConfig {

    boolean isProtoType();

    TypeModel typeModel();

    boolean isResourceName();

    ResourceNameConfig resourceNameConfig();
  }

  public class Variable {

    String name;

    ImmutableList<String> accessors;

    VariableTypeConfig type;
  }


  ImmutableList<Variable> stringFormattedVariables();

  ScopeTable scopeTable();
}
