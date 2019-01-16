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
import com.google.api.codegen.config.TypeModel;
import com.google.api.codegen.transformer.ImportTypeTable;
import com.google.api.codegen.transformer.MethodContext;
import com.google.api.codegen.transformer.SampleImportTransformer;
import com.google.api.codegen.transformer.StandardImportSectionTransformer;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.viewmodel.CallingForm;
import com.google.api.codegen.viewmodel.OutputView;
import java.util.List;

public class JavaSampleImportTransformer extends SampleImportTransformer {

  private static final String API_FUTURE = "com.google.api.common.ApiFuture";
  private static final String BIDI_STEAMING_CALLABLE =
      "com.google.api.gax.rpc.BidiStreamingCallable";
  private static final String OPERATION_FUTURE = "com.google.api.gax.longrunning.OperationFuture";
  private static final String SERVER_STREAM = "com.google.api.gax.rpc.ServerStream";
  private static final String API_STREAM_OBSERVER = "com.google.api.gax.rpc.ApiStreamObserver";

  public JavaSampleImportTransformer() {
    super(new StandardImportSectionTransformer());
  }

  @Override
  protected void addSampleBodyImports(MethodContext context, CallingForm form) {
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
      case FlattenedPaged:
      case CallablePaged:
        typeTable.saveNicknameFor(API_FUTURE);
        saveResponseTypeName(context);
        saveResourceTypeName(context);
        break;
      case Callable:
        typeTable.saveNicknameFor(API_FUTURE);
        typeTable.getAndSaveNicknameFor(namer.getGenericAwareResponseTypeName(context));
        saveResponseTypeName(context);
        break;
      case CallableList:
        saveResponseTypeName(context);
        saveResourceTypeName(context);
        break;
      case CallableStreamingBidi:
        typeTable.saveNicknameFor(BIDI_STEAMING_CALLABLE);
        typeTable.getAndSaveNicknameFor(namer.getGenericAwareResponseTypeName(context));
        break;
      case CallableStreamingClient:
        typeTable.saveNicknameFor(API_STREAM_OBSERVER);
        typeTable.getAndSaveNicknameFor(namer.getGenericAwareResponseTypeName(context));
        break;
      case CallableStreamingServer:
        typeTable.saveNicknameFor(SERVER_STREAM);
        typeTable.getAndSaveNicknameFor(namer.getGenericAwareResponseTypeName(context));
        break;
      case LongRunningCallable:
        typeTable.saveNicknameFor(OPERATION_FUTURE);
        typeTable.getAndSaveNicknameFor(namer.getGenericAwareResponseTypeName(context));
        saveResponseTypeNameForLongRunningMethod(context);
        break;
      case LongRunningFlattenedAsync:
      case LongRunningRequestAsync:
        saveResponseTypeNameForLongRunningMethod(context);
        break;
    }
  }

  @Override
  protected void addOutputImports(MethodContext context, List<OutputView> views) {
    ImportTypeTable typeTable = context.getTypeTable();
    for (OutputView view : views) {
      switch (view.kind()) {
        case DEFINE:
          TypeModel type = ((OutputView.DefineView) view).reference().type();
          if (type != null) {
            typeTable.getAndSaveNicknameFor(type);
          } else {
            saveResourceTypeName(context);
          }
          break;
        case LOOP:
          OutputView.LoopView loopView = (OutputView.LoopView) view;
          type = loopView.collection().type();
          if (type != null) {
            typeTable.getAndSaveNicknameFor(type.makeOptional());
          } else {
            saveResourceTypeName(context);
          }
          addOutputImports(context, loopView.body());
          break;
        case COMMENT:
        case PRINT:
          break; // fall through
        default:
          throw new IllegalArgumentException("unrecognized output view kind: " + view.kind());
      }
    }
  }

  private void saveResourceTypeName(MethodContext context) {
    FieldConfig resourceFieldConfig =
        context.getMethodConfig().getPageStreaming().getResourcesFieldConfig();
    if (context.getFeatureConfig().useResourceNameFormatOption(resourceFieldConfig)) {
      context
          .getNamer()
          .getAndSaveElementResourceTypeName(context.getTypeTable(), resourceFieldConfig);
    } else {
      context.getTypeTable().getAndSaveNicknameForElementType(resourceFieldConfig.getField());
    }
  }

  private void saveResponseTypeName(MethodContext context) {
    if (!context.getMethodModel().isOutputTypeEmpty()) {
      context
          .getMethodModel()
          .getAndSaveResponseTypeName(context.getTypeTable(), context.getNamer());
    }
  }

  private void saveResponseTypeNameForLongRunningMethod(MethodContext context) {
    context
        .getTypeTable()
        .getAndSaveNicknameFor(context.getMethodConfig().getLongRunningConfig().getReturnType());
  }
}
