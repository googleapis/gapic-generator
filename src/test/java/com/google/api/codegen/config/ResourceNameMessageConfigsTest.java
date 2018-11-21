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
package com.google.api.codegen.config;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;

import com.google.api.MethodSignature;
import com.google.api.Resource;
import com.google.api.ResourceSet;
import com.google.api.codegen.CollectionConfigProto;
import com.google.api.codegen.CollectionOneofProto;
import com.google.api.codegen.ConfigProto;
import com.google.api.codegen.FixedResourceNameValueProto;
import com.google.api.codegen.FlatteningConfigProto;
import com.google.api.codegen.FlatteningGroupProto;
import com.google.api.codegen.InterfaceConfigProto;
import com.google.api.codegen.MethodConfigProto;
import com.google.api.codegen.ResourceNameMessageConfigProto;
import com.google.api.codegen.ResourceNameTreatment;
import com.google.api.codegen.common.TargetLanguage;
import com.google.api.codegen.util.ProtoParser;
import com.google.api.tools.framework.model.BoundedDiagCollector;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.MessageType;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.ProtoFile;
import com.google.api.tools.framework.model.TypeRef;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

public class ResourceNameMessageConfigsTest {
  private static final ProtoParser protoParser = Mockito.spy(ProtoParser.class);
  private static ConfigProto configProto;
  private static final Method createShelvesMethod = Mockito.mock(Method.class);
  private static final MessageType createShelvesRequest = Mockito.mock(MessageType.class);
  private static final MessageType createShelvesResponse = Mockito.mock(MessageType.class);
  private static final MessageType bookType = Mockito.mock(MessageType.class);
  private static final Field shelfName = Mockito.mock(Field.class);
  private static final Field shelfTheme = Mockito.mock(Field.class);
  private static final MessageType shelfMessage = Mockito.mock(MessageType.class);
  private static final Field bookName = Mockito.mock(Field.class);
  private static final Field bookAuthor = Mockito.mock(Field.class);
  private static final MessageType bookMessage = Mockito.mock(MessageType.class);
  private static final ProtoFile protoFile = Mockito.mock(ProtoFile.class);
  private static final ImmutableList<ProtoFile> sourceProtoFiles = ImmutableList.of(protoFile);
  private static final Method insertBook = Mockito.mock(Method.class);

  private static final String DEFAULT_PACKAGE = "library";
  private static final String GAPIC_SHELF_PATH = "shelves/{shelf_id}";
  private static final String GAPIC_BOOK_PATH = "shelves/{shelf_id}/books/{book_id}";
  private static final String ARCHIVED_BOOK_PATH = "archives/{archive_path}/books/{book_id=**}";
  private static final String PROTO_SHELF_PATH = "shelves/{shelf}";
  private static final String PROTO_BOOK_PATH = "bookShelves/{book}";
  private static final String CREATE_SHELF_METHOD_NAME = "CreateShelf";

  private static final Map<Resource, ProtoFile> allResourceDefs =
      ImmutableMap.of(
          Resource.newBuilder().setName("Shelf").setPath(PROTO_SHELF_PATH).build(),
          protoFile,
          Resource.newBuilder().setName("Book").setPath(PROTO_BOOK_PATH).build(),
          protoFile,
          Resource.newBuilder()
              .setName("archived_book")
              .setPath("archives/{archive}/books/{book}")
              .build(),
          protoFile);

  private static final Map<ResourceSet, ProtoFile> allResourceSetDefs = ImmutableMap.of();

