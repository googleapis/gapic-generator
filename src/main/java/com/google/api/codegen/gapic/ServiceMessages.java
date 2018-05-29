/* Copyright 2016 Google LLC
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
package com.google.api.codegen.gapic;

import com.google.api.tools.framework.model.TypeRef;
import com.google.protobuf.Empty;

/** Utility class with methods to work with service methods. */
public class ServiceMessages {

  private static final String LRO_TYPE = "google.longrunning.Operation";

  /** Returns true if the message is the empty message. */
  public boolean isEmptyType(TypeRef type) {
    return s_isEmptyType(type);
  }

  public static boolean s_isEmptyType(TypeRef type) {
    return type.isMessage()
        && type.getMessageType().getFullName().equals(Empty.getDescriptor().getFullName());
  }
}
