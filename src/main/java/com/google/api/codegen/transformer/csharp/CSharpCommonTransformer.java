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
package com.google.api.codegen.transformer.csharp;

import com.google.api.codegen.config.GrpcStreamingConfig.GrpcStreamingType;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.MethodModel;
import com.google.api.codegen.transformer.GapicInterfaceContext;
import com.google.api.codegen.transformer.InterfaceContext;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.ParamWithSimpleDoc;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CSharpCommonTransformer {

  public void addCommonImports(GapicInterfaceContext context) {
    ModelTypeTable typeTable = context.getImportTypeTable();
    // Common imports, only one class per required namespace is needed.
    typeTable.saveNicknameFor("Google.Api.Gax.GaxPreconditions");
    typeTable.saveNicknameFor("Google.Api.Gax.Grpc.ServiceSettingsBase");
    typeTable.saveNicknameFor("Google.Protobuf.WellKnownTypes.SomeSortOfWellKnownType");
    typeTable.saveNicknameFor("Grpc.Core.StatusCode");
    typeTable.saveNicknameFor("System.Collections.ObjectModel.ReadOnlyCollection");
    typeTable.saveNicknameFor("System.Threading.Tasks.Task");
    typeTable.saveNicknameFor("System.Threading.Thread");
    typeTable.saveNicknameFor("System.NotImplementedException");
    typeTable.saveNicknameFor("System.Collections.IEnumerable");
    typeTable.saveNicknameFor("System.Collections.Generic.IEnumerable");
  }

  public List<MethodModel> getSupportedMethods(InterfaceContext context) {
    List<MethodModel> result = new ArrayList<>();
    boolean mixinsDisabled = !context.getFeatureConfig().enableMixins();
    for (MethodModel method : context.getSupportedMethods()) {
      if (mixinsDisabled && context.getMethodConfig(method).getRerouteToGrpcInterface() != null) {
        continue;
      }
      MethodConfig methodConfig = context.getMethodConfig(method);
      if (methodConfig.getGrpcStreamingType() == GrpcStreamingType.ClientStreaming) {
        // Client-streaming not yet supported

        continue;
      }
      result.add(method);
    }
    return result;
  }

  public List<ParamWithSimpleDoc> callSettingsParam() {
    return ImmutableList.of(
        makeParam(
            "CallSettings",
            "callSettings",
            "null",
            "If not null, applies overrides to this RPC call."));
  }

  public List<ParamWithSimpleDoc> cancellationTokenParam() {
    return ImmutableList.of(
        makeParam(
            "CancellationToken",
            "cancellationToken",
            null,
            "A <see cref=\"CancellationToken\"/> to use for this RPC."));
  }

  public List<ParamWithSimpleDoc> pagedMethodAdditionalParams() {
    return ImmutableList.of(
        makeParam(
            "string",
            "pageToken",
            "null",
            "The token returned from the previous request.",
            "A value of <c>null</c> or an empty string retrieves the first page."),
        makeParam(
            "int?",
            "pageSize",
            "null",
            "The size of page to request. The response will not be larger than this, but may be smaller.",
            "A value of <c>null</c> or 0 uses a server-defined page size."));
  }

  public ParamWithSimpleDoc makeParam(
      String typeName, String name, String defaultValue, String... doc) {
    return ParamWithSimpleDoc.newBuilder()
        .name(name)
        .elementTypeName("")
        .typeName(typeName)
        .setCallName("")
        .addCallName("")
        .getCallName("")
        .isMap(false)
        .isArray(false)
        .isPrimitive(false)
        .isOptional(false)
        .defaultValue(defaultValue)
        .docLines(Arrays.asList(doc))
        .build();
  }
}
