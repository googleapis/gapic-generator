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

import com.google.api.codegen.CodegenTestUtil;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.protoannotations.GapicCodeGeneratorAnnotationsTest;
import com.google.api.tools.framework.model.BoundedDiagCollector;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.MessageType;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.ProtoFile;
import com.google.api.tools.framework.model.testing.TestDataLocator;
import com.google.common.collect.ImmutableSet;
import com.google.longrunning.OperationInfo;
import java.util.List;
import java.util.Map;
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
  private static ProtoFile bookFromAnywhereProtoFile;
  private static Field shelfNameField;
  private static Interface libraryService;
  private static Method deleteShelfMethod;
  private static Method getBigBookMethod;
  private static MessageType book;
  private static MessageType shelf;
  private static final DiagCollector diagCollector = new BoundedDiagCollector();

  // Object under test.
  private static ProtoParser protoParser = new ProtoParser(true);

  @BeforeClass
  public static void startUp() {
    // Load and parse protofile.

    testDataLocator = TestDataLocator.create(GapicCodeGeneratorAnnotationsTest.class);
    testDataLocator.addTestDataSource(CodegenTestUtil.class, "testsrc/common");

    model =
        CodegenTestUtil.readModel(
            testDataLocator, tempDir, protoFiles, new String[] {"library.yaml"});

    libraryProtoFile =
        model
            .getFiles()
            .stream()
            .filter(f -> f.getSimpleName().equals("library.proto"))
            .findFirst()
            .get();

    bookFromAnywhereProtoFile =
        model
            .getFiles()
            .stream()
            .filter(f -> f.getSimpleName().equals("book_from_anywhere.proto"))
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

    book =
        libraryProtoFile
            .getMessages()
            .stream()
            .filter(m -> m.getSimpleName().equals("Book"))
            .findFirst()
            .get();
    shelfNameField =
        shelf.getFields().stream().filter(f -> f.getSimpleName().equals("name")).findFirst().get();

    libraryService = libraryProtoFile.getInterfaces().get(0);
    deleteShelfMethod = libraryService.lookupMethod("DeleteShelf");
    getBigBookMethod = libraryService.lookupMethod("GetBigBook");

    // resourceDefs = protoParser.getResourceDefs(Arrays.asList(libraryProtoFile), diagCollector);
    // resourceSetDefs =
    //    protoParser.getResourceSetDefs(Arrays.asList(libraryProtoFile), diagCollector);
  }

  @Test
  public void testGetPackageName() {
    String packageName = GapicProductConfig.getPackageName(model);
    assertThat(packageName).isEqualTo("google.example.library.v1");
  }

  @Test
  public void testGetEmptyResource() {
    MessageType book =
        libraryProtoFile
            .getMessages()
            .stream()
            .filter(m -> m.getSimpleName().equals("Book"))
            .findFirst()
            .get();
    Field authorBookField =
        book.getFields().stream().filter(f -> f.getSimpleName().equals("author")).findFirst().get();
    assertThat(protoParser.getResourceReference(authorBookField)).isNull();
  }

  @Test
  public void testGetLongRunningOperation() {
    OperationInfo operationTypes = protoParser.getLongRunningOperation(getBigBookMethod);

    OperationInfo expected =
        OperationInfo.newBuilder()
            .setResponseType("google.example.library.v1.Book")
            .setMetadataType("google.example.library.v1.GetBigBookMetadata")
            .build();
    assertThat(operationTypes).isEqualTo(expected);
  }

  @Test
  public void testIsHttpGetMethod() {
    assertThat(protoParser.isHttpGetMethod(deleteShelfMethod)).isFalse();
    assertThat(protoParser.isHttpGetMethod(getBigBookMethod)).isTrue();
  }

  @Test
  public void testGetServiceAddress() {
    String defaultHost = protoParser.getServiceAddress(libraryService);
    assertThat(defaultHost).isEqualTo("library-example.googleapis.com:1234");
  }

  @Test
  public void testGetRequiredFields() {
    Method publishSeriesMethod = libraryService.lookupMethod("PublishSeries");
    List<String> requiredFields = protoParser.getRequiredFields(publishSeriesMethod);
    assertThat(requiredFields).containsExactly("books", "series_uuid", "shelf");
  }

  @Test
  public void testGetResourceType() {
    MessageType getShelfRequest =
        libraryProtoFile
            .getMessages()
            .stream()
            .filter(m -> m.getSimpleName().equals("GetShelfRequest"))
            .findFirst()
            .get();
    Field shelves =
        getShelfRequest
            .getFields()
            .stream()
            .filter(f -> f.getSimpleName().equals("name"))
            .findFirst()
            .get();
    String shelfType = protoParser.getResourceReference(shelves).getType();
    assertThat(shelfType).isEqualTo("library.googleapis.com/Shelf");
  }

  @Test
  public void testGetMethodSignatures() {
    Method getShelfMethod = libraryService.lookupMethod("GetShelf");
    List<List<String>> getShelfFlattenings = protoParser.getMethodSignatures(getShelfMethod);
    assertThat(getShelfFlattenings.size()).isEqualTo(3);

    List<String> firstSignature = getShelfFlattenings.get(0);
    assertThat(firstSignature.size()).isEqualTo(1);
    assertThat(firstSignature.get(0)).isEqualTo("name");

    List<String> additionalSignature = getShelfFlattenings.get(1);
    assertThat(additionalSignature.size()).isEqualTo(2);
    assertThat(additionalSignature.get(0)).isEqualTo("name");
    assertThat(additionalSignature.get(1)).isEqualTo("message");

    List<String> additionalSignature2 = getShelfFlattenings.get(2);
    assertThat(additionalSignature2).containsExactly("name", "message", "string_builder");
  }

  @Test
  public void testEmptySignature() {
    // Test that we can detect empty method signatures.
    Method listShelvesMethod = libraryService.lookupMethod("ListShelves");
    List<List<String>> listShelvesFlattenings = protoParser.getMethodSignatures(listShelvesMethod);
    assertThat(listShelvesFlattenings.size()).isEqualTo(1);
    List<String> emptySignature = listShelvesFlattenings.get(0);
    assertThat(emptySignature.size()).isEqualTo(0);
  }

  @Test
  public void testNoSignature() {
    // Test that we can detect the absence of method signatures.
    Method streamShelvesMethod = libraryService.lookupMethod("StreamShelves");
    List<List<String>> listShelvesFlattenings =
        protoParser.getMethodSignatures(streamShelvesMethod);
    assertThat(listShelvesFlattenings.size()).isEqualTo(0);
  }

  /** The OAuth scopes for this service (e.g. "https://cloud.google.com/auth/cloud-platform"). */
  @Test
  public void testGetAuthScopes() {
    List<String> scopes = protoParser.getAuthScopes(libraryService);
    assertThat(scopes)
        .containsExactly(
            "https://www.googleapis.com/auth/library",
            "https://www.googleapis.com/auth/cloud-platform");
  }

  @Test
  public void testHttpRuleUrl() {
    ImmutableSet<String> deleteHeaderParams = protoParser.getHeaderParams(deleteShelfMethod);
    assertThat(deleteHeaderParams).containsExactly("name");

    Method publishMethod = libraryService.lookupMethod("PublishSeries");
    ImmutableSet<String> publishHeaderParams = protoParser.getHeaderParams(publishMethod);
    assertThat(publishHeaderParams).containsExactly("shelf.name");
  }

  @Test
  public void testGetFieldNamePatterns() {
    Method publishMethod = libraryService.lookupMethod("PublishSeries");
    Map<String, String> fieldNamePatterns = protoParser.getFieldNamePatterns(publishMethod);
    assertThat(fieldNamePatterns.size()).isEqualTo(2);
    assertThat(fieldNamePatterns.get("shelf.name")).isEqualTo("Shelf");
    assertThat(fieldNamePatterns.get("books.name")).isEqualTo("BookOneof");
  }
}
