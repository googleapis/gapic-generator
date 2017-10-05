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
package com.google.api.codegen.viewmodel.testing;

import com.google.auto.value.AutoValue;
import javax.annotation.Nullable;

@AutoValue
public abstract class ClientTestAssertView {
  public abstract String actualValueGetter();

  public abstract String expectedValueIdentifier();

  public abstract boolean isArray();

  @Nullable
  public abstract String expectedValueTransformFunction();

  @Nullable
  public abstract String actualValueTransformFunction();

  // Enum type name is needed for ruby because enum fields are converted to symbols when assigned
  // for a message. In order to do comparison, the enum value will need to be resolved to the
  // corresponding symbol using the enumTypeName.
  @Nullable
  public abstract String enumTypeName();

  // Message type name is needed for ruby since the value of the init code is in the form of a hash
  // and will need to be converted to its message type in order to compare with what is in the
  // request.
  @Nullable
  public abstract String messageTypeName();

  public boolean hasExpectedValueTransformFunction() {
    return expectedValueTransformFunction() != null;
  }

  public boolean hasActualValueTransformFunction() {
    return actualValueTransformFunction() != null;
  }

  public boolean hasEnumTypeName() {
    return enumTypeName() != null;
  }

  public boolean hasMessageTypeName() {
    return messageTypeName() != null;
  }

  public static Builder newBuilder() {
    return new AutoValue_ClientTestAssertView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder actualValueGetter(String val);

    public abstract Builder expectedValueIdentifier(String val);

    public abstract Builder isArray(boolean val);

    public abstract Builder expectedValueTransformFunction(String val);

    public abstract Builder actualValueTransformFunction(String val);

    public abstract Builder enumTypeName(String val);

    public abstract Builder messageTypeName(String val);

    public abstract ClientTestAssertView build();
  }
}
