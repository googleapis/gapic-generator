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
import com.google.api.OperationData;
import com.google.api.Resource;
import com.google.api.ResourceSet;
import com.google.api.codegen.CodegenTestUtil;
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
import java.util.Arrays;
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
  private static Field shelfNameField;
  private static Interface libraryService;
  private static Method deleteShelfMethod;
  private static Method getBigBookMethod;
  private static MessageType book;
  private static MessageType shelf;
  private static Map<Resource, ProtoFile> resourceDefs;
  private static Map<ResourceSet, ProtoFile> resourceSetDefs;
  private static final DiagCollector diagCollector = new BoundedDiagCollector();

  // Object under test.
  private static ProtoParser protoParser = new ProtoParser();

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

    resourceDefs = protoParser.getResourceDefs(Arrays.asList(libraryProtoFile), diagCollector);
    resourceSetDefs =
        protoParser.getResourceSetDefs(Arrays.asList(libraryProtoFile), diagCollector);
  }

  @Test
  public void testGetPackageName() {
    String packageName = protoParser.getPackageName(model);
    assertThat(packageName).isEqualTo("google.example.library.v1");
  }

  @Test
  public void testGetResourcePath() {
    Field shelfNameField =
        shelf.getFields().stream().filter(f -> f.getSimpleName().equals("name")).findFirst().get();
    assertThat(protoParser.getResource(shelfNameField).getPath()).isEqualTo("shelves/{shelf_id}");
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
    assertThat(protoParser.getResource(authorBookField)).isNull();
  }

  /** Return the entity name, e.g. "shelf" for a resource field. */
  @Test
  public void testGetResourceEntityName() {
    assertThat(protoParser.getResourceEntityName(shelfNameField)).isEqualTo("Shelf");
  }

  @Test
  public void testGetResourceSet() {
    Field bookNameField =
        book.getFields().stream().filter(f -> f.getSimpleName().equals("name")).findFirst().get();
    ResourceSet bookResourceSet = protoParser.getResourceSet(bookNameField);
    assertThat(bookResourceSet).isNotNull();
    assertThat(bookResourceSet.getName()).isEqualTo("BookOneOf");
    assertThat(bookResourceSet.getResourcesCount()).isEqualTo(1);
    assertThat(bookResourceSet.getResources(0))
        .isEqualTo(Resource.newBuilder().setName("DeletedBook").setPath("_deleted-book_").build());
    assertThat(bookResourceSet.getResourceReferencesList()).containsExactly("ArchivedBook", "Book");
  }

  @Test
  public void testGetAllResourceDefs() {
    // resourceDefs has already been computed in the setUp() method.

    assertThat(resourceDefs).hasSize(4);
    assertThat(resourceDefs)
        .containsEntry(
            Resource.newBuilder().setName("Shelf").setPath("shelves/{shelf_id}").build(),
            libraryProtoFile);
    assertThat(resourceDefs)
        .containsEntry(
            Resource.newBuilder().setName("Project").setPath("projects/{project}").build(),
            libraryProtoFile);
    assertThat(resourceDefs)
        .containsEntry(
            Resource.newBuilder()
                .setName("Book")
                .setPath("shelves/{shelf_id}/books/{book_id}")
                .build(),
            libraryProtoFile);
    assertThat(resourceDefs)
        .containsEntry(
            Resource.newBuilder()
                .setName("ArchivedBook")
                .setPath("archives/{archive_path}/books/{book_id=**}")
                .build(),
            libraryProtoFile);
  }

  @Test
  public void testGetAllResourceSetDefs() {
    // resourceSetDefs has already been computed in the setUp() method.
    assertThat(resourceSetDefs).hasSize(1);
    assertThat(resourceSetDefs)
        .containsEntry(
            ResourceSet.newBuilder()
                .setName("BookOneOf")
                .addResources(
                    Resource.newBuilder().setName("DeletedBook").setPath("_deleted-book_"))
                .addResourceReferences("ArchivedBook")
                .addResourceReferences("Book")
                .build(),
            libraryProtoFile);
  }

  @Test
  public void testGetResourceTypeEntityNameFromOneof() {
    MessageType getBookFromAnywhereRequest =
        libraryProtoFile
            .getMessages()
            .stream()
            .filter(m -> m.getSimpleName().equals("GetBookFromAnywhereRequest"))
            .findFirst()
            .get();
    Field nameField =
        getBookFromAnywhereRequest
            .getFields()
            .stream()
            .filter(f -> f.getSimpleName().equals("name"))
            .findFirst()
            .get();
    assertThat(protoParser.getResourceReferenceName(nameField, resourceDefs, resourceSetDefs))
        .isEqualTo("BookOneOf");

    Field altBookNameField =
        getBookFromAnywhereRequest
            .getFields()
            .stream()
            .filter(f -> f.getSimpleName().equals("alt_book_name"))
            .findFirst()
            .get();
    assertThat(
            protoParser.getResourceReferenceName(altBookNameField, resourceDefs, resourceSetDefs))
        .isEqualTo("Book");
  }

  @Test
  public void getResourceEntityName() {
    assertThat(protoParser.getResourceEntityName(shelfNameField)).isEqualTo("Shelf");
  }

  @Test
  public void testGetLongRunningOperation() {
    OperationData operationTypes = protoParser.getLongRunningOperation(getBigBookMethod);

    OperationData expected =
        OperationData.newBuilder()
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
    String shelfType = protoParser.getResourceReference(shelves);
    assertThat(shelfType).isEqualTo("Shelf");
  }

  @Test
  public void testGetMethodSignatures() {
    Method getShelfMethod = libraryService.lookupMethod("GetShelf");
    List<MethodSignature> getShelfFlattenings = protoParser.getMethodSignatures(getShelfMethod);
    assertThat(getShelfFlattenings.size()).isEqualTo(3);

    MethodSignature firstSignature = getShelfFlattenings.get(0);
    assertThat(firstSignature.getFieldsList().size()).isEqualTo(1);
    assertThat(firstSignature.getFieldsList().get(0)).isEqualTo("name");

    MethodSignature additionalSignature = getShelfFlattenings.get(1);
    assertThat(additionalSignature.getFieldsList().size()).isEqualTo(2);
    assertThat(additionalSignature.getFieldsList().get(0)).isEqualTo("name");
    assertThat(additionalSignature.getFieldsList().get(1)).isEqualTo("message");

    MethodSignature additionalSignature2 = getShelfFlattenings.get(2);
    assertThat(additionalSignature2.getFieldsList())
        .containsExactly("name", "message", "string_builder");
  }

  @Test
  public void testEmptySignature() {
    // Test that we can detect empty method signatures.
    Method listShelvesMethod = libraryService.lookupMethod("ListShelves");
    List<MethodSignature> listShelvesFlattenings =
        protoParser.getMethodSignatures(listShelvesMethod);
    assertThat(listShelvesFlattenings.size()).isEqualTo(1);
    MethodSignature emptySignature = listShelvesFlattenings.get(0);
    assertThat(emptySignature.getFieldsList().size()).isEqualTo(0);
  }

  @Test
  public void testNoSignature() {
    // Test that we can detect the absence of method signatures.
    Method streamShelvesMethod = libraryService.lookupMethod("StreamShelves");
    List<MethodSignature> listShelvesFlattenings =
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
}
