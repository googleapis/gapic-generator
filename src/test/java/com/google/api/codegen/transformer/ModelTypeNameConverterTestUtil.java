/* Copyright 2017 Google LLC
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
import com.google.api.tools.framework.model.ProtoElement;
import com.google.api.tools.framework.model.TypeRef;
import com.google.api.tools.framework.model.testing.TestDataLocator;
import java.util.Collection;
import org.junit.rules.TemporaryFolder;

public class ModelTypeNameConverterTestUtil {
  public static TypeRef getTestEnumType(TemporaryFolder tempDir) {
    return getTestType(tempDir, "Book", "Rating");
  }

  public static TypeRef getTestType(TemporaryFolder tempDir, String... path) {
    String fileName = "library.proto";
    TestDataLocator locator = TestDataLocator.create(CodegenTestUtil.class);
    locator.addTestDataSource(CodegenTestUtil.class, "testsrc");
    Model model =
        CodegenTestUtil.readModel(
            locator, tempDir, new String[] {fileName}, new String[] {"library.yaml"});
    ProtoContainerElement container = getElementWithName(model.getFiles(), fileName);
    if (container == null) {
      throw new IllegalStateException("file not found: " + fileName);
    }

    for (int i = 0; i < path.length; i++) {
      String pathElement = path[i];
      MessageType messageType = getElementWithName(container.getMessages(), pathElement);
      EnumType enumType = getElementWithName(container.getEnums(), pathElement);
      if (messageType != null) {
        container = messageType;
      } else if (enumType != null && i == path.length - 1) {
        return TypeRef.of(enumType);
      } else if (enumType != null) {
        throw new IllegalStateException("enum type cannot contain further elements: " + enumType);
      } else {
        throw new IllegalStateException("element not found: " + pathElement);
      }
    }

    if (container instanceof MessageType) {
      return TypeRef.of((MessageType) container);
    }
    throw new IllegalStateException("not a type: " + container);
  }

  private static <E extends ProtoElement> E getElementWithName(
      Collection<E> elements, String name) {
    for (E e : elements) {
      if (e.getSimpleName().equals(name)) {
        return e;
      }
    }
    return null;
  }
}
