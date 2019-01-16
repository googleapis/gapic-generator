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
package com.google.api.codegen.transformer.java;

import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.config.MethodModel;
import com.google.api.codegen.transformer.ImportTypeTable;
import com.google.api.codegen.transformer.MethodContext;
import com.google.api.codegen.transformer.SampleImportTransformer;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.viewmodel.CallingForm;

public class JavaSampleImportTransformer extends SampleImportTransformer {

  public JavaSampleImportTransformer() {
    super(new StandardImportSectionTransformer());
  }

  @Override
  public void addSampleBodyImports(MethodContext context, CallingForm form) {
    ImportTypeTable typeTable = context.getTypeTable();
    MethodModel method = context.getMethodModel();
    SurfaceNamer namer = context.getNamer();
    switch (form) {
      case Request:
      case Flattened:
        saveResponseTypeName(context);
        break;
      case RequestPaged:
        saveResourceTypeName(context);
        break;
      case FlattendPaged:
      case CallablePaged:
        typeTable.saveNicknameFor("com.google.api.common.ApiFuture");
        context.getMethodModel().getAndSaveResponseTypeName(typeTable, namer);
        saveResourceTypeName(context);
        break;
      case Callable:
        typeTable.saveNicknameFor("com.google.api.common.ApiFuture");
        type.getAndSaveNicknameFor(namer.getGenericAwareResponseTypeName(context));
        saveResponseTypeName(context);
        break;
      case CallableList:
      	saveResponseTypeName(context);
      	saveResourceTypeName(context);
      	break;
      case CallableStreamingBidi:
      	typeTable.saveNicknameFor("com.google.api.gax.rpc.BidiStreamingCallable");
      	type.getAndSaveNicknameFor(namer.getGenericAwareResponseTypeName(context));
      	break;
      case CallableStreamingClient:
      	
      case CallableStreamingServer:
      case LongrunningCallable:
      case LongrunningFlattenedAsync:
        LongrunningRequestAsync:
        break;
    }
  }

  private void saveResourceTypeName(MethodContext context) {
    FieldConfig resourceFieldConfig =
        context.getMethodConfig().getPageStreaming().getResourcesFieldConfig();
    if (context.getFeatureConfig().useResourceNameFormatOption(resourceFieldConfig)) {
      namer.getAndSaveElementResourceTypeName(typeTable, resourceFieldConfig);
    } else {
      typeTable.getAndSaveNicknameForElementType(resourceField);
    }
  }

  private void saveResponseTypeName(MethodContext context) {
    if (!method.isOutputTypeEmpty()) {
      context.getMethodModel().getAndSaveResponseTypeName(typeTable, namer);
    }
  }
}
