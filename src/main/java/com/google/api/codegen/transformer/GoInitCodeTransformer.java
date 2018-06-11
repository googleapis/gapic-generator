/* Copyright 2018 Google LLC
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
package com.google.api.codegen.transformer;

import com.google.api.codegen.config.TypeModel;
import com.google.api.codegen.metacode.InitCodeContext;
import com.google.api.codegen.transformer.go.GoModelTypeNameConverter;
import com.google.api.codegen.util.go.GoTypeTable;

/*
InitCodeContext{
  initObjectType=Protobuf FieldModel (google.example.library.v1.CreateShelfRequest): {null},
  suggestedName=com.google.api.codegen.util.Name@414ef28f,
  symbolTable=com.google.api.codegen.util.SymbolTable@3feaf7cb,
  initFields=[
    Protobuf FieldModel: {
      google.example.library.v1.Shelf
      google.example.library.v1.CreateShelfRequest.shelf
      }
  ],
  outputType=SingleObject,
  valueGenerator=com.google.api.codegen.util.testing.TestValueGenerator@5c9dd20f,
  initFieldConfigStrings=[],
  additionalInitCodeNodes=null,
  initValueConfigMap={},
  fieldConfigMap={
    google.example.library.v1.CreateShelfRequest.shelf=FieldConfig{
      field=Protobuf FieldModel: {google.example.library.v1.Shelf google.example.library.v1.CreateShelfRequest.shelf},
      resourceNameTreatment=NONE,
      resourceNameConfig=null,
      messageResourceNameConfig=null
    }
  }
}
*/

public class GoInitCodeTransformer {
  public String generateInitCode(MethodContext methodContext, InitCodeContext initCodeContext) {
    InitWriter writer = new InitWriter();
    writer.struct(methodContext.getMethodModel().getInputType());
    return writer.sb.toString();
  }

  private static class InitWriter {
    StringBuilder sb = new StringBuilder();
    ModelTypeTable typeTable =
        new ModelTypeTable(new GoTypeTable(), new GoModelTypeNameConverter());

    void struct(TypeModel type) {
      typeInit(type).append("{}");
    }

    private StringBuilder typeInit(TypeModel type) {
      // The type name is "*T", but we need "&T" to init.
      return sb.append('&').append(typeTable.getAndSaveNicknameFor(type).substring(1));
    }
  }
}
