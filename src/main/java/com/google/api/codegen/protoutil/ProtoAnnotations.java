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
package com.google.api.codegen.protoutil;

import com.google.api.AnnotationsProto;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.ProtoFile;
import com.google.protobuf.ExtensionRegistry;

public class ProtoAnnotations {
  // Full name of the protofile containing proto annotations definitions.
  public static final String PROTO_FULL_NAME = "google/api/annotations.proto";
  public static ExtensionRegistry registry = ExtensionRegistry.newInstance();

  public ProtoAnnotations(Model model) {
    ProtoFile annotationProto =
        model
            .getFiles()
            .stream()
            .filter(f -> f.getProto().getName().equals(PROTO_FULL_NAME))
            .findFirst()
            .get();
    AnnotationsProto.registerAllExtensions(registry);
    registry.toString();
  }

  //  public static void parseExtension() {
  //    registry.add(MyProto.bar);
  //    MyProto.Foo message = MyProto.Foo.parseFrom(input, registry);
  //  }
}
