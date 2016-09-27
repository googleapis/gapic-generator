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

import com.google.api.codegen.viewmodel.GrpcStubView;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class GrpcStubTransformer {
  public List<GrpcStubView> generateGrpcStubs(SurfaceTransformerContext context) {
    List<GrpcStubView> stubs = new ArrayList<>();
    SurfaceNamer namer = context.getNamer();

    Map<String, Interface> interfaces = new TreeMap<>();
    Map<String, List<String>> methods = new TreeMap<>();
    Map
    for (Method method : context.getSupportedMethods()) {
      Interface targetInterface = context.asMethodContext(method).getTargetInterface();
      interfaces.put(targetInterface.getFullName(), targetInterface);
      if (methods.containsKey(targetInterface.getFullName())) {
        methods.get(targetInterface.getFullName()).add(namer.getApiMethodName(method));
      } else {
        methods.put(
            targetInterface.getFullName(),
            new ArrayList<String>(Arrays.asList(namer.getApiMethodName(method))));
      }
    }

    for (String interfaceName : interfaces.keySet()) {
      Interface interfaze = interfaces.get(interfaceName);
      GrpcStubView.Builder stub = GrpcStubView.newBuilder();
      stub.name(namer.getStubName(interfaze));
      stub.fullyQualifiedType(namer.getFullyQualifiedStubType(interfaze));
      stub.createStubFunctionName(namer.getCreateStubFunctionName(interfaze));
      String grpcClientTypeName = namer.getGrpcClientTypeName(interfaze);
      stub.grpcClientTypeName(context.getTypeTable().getAndSaveNicknameFor(grpcClientTypeName));
      stub.grpcClientVariableName(namer.getGrpcClientVariableName(interfaze));
      stub.grpcClientImportName(namer.getGrpcClientImportName(interfaze));
      stub.methodNames(methods.get(interfaceName));
      stub.stubMethodsArrayName(namer.getStubMethodsArrayName(interfaze));
      stub.namespace(namer.getNamespace(interfaze));
      stub.protoFileName(interfaze.getFile().getSimpleName());
      stubs.add(stub.build());
    }

    return stubs;
  }

  public GrpcStubView generateGrpcStub(SurfaceTransformerContext context, Method method) {
    SurfaceNamer namer = context.getNamer();
    Interface targetInterface = context.asMethodContext(method).getTargetInterface();
    GrpcStubView.Builder stub = GrpcStubView.newBuilder();
    stub.name(namer.getStubName(targetInterface));
    stub.fullyQualifiedType(namer.getFullyQualifiedStubType(targetInterface));
    stub.createStubFunctionName(namer.getCreateStubFunctionName(targetInterface));
    String grpcClientTypeName = namer.getGrpcClientTypeName(targetInterface);
    stub.grpcClientTypeName(context.getTypeTable().getAndSaveNicknameFor(grpcClientTypeName));
    stub.grpcClientVariableName(namer.getGrpcClientVariableName(targetInterface));
    stub.grpcClientImportName(namer.getGrpcClientImportName(targetInterface));
    stub.methodNames(Collections.singletonList(namer.getApiMethodName(method)));
    stub.stubMethodsArrayName(namer.getStubMethodsArrayName(targetInterface));
    stub.namespace(namer.getNamespace(targetInterface));
    stub.protoFileName(targetInterface.getFile().getSimpleName());

    return stub.build();
  }
}
