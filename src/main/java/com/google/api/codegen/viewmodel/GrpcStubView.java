/* Copyright 2016 Google LLC
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
package com.google.api.codegen.viewmodel;

import com.google.auto.value.AutoValue;
import java.util.List;

@AutoValue
public abstract class GrpcStubView {
  public abstract String name();

  public abstract String fullyQualifiedType();

  public abstract String createStubFunctionName();

  public abstract String grpcClientVariableName();

  public abstract String grpcClientImportName();

  public abstract String grpcClientTypeName();

  public abstract List<String> methodNames();

  public abstract String stubMethodsArrayName();

  public abstract String protoFileName();

  public abstract String namespace();

  public static Builder newBuilder() {
    return new AutoValue_GrpcStubView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder name(String val);

    public abstract Builder fullyQualifiedType(String val);

    public abstract Builder createStubFunctionName(String val);

    public abstract Builder grpcClientVariableName(String val);

    public abstract Builder grpcClientImportName(String val);

    public abstract Builder grpcClientTypeName(String val);

    public abstract Builder methodNames(List<String> val);

    public abstract Builder stubMethodsArrayName(String val);

    public abstract Builder protoFileName(String val);

    public abstract Builder namespace(String val);

    public abstract GrpcStubView build();
  }
}
