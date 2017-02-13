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
package com.google.api.codegen.transformer;

import com.google.api.codegen.CodegenTestUtil;
import com.google.api.tools.framework.model.EnumType;
import com.google.api.tools.framework.model.MessageType;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.ProtoContainerElement;
import com.google.api.tools.framework.model.ProtoFile;
import com.google.api.tools.framework.model.TypeRef;
import com.google.api.tools.framework.model.testing.TestDataLocator;
import org.junit.rules.TemporaryFolder;

public class ModelTypeNameConverterTestUtil {
  public static TypeRef getTestEnumType(TemporaryFolder tempDir) {
    return getTestType(tempDir, "Book", "Rating");
  }

  public static TypeRef getTestType(TemporaryFolder tempDir, String... path) {
    String fileName = "library.proto";
    TestDataLocator locator = TestDataLocator.create(CodegenTestUtil.class);
    Model model =
        CodegenTestUtil.readModel(
            locator, tempDir, new String[] {fileName}, new String[] {"library.yaml"});
    ProtoContainerElement container = null;
    for (ProtoFile file : model.getFiles()) {
      if (file.getSimpleName().equals(fileName)) {
        container = file;
        break;
      }
    }
    if (container == null) {
      throw new IllegalStateException("file not found: " + fileName);
    }

    pathLoop:
    for (int i = 0; i < path.length; i++) {
      String pathElement = path[i];
      for (MessageType message : container.getMessages()) {
        if (message.getSimpleName().equals(pathElement)) {
          container = message;
          continue pathLoop;
        }
      }
      for (EnumType enumType : container.getEnums()) {
        if (enumType.getSimpleName().equals(pathElement)) {
          if (i != path.length - 1) {
            throw new IllegalStateException(
                "enum type cannot contain further elements: " + enumType);
          }
          return TypeRef.of(enumType);
        }
      }
      throw new IllegalStateException("element not found: " + pathElement);
    }

    if (container instanceof MessageType) {
      return TypeRef.of((MessageType) container);
    }
    throw new IllegalStateException("not a type: " + container);
  }
}