  @BeforeClass
  public static void startUp() {
    configProto =
        ConfigProto.newBuilder()
            .addResourceNameGeneration(
                ResourceNameMessageConfigProto.newBuilder()
                    .setMessageName("Book")
                    .putFieldEntityMap("name", "book"))
            .addResourceNameGeneration(
                ResourceNameMessageConfigProto.newBuilder()
                    .setMessageName("BookFromAnywhere")
                    .putFieldEntityMap("name", "book_oneof"))
            .addResourceNameGeneration(
                ResourceNameMessageConfigProto.newBuilder()
                    .setMessageName("Shelf")
                    .putFieldEntityMap("name", "shelf"))
            .addCollectionOneofs(
                CollectionOneofProto.newBuilder()
                    .setOneofName("book_oneof")
                    .addAllCollectionNames(Arrays.asList("book", "archived_book", "deleted_book"))
                    .build())
            .addCollections(
                CollectionConfigProto.newBuilder()
                    .setNamePattern(GAPIC_SHELF_PATH)
                    .setEntityName("shelf"))
            .addCollections(
                CollectionConfigProto.newBuilder()
                    .setNamePattern(ARCHIVED_BOOK_PATH)
                    .setEntityName("archived_book"))
            .addFixedResourceNameValues(
                FixedResourceNameValueProto.newBuilder()
                    .setEntityName("deleted_book")
                    .setFixedValue("_deleted-book_"))
            .addInterfaces(
                InterfaceConfigProto.newBuilder()
                    .addCollections(
                        CollectionConfigProto.newBuilder()
                            .setNamePattern(GAPIC_SHELF_PATH)
                            .setEntityName("shelf"))
                    .addCollections(
                        CollectionConfigProto.newBuilder()
                            .setNamePattern(GAPIC_BOOK_PATH)
                            .setEntityName("book")))
            .build();

    Mockito.when(shelfName.getParent()).thenReturn(shelfMessage);
    Mockito.when(shelfName.getType()).thenReturn(TypeRef.fromPrimitiveName("string"));
    Mockito.when(shelfName.getSimpleName()).thenReturn("name");

    Mockito.when(shelfMessage.getFullName()).thenReturn("library.Shelf");
    Mockito.when(shelfMessage.getFields()).thenReturn(ImmutableList.of(shelfName, shelfTheme));
    Mockito.when(shelfMessage.getSimpleName()).thenReturn("Shelf");
    Mockito.when(shelfMessage.getFields()).thenReturn(ImmutableList.of(shelfName, shelfTheme));

    Mockito.when(bookName.getParent()).thenReturn(bookMessage);
    Mockito.when(bookName.getSimpleName()).thenReturn("name");
    Mockito.when(bookName.getType()).thenReturn(TypeRef.fromPrimitiveName("string"));

    Mockito.when(bookMessage.getFullName()).thenReturn("library.Book");
    Mockito.when(bookMessage.getSimpleName()).thenReturn("Book");
    Mockito.when(bookMessage.getFields()).thenReturn(ImmutableList.of(bookAuthor, bookName));

    Mockito.doReturn(null).when(protoParser).getResourceSet(any());
    Mockito.when(protoFile.getSimpleName()).thenReturn("library");
    Mockito.when(protoFile.getMessages()).thenReturn(ImmutableList.of(bookMessage, shelfMessage));

    Mockito.doReturn("library").when(protoParser).getProtoPackage(protoFile);

    Mockito.when(createShelvesMethod.getSimpleName()).thenReturn(CREATE_SHELF_METHOD_NAME);
    Mockito.when(createShelvesMethod.getInputType()).thenReturn(TypeRef.of(createShelvesRequest));
    Mockito.when(createShelvesMethod.getOutputType()).thenReturn(TypeRef.of(createShelvesResponse));

    Mockito.doReturn(shelfMessage).when(createShelvesMethod).getInputMessage();

    Mockito.doReturn(bookMessage).when(insertBook).getInputMessage();
    Mockito.doReturn(protoFile).when(bookMessage).getParent();
    // Mockito.doReturn("Book").when(protoParser).getResourceReference(bookName);
  }

  @Test
  public void testCreateResourceNamesWithProtoFilesOnly() {
    DiagCollector diagCollector = new BoundedDiagCollector();
    ConfigProto emptyConfigProto = ConfigProto.getDefaultInstance();
    String defaultPackage = "";

    Mockito.doReturn(Resource.newBuilder().setPath(PROTO_BOOK_PATH).build())
        .when(protoParser)
        .getResource(bookName);
    Mockito.doReturn(Resource.newBuilder().setPath(PROTO_SHELF_PATH).build())
        .when(protoParser)
        .getResource(shelfName);

    ResourceNameMessageConfigs messageConfigs =
        ResourceNameMessageConfigs.createMessageResourceTypesConfig(
            sourceProtoFiles,
            diagCollector,
            emptyConfigProto,
            defaultPackage,
            allResourceDefs,
            allResourceSetDefs,
            protoParser);
    assertThat(diagCollector.getErrorCount()).isEqualTo(0);

    assertThat(messageConfigs.getResourceTypeConfigMap().size()).isEqualTo(2);
    ResourceNameMessageConfig bookMessageConfig =
        messageConfigs.getResourceTypeConfigMap().get("library.Book");
    assertThat(bookMessageConfig.fieldEntityMap().get("name")).isEqualTo("Book");
    ResourceNameMessageConfig shelfMessageConfig =
        messageConfigs.getResourceTypeConfigMap().get("library.Shelf");
    assertThat(shelfMessageConfig.fieldEntityMap().get("name")).isEqualTo("Shelf");
  }

