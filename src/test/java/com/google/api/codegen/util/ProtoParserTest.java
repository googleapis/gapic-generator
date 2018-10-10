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
package com.google.api.codegen.util;

import static com.google.common.truth.Truth.assertThat;

import com.google.api.MethodSignature;
import com.google.api.Retry;
import com.google.api.codegen.CodegenTestUtil;
import com.google.api.codegen.protoannotations.GapicCodeGeneratorAnnotationsTest;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.MessageType;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.ProtoFile;
import com.google.api.tools.framework.model.testing.TestDataLocator;
import com.google.longrunning.OperationTypes;
import com.google.rpc.Code;
import java.util.Collections;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ProtoParserTest {
  private static String[] protoFiles = {"library.proto"};

  @ClassRule public static TemporaryFolder tempDir = new TemporaryFolder();
  private static Model model;
  private static TestDataLocator testDataLocator;
  private static ProtoFile libraryProtoFile;
  private static Field shelfNameField;
  private static Interface libraryService;
  private static Method deleteShelfMethod;
  private static Method getBigBookMethod;
  private static MessageType shelf;

  // Object under test.
  private static ProtoParser protoParser = new ProtoParser();

  @BeforeClass
  public static void startUp() {
    // Load and parse protofile.

    testDataLocator = TestDataLocator.create(GapicCodeGeneratorAnnotationsTest.class);
    testDataLocator.addTestDataSource(CodegenTestUtil.class, "testsrc/common");

    model = CodegenTestUtil.readModel(testDataLocator, tempDir, protoFiles, new String[0]);

    libraryProtoFile =
        model
            .getFiles()
            .stream()
            .filter(f -> f.getSimpleName().equals("library.proto"))
            .findFirst()
            .get();

    model.addRoot(libraryProtoFile);

    libraryService = libraryProtoFile.getInterfaces().get(0);

    shelf =
        libraryProtoFile
            .getMessages()
            .stream()
            .filter(m -> m.getSimpleName().equals("Shelf"))
            .findFirst()
            .get();
    shelfNameField =
        shelf.getFields().stream().filter(f -> f.getSimpleName().equals("name")).findFirst().get();

    libraryService = libraryProtoFile.getInterfaces().get(0);
    deleteShelfMethod = libraryService.lookupMethod("DeleteShelf");
    getBigBookMethod = libraryService.lookupMethod("GetBigBook");
  }

  @Test
  public void testGetResourcePath() {
    Field shelfNameField =
        shelf.getFields().stream().filter(f -> f.getSimpleName().equals("name")).findFirst().get();
    assertThat(protoParser.getResourcePath(shelfNameField)).isEqualTo("shelves/*");
  }

  @Test
  public void testGetEmptyResourcePath() {
    MessageType book =
        libraryProtoFile
            .getMessages()
            .stream()
            .filter(m -> m.getSimpleName().equals("Book"))
            .findFirst()
            .get();
    Field authorBookField =
        book.getFields().stream().filter(f -> f.getSimpleName().equals("author")).findFirst().get();
    assertThat(protoParser.getResourcePath(authorBookField)).isNull();
  }

  /** Return the entity name, e.g. "shelf" for a resource field. */
  @Test
  public void getResourceEntityName() {
    assertThat(protoParser.getResourceEntityName(shelfNameField)).isEqualTo("shelf");
  }

  @Test
  public void getLongRunningOperation() {
    OperationTypes operationTypes = protoParser.getLongRunningOperation(getBigBookMethod);

    OperationTypes expected =
        OperationTypes.newBuilder()
            .setResponse("google.example.library.v1.Book")
            .setMetadata("google.example.library.v1.GetBigBookMetadata")
            .build();
    assertThat(operationTypes).isEqualTo(expected);
  }

  @Test
  public void testGetRetryOnExplicitRetryCodes() {
    Retry deleteShelfRetry = protoParser.getRetry(deleteShelfMethod);
    Retry expectedDeleteShelfRetry =
        Retry.newBuilder().addCodes(Code.UNAVAILABLE).addCodes(Code.DEADLINE_EXCEEDED).build();
    assertThat(deleteShelfRetry).isEqualTo(expectedDeleteShelfRetry);
  }

  @Test
  public void testIsHttpGetMethod() {
    assertThat(protoParser.isHttpGetMethod(deleteShelfMethod)).isFalse();
    assertThat(protoParser.isHttpGetMethod(getBigBookMethod)).isTrue();
  }

  @Test
  public void testGetServiceAddress() {
    String defaultHost = protoParser.getServiceAddress(libraryService);
    assertThat(defaultHost).isEqualTo("library-example.googleapis.com");
  }

  @Test
  public void testGetRequiredFields() {
    Method publishSeriesMethod = libraryService.lookupMethod("PublishSeries");
    List<String> requiredFields = protoParser.getRequiredFields(publishSeriesMethod);
    Collections.sort(requiredFields);
    assertThat(requiredFields.size()).isEqualTo(2);
    assertThat(requiredFields.get(0)).isEqualTo("books");
    assertThat(requiredFields.get(1)).isEqualTo("shelf");
  }

  @Test
  public void testGetResourceType() {
    MessageType listShelvesResponse =
        libraryProtoFile
            .getMessages()
            .stream()
            .filter(m -> m.getSimpleName().equals("ListShelvesResponse"))
            .findFirst()
            .get();
    Field shelves =
        listShelvesResponse
            .getFields()
            .stream()
            .filter(f -> f.getSimpleName().equals("shelves"))
            .findFirst()
            .get();
    String shelfType = protoParser.getResourceType(shelves);
    assertThat(shelfType).isEqualTo("google.example.library.v1.Shelf");
  }

  @Test
  public void testGetMethodSignatures() {
    Method getShelfMethod =
        libraryProtoFile
            .getInterfaces()
            .stream()
            .filter(i -> i.lookupMethod("GetShelf") != null)
            .findFirst()
            .get()
            .lookupMethod("GetShelf");
    List<MethodSignature> getShelfFlattenings = protoParser.getMethodSignatures(getShelfMethod);
    assertThat(getShelfFlattenings.size()).isEqualTo(2);

    MethodSignature firstSignature = getShelfFlattenings.get(0);
    assertThat(firstSignature.getFieldsList().size()).isEqualTo(1);
    assertThat(firstSignature.getFieldsList().get(0)).isEqualTo("name");

    MethodSignature additionalSignature = getShelfFlattenings.get(1);
    assertThat(additionalSignature.getFieldsList().size()).isEqualTo(2);
    assertThat(additionalSignature.getFieldsList().get(0)).isEqualTo("name");
    assertThat(additionalSignature.getFieldsList().get(1)).isEqualTo("message");
  }

  /** The OAuth scopes for this service (e.g. "https://cloud.google.com/auth/cloud-platform"). */
  @Test
  public void testGetAuthScopes() {
    List<String> scopes = protoParser.getAuthScopes(libraryService);
    assertThat(scopes.size()).isEqualTo(2);
    assertThat(scopes.get(0)).isEqualTo("https://www.googleapis.com/auth/library");
    assertThat(scopes.get(1)).isEqualTo("https://www.googleapis.com/auth/cloud-platform");
  }
}
