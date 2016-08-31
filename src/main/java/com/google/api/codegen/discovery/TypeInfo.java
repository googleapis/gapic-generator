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

import com.google.auto.value.AutoValue;
import com.google.protobuf.Field;
import javax.annotation.Nullable;

@AutoValue
public abstract class TypeInfo {

  public abstract String name();

  public abstract Field.Kind kind();

  public abstract String doc();

  public abstract boolean isMap();

  @Nullable
  public abstract TypeInfo mapKey();

  @Nullable
  public abstract TypeInfo mapValue();

  public abstract boolean isArray();

  public static Builder newBuilder() {
    return new AutoValue_TypeInfo.Builder();
  }

  @AutoValue.Builder
  public static abstract class Builder {

    public abstract Builder name(String val);

    public abstract Builder kind(Field.Kind val);

    public abstract Builder doc(String val);

    public abstract Builder isMap(boolean val);

    public abstract Builder mapKey(@Nullable TypeInfo val);

    public abstract Builder mapValue(@Nullable TypeInfo val);

    public abstract Builder isArray(boolean val);

    public abstract TypeInfo build();
  }
}