  @Test
  public void testCreateResourceNamesWithConfigOnly() {
    DiagCollector diagCollector = new BoundedDiagCollector();
    ConfigProto configProto =
        ConfigProto.newBuilder()
            .addResourceNameGeneration(
                ResourceNameMessageConfigProto.newBuilder()
                    .setMessageName("Book")
                    .putFieldEntityMap("name", "book"))
            .addResourceNameGeneration(
                ResourceNameMessageConfigProto.newBuilder()
                    .setMessageName("BookFromAnywhere")
                    .putFieldEntityMap("name", "book_oneof"))
            .addResourceNameGeneration(
                ResourceNameMessageConfigProto.newBuilder()
                    .setMessageName("Shelf")
                    .putFieldEntityMap("name", "shelf"))
            .addCollectionOneofs(
                CollectionOneofProto.newBuilder()
                    .setOneofName("book_oneof")
                    .addAllCollectionNames(Arrays.asList("book", "archived_book"))
                    .build())
            .build();

    String defaultPackage = "library";

    ResourceNameMessageConfigs messageConfigs =
        ResourceNameMessageConfigs.createMessageResourceTypesConfig(
            diagCollector, configProto, defaultPackage);
    assertThat(diagCollector.getErrorCount()).isEqualTo(0);
    assertThat(messageConfigs).isNotNull();
    assertThat(messageConfigs.getResourceTypeConfigMap().size()).isEqualTo(3);

    ResourceNameMessageConfig bookResource =
        messageConfigs.getResourceTypeConfigMap().get("library.Book");
    assertThat(bookResource.getEntityNameForField("name")).isEqualTo("book");

    ResourceNameMessageConfig getShelfRequestObject =
        messageConfigs.getResourceTypeConfigMap().get("library.BookFromAnywhere");
    assertThat(getShelfRequestObject.getEntityNameForField("name")).isEqualTo("book_oneof");

    ResourceNameMessageConfig shelfResource =
        messageConfigs.getResourceTypeConfigMap().get("library.Shelf");
    assertThat(shelfResource.getEntityNameForField("name")).isEqualTo("shelf");
  }

  @Test
  public void testCreateResourceNames() {
    DiagCollector diagCollector = new BoundedDiagCollector();

    Map<ResourceSet, ProtoFile> resourceSetDefs = new HashMap<>();
    ResourceNameMessageConfigs messageConfigs =
        ResourceNameMessageConfigs.createMessageResourceTypesConfig(
            sourceProtoFiles,
            diagCollector,
            configProto,
            DEFAULT_PACKAGE,
            allResourceDefs,
            resourceSetDefs,
            protoParser);
    assertThat(diagCollector.getErrorCount()).isEqualTo(0);
  }

  @Test
  public void testCreateResourceNameConfigs() {
    DiagCollector diagCollector = new BoundedDiagCollector();

    Map<String, ResourceNameConfig> resourceNameConfigs =
        GapicProductConfig.createResourceNameConfigs(
            diagCollector,
            configProto,
            sourceProtoFiles,
            TargetLanguage.CSHARP,
            allResourceDefs,
            allResourceSetDefs,
            protoParser);

    assertThat(diagCollector.getErrorCount()).isEqualTo(0);
    assertThat(resourceNameConfigs.size()).isEqualTo(7);

    assertThat(((SingleResourceNameConfig) resourceNameConfigs.get("Book")).getNamePattern())
        .isEqualTo(PROTO_BOOK_PATH);

    // Both Protofile and GAPIC config have definitions for archived_book.
    assertThat(diagCollector.getDiags().get(0).getMessage())
        .contains("archived_book from protofile clashes with a Resource");
    assertThat(
            ((SingleResourceNameConfig) resourceNameConfigs.get("archived_book")).getNamePattern())
        .isEqualTo(ARCHIVED_BOOK_PATH);
    assertThat(((SingleResourceNameConfig) resourceNameConfigs.get("book")).getNamePattern())
        .isEqualTo(GAPIC_BOOK_PATH);
    assertThat(((SingleResourceNameConfig) resourceNameConfigs.get("shelf")).getNamePattern())
        .isEqualTo(GAPIC_SHELF_PATH);
    assertThat(
            ((ResourceNameOneofConfig) resourceNameConfigs.get("book_oneof"))
                .getResourceNameConfigs())
        .hasSize(3);
    assertThat(((FixedResourceNameConfig) resourceNameConfigs.get("deleted_book")).getFixedValue())
        .isEqualTo("_deleted-book_");
    assertThat(((SingleResourceNameConfig) resourceNameConfigs.get("Shelf")).getNamePattern())
        .isEqualTo(PROTO_SHELF_PATH);

    // Use GAPIC_BOOK_PATH from gapic config.
    assertThat(((SingleResourceNameConfig) resourceNameConfigs.get("book")).getNamePattern())
        .isEqualTo(GAPIC_BOOK_PATH);

    // "Book" is the name from the unnamed Resource in the Book message type.
    SingleResourceNameConfig bookResourcenameConfigFromProtoFile =
        (SingleResourceNameConfig) resourceNameConfigs.get("Book");
    assertThat(bookResourcenameConfigFromProtoFile.getNamePattern()).isEqualTo(PROTO_BOOK_PATH);
    assertThat(diagCollector.getErrorCount()).isEqualTo(0);
  }

