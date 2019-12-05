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

import com.google.api.ResourceDescriptor;
import com.google.api.ResourceReference;
import com.google.api.codegen.CollectionConfigProto;
import com.google.api.codegen.CollectionOneofProto;
import com.google.api.codegen.ConfigProto;
import com.google.api.codegen.FlatteningConfigProto;
import com.google.api.codegen.FlatteningGroupProto;
import com.google.api.codegen.InterfaceConfigProto;
import com.google.api.codegen.MethodConfigProto;
import com.google.api.codegen.ResourceNameMessageConfigProto;
import com.google.api.codegen.ResourceNameTreatment;
import com.google.api.codegen.common.TargetLanguage;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.ProtoParser;
import com.google.api.tools.framework.model.BoundedDiagCollector;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.Diag.Kind;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.MessageType;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.ProtoFile;
import com.google.api.tools.framework.model.TypeRef;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.Spy;

public class ResourceNameMessageConfigsTest {
  @Spy private static final ProtoParser protoParser = Mockito.mock(ProtoParser.class);
  private static ConfigProto configProto;
  private static ConfigProto configProtoV2;
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
  private static final Interface anInterface = Mockito.mock(Interface.class);
  private static final Method insertBook = Mockito.mock(Method.class);

  private static final String DEFAULT_PACKAGE = "library";
  private static final FileDescriptorProto fileDescriptor =
      FileDescriptorProto.newBuilder()
          .setPackage(DEFAULT_PACKAGE)
          .build(); // final class, can't be mocked
  private static final String GAPIC_SHELF_PATH = "shelves/{shelf_id}";
  private static final String DELETED_BOOK_PATH = "_deleted-book_";
  private static final String GAPIC_BOOK_PATH = "shelves/{shelf_id}/books/{book_id}";
  private static final String PROTO_ARCHIVED_BOOK_PATH =
      "archives/{archive_path}/books/{book_id=**}";
  private static final String GAPIC_ARCHIVED_BOOK_PATH = "archives/{archive}/books/{book}";
  private static final String PROTO_SHELF_PATH = "shelves/{shelf}";
  private static final String PROTO_BOOK_PATH = "bookShelves/{book}";
  private static final String CREATE_SHELF_METHOD_NAME = "CreateShelf";

  private static final ResourceDescriptorConfig SHELF_RESOURCE_DESCRIPTOR_CONFIG =
      ResourceDescriptorConfig.from(
          ResourceDescriptor.newBuilder()
              .setType("library.googleapis.com/Shelf")
              .addPattern(PROTO_SHELF_PATH)
              .build(),
          protoFile,
          true);

  private static final ResourceDescriptorConfig BOOK_RESOURCE_DESCRIPTOR_CONFIG =
      ResourceDescriptorConfig.from(
          ResourceDescriptor.newBuilder()
              .setType("library.googleapis.com/Book")
              .addPattern(PROTO_BOOK_PATH)
              .build(),
          protoFile,
          true);

  private static final ResourceDescriptorConfig ARCHIVED_BOOK_RESOURCE_DESCRIPTOR_CONFIG =
      ResourceDescriptorConfig.from(
          ResourceDescriptor.newBuilder()
              .setType("library.googleapis.com/ArchivedBook")
              .addPattern(PROTO_ARCHIVED_BOOK_PATH)
              .build(),
          protoFile,
          true);

  private static final Map<String, ResourceDescriptorConfig> resourceDescriptorConfigMap =
      ImmutableMap.of(
          "library.googleapis.com/Shelf",
          SHELF_RESOURCE_DESCRIPTOR_CONFIG,
          "library.googleapis.com/Book",
          BOOK_RESOURCE_DESCRIPTOR_CONFIG,
          "library.googleapis.com/ArchivedBook",
          ARCHIVED_BOOK_RESOURCE_DESCRIPTOR_CONFIG);

