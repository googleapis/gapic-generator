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
package com.google.api.codegen.discovery;

import com.google.api.codegen.ApiaryConfig;
import com.google.api.codegen.DiscoveryImporter;
import com.google.auto.value.AutoValue;
import com.google.protobuf.Field;
import com.google.protobuf.Method;
import com.google.protobuf.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;

@AutoValue
public abstract class MethodInfo {

  public abstract List<String> resources();

  public abstract String name();

  public abstract List<FieldInfo> fields();

  @Nullable
  public abstract TypeInfo inputType();

  @Nullable
  public abstract TypeInfo inputRequestType();

  @Nullable
  public abstract TypeInfo outputType();

  public abstract boolean isPageStreaming();

  @Nullable
  public abstract FieldInfo pageStreamingResourceField();

  public static Builder newBuilder() {
    return new AutoValue_MethodInfo.Builder();
  }

  @AutoValue.Builder
  public static abstract class Builder {

    public abstract Builder resources(List<String> val);

    public abstract Builder name(String val);

    public abstract Builder fields(List<FieldInfo> val);

    public abstract Builder inputType(TypeInfo val);

    public abstract Builder inputRequestType(TypeInfo val);

    public abstract Builder outputType(TypeInfo val);

    public abstract Builder isPageStreaming(boolean val);

    public abstract Builder pageStreamingResourceField(FieldInfo val);

    public abstract MethodInfo build();
  }
}
