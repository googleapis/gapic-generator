/* Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.api.codegen.viewmodel;

import com.google.auto.value.AutoValue;

public interface AccessorView {
  enum Kind {
    MEMBER,
    INDEX
  }

  Kind kind();

  @AutoValue
  abstract class MemberView implements AccessorView {
    public abstract String member();

    public Kind kind() {
      return Kind.MEMBER;
    }

    public static Builder newBuilder() {
      return new AutoValue_AccessorView_MemberView.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
      public abstract Builder member(String val);

      public abstract MemberView build();
    }
  }

  @AutoValue
  abstract class IndexView implements AccessorView {
    public abstract String index();

    public Kind kind() {
      return Kind.INDEX;
    }

    public static Builder newBuilder() {
      return new AutoValue_AccessorView_IndexView.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
      public abstract Builder index(String val);

      public abstract IndexView build();
    }
  }
}
