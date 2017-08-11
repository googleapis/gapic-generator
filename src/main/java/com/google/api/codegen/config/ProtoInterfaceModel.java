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
package com.google.api.codegen.config;

import com.google.api.tools.framework.model.Interface;

/** Protobuf-based InterfaceModel. */
public class ProtoInterfaceModel implements InterfaceModel {
  private final Interface protoInterface;

  public ProtoInterfaceModel(Interface protoInterface) {
    this.protoInterface = protoInterface;
  }

  public Interface getInterface() {
    return protoInterface;
  }

  @Override
  public ApiSource getApiSource() {
    return ApiSource.PROTO;
  }

  @Override
  public String getSimpleName() {
    return protoInterface.getSimpleName();
  }

  @Override
  public String getFullName() {
    return protoInterface.getFullName();
  }

  @Override
  public String getParentFullName() {
    return protoInterface.getParent().getFullName();
  }

  @Override
  public String getFileSimpleName() {
    return protoInterface.getFile().getSimpleName();
  }

  @Override
  public String getFileFullName() {
    return protoInterface.getFile().getFullName();
  }

  @Override
  public boolean equals(Object o) {
    return o != null
        && o instanceof ProtoInterfaceModel
        && ((ProtoInterfaceModel) o).protoInterface.equals(this.protoInterface);
  }
}
