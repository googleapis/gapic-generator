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
package com.google.api.codegen.viewmodel;

import com.google.auto.value.AutoValue;

import java.util.List;

@AutoValue
public abstract class BundlingDescriptorClassView {

  public abstract String name();

  public abstract String requestTypeName();

  public abstract String responseTypeName();

  public abstract String bundledFieldTypeName();

  public abstract String subresponseTypeName();

  public abstract List<BundlingPartitionKeyView> partitionKeys();

  public abstract List<FieldCopyView> discriminatorFieldCopies();

  public abstract String fnGetBundledField();

  public abstract String fnSetBundledField();

  public abstract String fnGetBundledFieldCount();

  public abstract String fnGetSubresponseByIndex();

  public abstract String fnSetSubresponse();

  public static Builder newBuilder() {
    return new AutoValue_BundlingDescriptorClassView.Builder();
  }

  @AutoValue.Builder
  public static abstract class Builder {

    public abstract Builder name(String val);

    public abstract Builder requestTypeName(String val);

    public abstract Builder responseTypeName(String val);

    public abstract Builder bundledFieldTypeName(String val);

    public abstract Builder subresponseTypeName(String val);

    public abstract Builder partitionKeys(List<BundlingPartitionKeyView> val);

    public abstract Builder discriminatorFieldCopies(
        List<FieldCopyView> generateDiscriminatorFieldCopies);

    public abstract Builder fnGetBundledField(String val);

    public abstract Builder fnSetBundledField(String val);

    public abstract Builder fnGetBundledFieldCount(String val);

    public abstract Builder fnGetSubresponseByIndex(String val);

    public abstract Builder fnSetSubresponse(String val);

    public abstract BundlingDescriptorClassView build();
  }
}