  @Test
  public void testCreateFlattenings() {
    ProtoMethodModel methodModel = new ProtoMethodModel(createShelvesMethod);
    Field bookField = Mockito.mock(Field.class);
    Mockito.when(bookField.getType()).thenReturn(TypeRef.of(bookType));
    Mockito.when(bookField.getParent()).thenReturn(createShelvesRequest);
    Mockito.when(bookField.getSimpleName()).thenReturn("book");
    Field nameField = Mockito.mock(Field.class);
    Mockito.when(nameField.getParent()).thenReturn(createShelvesRequest);
    Mockito.when(createShelvesRequest.getFullName()).thenReturn("library.CreateShelvesRequest");
    Mockito.when(nameField.getType()).thenReturn(TypeRef.fromPrimitiveName("string"));
    Mockito.when(nameField.getSimpleName()).thenReturn("name");
    Mockito.when(createShelvesRequest.lookupField("book")).thenReturn(bookField);
    Mockito.when(createShelvesRequest.lookupField("name")).thenReturn(nameField);
    Mockito.when(createShelvesRequest.getFields())
        .thenReturn(ImmutableList.of(bookField, nameField));

    Mockito.doReturn("library.Book").when(protoParser).getResourceReference(bookField);
    Mockito.doReturn("library.Shelf").when(protoParser).getResourceReference(nameField);

    // ProtoFile contributes flattenings {["name", "book"], ["name"]}.
    Mockito.doReturn(
            Arrays.asList(
                MethodSignature.newBuilder().addFields("name").addFields("book").build(),
                MethodSignature.newBuilder().addFields("name").build()))
        .when(protoParser)
        .getMethodSignatures(createShelvesMethod);

    String flatteningConfigName = "flatteningGroupName";
    // Gapic config contributes flattenings {["book"]}.
    MethodConfigProto methodConfigProto =
        MethodConfigProto.newBuilder()
            .setName(CREATE_SHELF_METHOD_NAME)
            .setFlattening(
                FlatteningConfigProto.newBuilder()
                    .addGroups(
                        FlatteningGroupProto.newBuilder()
                            .addAllParameters(Arrays.asList("book"))
                            .setFlatteningGroupName(flatteningConfigName)))
            .setResourceNameTreatment(ResourceNameTreatment.STATIC_TYPES)
            .build();
    InterfaceConfigProto interfaceConfigProto =
        configProto.toBuilder().getInterfaces(0).toBuilder().addMethods(methodConfigProto).build();

    configProto =
        configProto
            .toBuilder()
            .setInterfaces(0, interfaceConfigProto)
            .addResourceNameGeneration(
                ResourceNameMessageConfigProto.newBuilder()
                    .setMessageName("CreateShelvesRequest")
                    .putFieldEntityMap("name", "shelf")
                    .putFieldEntityMap("book", "book"))
            .build();

    DiagCollector diagCollector = new BoundedDiagCollector();
    ResourceNameMessageConfigs messageConfigs =
        ResourceNameMessageConfigs.createMessageResourceTypesConfig(
            sourceProtoFiles,
            diagCollector,
            configProto,
            DEFAULT_PACKAGE,
            allResourceDefs,
            allResourceSetDefs,
            protoParser);
    ImmutableMap<String, ResourceNameConfig> resourceNameConfigs =
        GapicProductConfig.createResourceNameConfigs(
            diagCollector,
            configProto,
            sourceProtoFiles,
            TargetLanguage.CSHARP,
            allResourceDefs,
            allResourceSetDefs,
            protoParser);

    List<FlatteningConfig> flatteningConfigs =
        new ArrayList<>(
            FlatteningConfig.createFlatteningConfigs(
                diagCollector,
                messageConfigs,
                resourceNameConfigs,
                methodConfigProto,
                methodModel,
                protoParser));
    assertThat(flatteningConfigs).isNotNull();
    assertThat(flatteningConfigs.size()).isEqualTo(3);

    // Check the flattening from the Gapic config.
    Optional<FlatteningConfig> flatteningConfigFromGapicConfig =
        flatteningConfigs
            .stream()
            .filter(f -> flatteningConfigName.equals(f.getFlatteningName()))
            .findAny();
    assertThat(flatteningConfigFromGapicConfig.isPresent()).isTrue();
    Map<String, FieldConfig> paramsFromGapicConfigFlattening =
        flatteningConfigFromGapicConfig.get().getFlattenedFieldConfigs();
    assertThat(paramsFromGapicConfigFlattening.size()).isEqualTo(1);
    assertThat(paramsFromGapicConfigFlattening.get("book").getField().getSimpleName())
        .isEqualTo("book");
    assertThat(
            ((ProtoField) paramsFromGapicConfigFlattening.get("book").getField())
                .getType()
                .getProtoType()
                .getMessageType())
        .isEqualTo(bookType);

    flatteningConfigs.remove(flatteningConfigFromGapicConfig.get());

    // Check the flattenings from the protofile annotations.
    flatteningConfigs.sort(Comparator.comparingInt(c -> Iterables.size(c.getFlattenedFields())));

    FlatteningConfig shelfFlattening = flatteningConfigs.get(0);
    assertThat(Iterables.size(shelfFlattening.getFlattenedFields())).isEqualTo(1);

    FieldConfig nameConfig = shelfFlattening.getFlattenedFieldConfigs().get("name");
    assertThat(nameConfig.getResourceNameTreatment()).isEqualTo(ResourceNameTreatment.STATIC_TYPES);
    assertThat(((SingleResourceNameConfig) nameConfig.getResourceNameConfig()).getNamePattern())
        .isEqualTo(GAPIC_SHELF_PATH);

    FlatteningConfig shelfAndBookFlattening = flatteningConfigs.get(1);
    assertThat(Iterables.size(shelfAndBookFlattening.getFlattenedFields())).isEqualTo(2);

    FieldConfig nameConfig2 = shelfAndBookFlattening.getFlattenedFieldConfigs().get("name");
    assertThat(nameConfig2.getResourceNameTreatment())
        .isEqualTo(ResourceNameTreatment.STATIC_TYPES);
    // Use GAPIC_SHELF_PATH over PROTO_SHELF_PATH.
    assertThat(((SingleResourceNameConfig) nameConfig2.getResourceNameConfig()).getNamePattern())
        .isEqualTo(GAPIC_SHELF_PATH);

    FieldConfig bookConfig = shelfAndBookFlattening.getFlattenedFieldConfigs().get("book");
    assertThat(bookConfig.getResourceNameTreatment()).isEqualTo(ResourceNameTreatment.STATIC_TYPES);
    // Use the resource name path from GAPIC config.
    assertThat(((SingleResourceNameConfig) bookConfig.getResourceNameConfig()).getNamePattern())
        .isEqualTo(GAPIC_BOOK_PATH);
    assertThat(((ProtoTypeRef) bookConfig.getField().getType()).getProtoType().getMessageType())
        .isEqualTo(bookType);
  }