  private static final Map<String, Set<ResourceDescriptorConfig>> patternResourceDescriptorMap =
      ImmutableMap.of(
          PROTO_SHELF_PATH,
          ImmutableSet.of(SHELF_RESOURCE_DESCRIPTOR_CONFIG),
          PROTO_BOOK_PATH,
          ImmutableSet.of(BOOK_RESOURCE_DESCRIPTOR_CONFIG),
          PROTO_ARCHIVED_BOOK_PATH,
          ImmutableSet.of(ARCHIVED_BOOK_RESOURCE_DESCRIPTOR_CONFIG));

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
                    .setNamePattern(GAPIC_ARCHIVED_BOOK_PATH)
                    .setEntityName("archived_book"))
            .addCollections(
                CollectionConfigProto.newBuilder()
                    .setEntityName("deleted_book")
                    .setNamePattern("_deleted-book_"))
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
    configProtoV2 =
        ConfigProto.newBuilder()
            .addCollections(CollectionConfigProto.newBuilder().setEntityName("shelf"))
            .addCollections(CollectionConfigProto.newBuilder().setEntityName("archived_book"))
            .addInterfaces(
                InterfaceConfigProto.newBuilder()
                    .addCollections(CollectionConfigProto.newBuilder().setEntityName("shelf"))
                    .addCollections(CollectionConfigProto.newBuilder().setEntityName("book")))
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
    Mockito.doReturn(ResourceReference.newBuilder().setType("library.googleapis.com/Book").build())
        .when(protoParser)
        .getResourceReference(bookName);
    Mockito.doReturn(true).when(protoParser).hasResourceReference(bookName);

    Mockito.when(bookMessage.getFullName()).thenReturn("library.Book");
    Mockito.when(bookMessage.getSimpleName()).thenReturn("Book");
    Mockito.when(bookMessage.getFields()).thenReturn(ImmutableList.of(bookAuthor, bookName));

    Mockito.when(protoFile.getSimpleName()).thenReturn("library");
    Mockito.when(protoFile.getMessages()).thenReturn(ImmutableList.of(bookMessage, shelfMessage));

    Mockito.doReturn("library").when(protoParser).getProtoPackage(protoFile);
    Mockito.doReturn(fileDescriptor).when(protoFile).getProto();

    Mockito.when(createShelvesMethod.getSimpleName()).thenReturn(CREATE_SHELF_METHOD_NAME);
    Mockito.when(createShelvesMethod.getInputType()).thenReturn(TypeRef.of(createShelvesRequest));
    Mockito.when(createShelvesMethod.getOutputType()).thenReturn(TypeRef.of(createShelvesResponse));

    Mockito.doReturn(shelfMessage).when(createShelvesMethod).getInputMessage();

    Mockito.doReturn(bookMessage).when(insertBook).getInputMessage();
    Mockito.doReturn(protoFile).when(bookMessage).getParent();
    Mockito.doReturn(ImmutableList.of(anInterface)).when(protoFile).getInterfaces();
    Mockito.doReturn("library.LibraryService").when(anInterface).getFullName();
    // Mockito.doReturn("Book").when(protoParser).getResourceReference(bookName);
  }

  @Test
  public void testCreateResourceNamesWithProtoFilesOnly() {

    Mockito.doReturn(ResourceReference.newBuilder().setType("library.googleapis.com/Shelf").build())
        .when(protoParser)
        .getResourceReference(shelfName);
    Mockito.doReturn(true).when(protoParser).hasResourceReference(shelfName);

    // For testing purposes, we don't care about the actual config; we just need an entry in the map
    ResourceNameConfig dummyConfig =
        SingleResourceNameConfig.newBuilder()
            .setNamePattern("")
            .setEntityId("")
            .setEntityName(Name.from("entity"))
            .build();
    Map<String, ResourceNameConfig> resourceNameConfigs =
        ImmutableMap.of("Book", dummyConfig, "Shelf", dummyConfig);
    ResourceNameMessageConfigs messageConfigs =
        ResourceNameMessageConfigs.createFromAnnotations(
            null,
            sourceProtoFiles,
            resourceNameConfigs,
            protoParser,
            resourceDescriptorConfigMap,
            Collections.emptyMap());

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
        ResourceNameMessageConfigs.createFromGapicConfigOnly(
            ImmutableList.of(), configProto, defaultPackage);
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
  public void testCreateResourceNameConfigs() {
    DiagCollector diagCollector = new BoundedDiagCollector();

    Map<String, ResourceNameConfig> resourceNameConfigs =
        GapicProductConfig.createResourceNameConfigsFromAnnotationsAndGapicConfig(
            null,
            diagCollector,
            configProtoV2,
            protoFile,
            TargetLanguage.CSHARP,
            resourceDescriptorConfigMap,
            resourceDescriptorConfigMap.keySet(),
            ImmutableSet.of(),
            Collections.emptyMap(),
            patternResourceDescriptorMap,
            Collections.emptyMap(),
            "library");

    assertThat(diagCollector.getErrorCount()).isEqualTo(0);
    assertThat(resourceNameConfigs.size()).isEqualTo(3);

    assertThat(((SingleResourceNameConfig) resourceNameConfigs.get("Book")).getNamePattern())
        .isEqualTo(PROTO_BOOK_PATH);
    assertThat(
            ((SingleResourceNameConfig) resourceNameConfigs.get("ArchivedBook")).getNamePattern())
        .isEqualTo(PROTO_ARCHIVED_BOOK_PATH);
    assertThat(((SingleResourceNameConfig) resourceNameConfigs.get("Shelf")).getNamePattern())
        .isEqualTo(PROTO_SHELF_PATH);

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

    Mockito.when(bookType.getFields()).thenReturn(ImmutableList.of());

    Field nameField = Mockito.mock(Field.class);
    Mockito.when(nameField.getParent()).thenReturn(createShelvesRequest);
    Mockito.when(createShelvesRequest.getFullName()).thenReturn("library.CreateShelvesRequest");
    Mockito.when(nameField.getType()).thenReturn(TypeRef.fromPrimitiveName("string"));
    Mockito.when(nameField.getSimpleName()).thenReturn("name");
    Mockito.doReturn(ResourceReference.newBuilder().setType("library.googleapis.com/Shelf").build())
        .when(protoParser)
        .getResourceReference(nameField);
    Mockito.doReturn(true).when(protoParser).hasResourceReference(nameField);

    Mockito.when(createShelvesRequest.lookupField("book")).thenReturn(bookField);
    Mockito.when(createShelvesRequest.lookupField("name")).thenReturn(nameField);
    Mockito.when(createShelvesRequest.getFields())
        .thenReturn(ImmutableList.of(bookField, nameField));
    Mockito.doReturn(ResourceReference.newBuilder().setType("library.googleapis.com/Shelf").build())
        .when(protoParser)
        .getResourceReference(shelfName);
    Mockito.doReturn(true).when(protoParser).hasResourceReference(shelfName);

    Mockito.doReturn(ResourceReference.newBuilder().setType("library.googleapis.com/Book").build())
        .when(protoParser)
        .getResourceReference(bookField);
    Mockito.doReturn(true).when(protoParser).hasResourceReference(bookField);
    Mockito.doReturn(ResourceReference.newBuilder().setType("library.googleapis.com/Shelf").build())
        .when(protoParser)
        .getResourceReference(nameField);
    Mockito.doReturn(true).when(protoParser).hasResourceReference(nameField);

    // ProtoFile contributes flattenings {["name", "book"], ["name"]}.
    Mockito.doReturn(Arrays.asList(Arrays.asList("name", "book"), Arrays.asList("name")))
        .when(protoParser)
        .getMethodSignatures(createShelvesMethod);

    Mockito.when(protoFile.getMessages())
        .thenReturn(ImmutableList.of(bookMessage, shelfMessage, createShelvesRequest));

    // Gapic config contributes flattenings {["book"]}.
    MethodConfigProto methodConfigProto =
        MethodConfigProto.newBuilder()
            .setName(CREATE_SHELF_METHOD_NAME)
            .setFlattening(
                FlatteningConfigProto.newBuilder()
                    .addGroups(
                        FlatteningGroupProto.newBuilder().addAllParameters(Arrays.asList("book"))))
            .setResourceNameTreatment(ResourceNameTreatment.STATIC_TYPES)
            .build();

    DiagCollector diagCollector = new BoundedDiagCollector();
    assertThat(diagCollector.getErrorCount()).isEqualTo(0);

    ImmutableMap<String, ResourceNameConfig> resourceNameConfigs =
        GapicProductConfig.createResourceNameConfigsFromAnnotationsAndGapicConfig(
            null,
            diagCollector,
            ConfigProto.getDefaultInstance(),
            protoFile,
            TargetLanguage.CSHARP,
            resourceDescriptorConfigMap,
            resourceDescriptorConfigMap.keySet(),
            Collections.emptySet(),
            Collections.emptyMap(),
            patternResourceDescriptorMap,
            Collections.emptyMap(),
            "library");
    assertThat(diagCollector.getErrorCount()).isEqualTo(0);
    ResourceNameMessageConfigs messageConfigs =
        ResourceNameMessageConfigs.createFromAnnotations(
            diagCollector,
            sourceProtoFiles,
            resourceNameConfigs,
            protoParser,
            resourceDescriptorConfigMap,
            Collections.emptyMap());

    List<FlatteningConfig> flatteningConfigs =
        new ArrayList<>(
            FlatteningConfig.createFlatteningConfigs(
                diagCollector,
                messageConfigs,
                resourceNameConfigs,
                methodConfigProto,
                methodModel,
                protoParser));
    assertThat(diagCollector.getErrorCount()).isEqualTo(0);

    List<Diag> warningDiags =
        diagCollector
            .getDiags()
            .stream()
            .filter(d -> d.getKind().equals(Kind.WARNING))
            .collect(Collectors.toList());

    assertThat(flatteningConfigs).isNotNull();
    assertThat(flatteningConfigs.size()).isEqualTo(3);

    // Check the flattening from the Gapic config.
    Optional<FlatteningConfig> flatteningConfigFromGapicConfig =
        flatteningConfigs
            .stream()
            .filter(
                f ->
                    f.getFlattenedFieldConfigs().size() == 1
                        && f.getFlattenedFieldConfigs().containsKey("book"))
            .findAny();
    assertThat(flatteningConfigFromGapicConfig.isPresent()).isTrue();
    Map<String, FieldConfig> paramsFromGapicConfigFlattening =
        flatteningConfigFromGapicConfig.get().getFlattenedFieldConfigs();
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
        .isEqualTo(PROTO_SHELF_PATH);

    FlatteningConfig shelfAndBookFlattening = flatteningConfigs.get(1);
    assertThat(Iterables.size(shelfAndBookFlattening.getFlattenedFields())).isEqualTo(2);

    FieldConfig nameConfig2 = shelfAndBookFlattening.getFlattenedFieldConfigs().get("name");
    assertThat(nameConfig2.getResourceNameTreatment())
        .isEqualTo(ResourceNameTreatment.STATIC_TYPES);
    // Use PROTO_SHELF_PATH over GAPIC_SHELF_PATH.
    assertThat(((SingleResourceNameConfig) nameConfig2.getResourceNameConfig()).getNamePattern())
        .isEqualTo(PROTO_SHELF_PATH);

    FieldConfig bookConfig = shelfAndBookFlattening.getFlattenedFieldConfigs().get("book");
    assertThat(bookConfig.getResourceNameTreatment()).isEqualTo(ResourceNameTreatment.STATIC_TYPES);
    // Use the resource name path from proto file.
    assertThat(((SingleResourceNameConfig) bookConfig.getResourceNameConfig()).getNamePattern())
        .isEqualTo(PROTO_BOOK_PATH);
    assertThat(((ProtoTypeRef) bookConfig.getField().getType()).getProtoType().getMessageType())
        .isEqualTo(bookType);

    // Restore protoFile.getMessages()
    Mockito.when(protoFile.getMessages()).thenReturn(ImmutableList.of(bookMessage, shelfMessage));
  }

  @Test
  public void testDefaultResourceNameTreatment() {
    // Test GapicMethodConfig.defaultResourceNameTreatment().
    ResourceNameTreatment noTreatment =
        GapicMethodConfig.defaultResourceNameTreatmentFromProto(
            createShelvesMethod, protoParser, DEFAULT_PACKAGE);
    assertThat(noTreatment).isEqualTo(ResourceNameTreatment.UNSET_TREATMENT);

    ResourceNameTreatment noConfigWithAnnotatedResourceReferenceTreatment =
        GapicMethodConfig.defaultResourceNameTreatmentFromProto(
            insertBook, protoParser, DEFAULT_PACKAGE);
    assertThat(noConfigWithAnnotatedResourceReferenceTreatment)
        .isEqualTo(ResourceNameTreatment.STATIC_TYPES);
  }
}
