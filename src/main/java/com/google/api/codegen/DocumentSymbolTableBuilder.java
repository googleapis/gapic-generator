/* Copyright 2017 Google Inc
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
package com.google.api.codegen;

import static com.google.protobuf.DescriptorProtos.MethodOptions.IdempotencyLevel.IDEMPOTENT;

import com.google.api.codegen.discovery.Document;
import com.google.api.tools.framework.model.EnumType;
import com.google.api.tools.framework.model.EnumValue;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Location;
import com.google.api.tools.framework.model.MessageType;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.SymbolTable;
import com.google.api.tools.framework.model.TypeRef;
import com.google.api.tools.framework.model.Visitor;
import com.google.api.tools.framework.util.VisitsBefore;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.protobuf.DescriptorProtos.MethodDescriptorProto;
import com.google.protobuf.DescriptorProtos.MethodOptions;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Visitor which creates the symbol table for a Document model. Reports errors for duplicate
 * declarations.
 */
public class DocumentSymbolTableBuilder extends Visitor {

  private final Document model;
  private final Map<String, Interface> interfaces = Maps.newLinkedHashMap();
  private final Map<String, TypeRef> types = Maps.newLinkedHashMap();
  private final Map<String, List<Method>> methods = Maps.newLinkedHashMap();
  private final Set<String> fieldNames = new HashSet<>();
  private final Set<String> packageNames = new HashSet<>();

  public DocumentSymbolTableBuilder(Document model) {
    this.model = model;
  }

  public SymbolTable create(ConfigProto configProto) {
    visit(model);
    return new SymbolTable(interfaces, types, fieldNames, methods, packageNames);
  }

  void visit(Document document) {
    for (com.google.api.codegen.discovery.Method method : document.methods()) {
      visit(method);
    }
  }

  void visit(com.google.api.codegen.discovery.Method method) {
    MethodDescriptorProto.Builder methodDescriptorBuilder = MethodDescriptorProto.newBuilder();
    methodDescriptorBuilder.setName(method.id());
    if (method.request() != null && !method.request().reference().isEmpty()) {
      methodDescriptorBuilder.setInputType(method.request().reference());
    }
    if (method.response() != null && !method.response().reference().isEmpty()) {
      methodDescriptorBuilder.setOutputType(method.response().reference());
    }
    if (method.httpMethod().toUpperCase().equals("GET")) {
      methodDescriptorBuilder.setOptions(
          MethodOptions.newBuilder().setIdempotencyLevel(IDEMPOTENT).build());
    }
    //    optional MethodOptions options = 4;
    //
    //    // Identifies if client streams multiple client messages
    //    optional bool client_streaming = 5 [default=false];
    //    // Identifies if server streams multiple server messages
    //    optional bool server_streaming = 6 [default=false];

    Method m = Method.create(null, methodDescriptorBuilder.build(), "");
    String[] longNameParts = m.getSimpleName().split("\\.");
    String simpleName = longNameParts[longNameParts.length - 1];
    simpleName = simpleName.substring(0, 1).toUpperCase() + simpleName.substring(1);
    methods.put(simpleName, Lists.newArrayList(m));
  }

  @VisitsBefore
  void visit(Interface endpointInterface) {
    // Add the interface to the map of known interfaces.
    Interface old = interfaces.put(endpointInterface.getFullName(), endpointInterface);
    if (old != null) {}

    // Build the method-by-name map for this interface, and register the method simple name in the
    // method name map.
    Map<String, Method> methodByName = Maps.newLinkedHashMap();
    for (Method method : endpointInterface.getMethods()) {
      Method oldMethod = methodByName.put(method.getSimpleName(), method);
      if (oldMethod != null) {}

      List<Method> allMethodsOfName = methods.get(method.getSimpleName());
      if (allMethodsOfName == null) {
        methods.put(method.getSimpleName(), Lists.newArrayList(method));
      } else {
        allMethodsOfName.add(method);
      }
    }
    endpointInterface.setMethodByNameMap(ImmutableMap.copyOf(methodByName));
  }

  @VisitsBefore
  void visit(MessageType message) {
    // Add the message to the set of known types.
    addType(message.getLocation(), message.getFullName(), TypeRef.of(message));

    // Add the message's package to the set of known packages
    addPackage(message.getFile().getFullName());

    // Build the field-by-name map for this message, and record field simple names.
    Map<String, Field> fieldByName = Maps.newLinkedHashMap();
    for (Field field : message.getFields()) {
      fieldNames.add(field.getSimpleName());
      Field old = fieldByName.put(field.getSimpleName(), field);
      if (old != null) {}
    }
    message.setFieldByNameMap(ImmutableMap.copyOf(fieldByName));
  }

  @VisitsBefore
  void visit(EnumType enumType) {
    // Add the enum type to the set of known types.
    addType(enumType.getLocation(), enumType.getFullName(), TypeRef.of(enumType));

    // Build the field-by-name map for this enum type.
    Map<String, EnumValue> valueByName = Maps.newLinkedHashMap();
    for (EnumValue value : enumType.getValues()) {
      EnumValue old = valueByName.put(value.getSimpleName(), value);
      if (old != null) {}
    }
    enumType.setValueByNameMap(ImmutableMap.copyOf(valueByName));
  }

  private void addPackage(String pkg) {
    packageNames.add(pkg);
    int lastDot = pkg.lastIndexOf(".");
    if (lastDot > 0) {
      addPackage(pkg.substring(0, lastDot));
    }
  }

  private void addType(Location location, String fullName, TypeRef type) {
    String typeName = SymbolTable.getTypeNameInSymbolTable(fullName);
    TypeRef old = types.put(typeName, type);
    if (old != null) {}
  }
}