  @Test
  public void testDefaultResourceNameTreatment() {
    // Test GapicMethodConfig.defaultResourceNameTreatment().

    Mockito.doReturn("Book").when(protoParser).getResourceReference(bookName);

    MethodConfigProto noConfig = MethodConfigProto.getDefaultInstance();

    ResourceNameTreatment noTreatment =
        GapicMethodConfig.defaultResourceNameTreatment(
            noConfig, createShelvesMethod, protoParser, DEFAULT_PACKAGE);
    assertThat(noTreatment).isEqualTo(ResourceNameTreatment.NONE);

    MethodConfigProto staticTypesMethodConfig =
        MethodConfigProto.newBuilder()
            .setResourceNameTreatment(ResourceNameTreatment.STATIC_TYPES)
            .build();

    ResourceNameTreatment resourceNameTreatment =
        GapicMethodConfig.defaultResourceNameTreatment(
            staticTypesMethodConfig, createShelvesMethod, protoParser, DEFAULT_PACKAGE);
    assertThat(resourceNameTreatment).isEqualTo(ResourceNameTreatment.STATIC_TYPES);

    ResourceNameTreatment noConfigWithAnnotatedResourceReferenceTreatment =
        GapicMethodConfig.defaultResourceNameTreatment(
            noConfig, insertBook, protoParser, DEFAULT_PACKAGE);
    assertThat(noConfigWithAnnotatedResourceReferenceTreatment)
        .isEqualTo(ResourceNameTreatment.STATIC_TYPES);

    // Reset the mock's behavior for the getResourceReference method.
    Mockito.doReturn(null).when(protoParser).getResourceReference(bookName);
  }
}
