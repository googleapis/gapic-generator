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
package com.google.api.codegen.transformer;

import com.google.api.codegen.config.MethodModel;
import com.google.api.codegen.viewmodel.GrpcStubView;
import com.google.api.tools.framework.model.Interface;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class GrpcStubTransformer {
  public List<GrpcStubView> generateGrpcStubs(GapicInterfaceContext context) {
    List<GrpcStubView> stubs = new ArrayList<>();
    Map<String, Interface> interfaces = new TreeMap<>();
    Map<String, List<MethodModel>> methods = new TreeMap<>();
    for (MethodModel method : context.getSupportedMethods()) {
      Interface targetInterface = context.asRequestMethodContext(method).getTargetInterface();
      interfaces.put(targetInterface.getFullName(), targetInterface);
      if (methods.containsKey(targetInterface.getFullName())) {
        methods.get(targetInterface.getFullName()).add(method);
      } else {
        methods.put(targetInterface.getFullName(), new ArrayList<>(Arrays.asList(method)));
      }
    }

    for (Map.Entry<String, Interface> entry : interfaces.entrySet()) {
      Interface apiInterface = entry.getValue();
      stubs.add(generateGrpcStub(context, apiInterface, methods.get(entry.getKey())));
    }

    return stubs;
  }

  public GrpcStubView generateGrpcStub(
      GapicInterfaceContext context, Interface targetInterface, List<MethodModel> methods) {
    SurfaceNamer namer = context.getNamer();
    GrpcStubView.Builder stub = GrpcStubView.newBuilder();

    stub.name(namer.getStubName(targetInterface));
    stub.fullyQualifiedType(namer.getFullyQualifiedStubType(targetInterface));
    stub.createStubFunctionName(namer.getCreateStubFunctionName(targetInterface));
    String grpcClientTypeName =
        namer.getAndSaveNicknameForGrpcClientTypeName(context.getModelTypeTable(), targetInterface);
    stub.grpcClientTypeName(grpcClientTypeName);
    stub.grpcClientVariableName(namer.getGrpcClientVariableName(targetInterface));
    stub.grpcClientImportName(namer.getGrpcClientImportName(targetInterface));

    List<String> methodNames = new ArrayList<>();
    for (MethodModel method : methods) {
      methodNames.add(
          namer.getApiMethodName(method, context.getMethodConfig(method).getVisibility()));
    }
    stub.methodNames(methodNames);

    stub.stubMethodsArrayName(namer.getStubMethodsArrayName(targetInterface));
    stub.namespace(namer.getNamespace(targetInterface));
    stub.protoFileName(targetInterface.getFile().getSimpleName());

    return stub.build();
  }
}
