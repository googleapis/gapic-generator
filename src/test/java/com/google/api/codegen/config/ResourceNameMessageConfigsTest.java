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

import com.google.api.MethodSignature;
import com.google.api.Resource;
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
import com.google.api.tools.framework.model.Diag.Kind;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

public class ResourceNameMessageConfigsTest {
  private static final ProtoParser protoParser = Mockito.spy(ProtoParser.class);
  private static ConfigProto configProto;
  private static final Field shelfName = Mockito.mock(Field.class);
  private static final Field shelfTheme = Mockito.mock(Field.class);
  private static final MessageType shelfMessage = Mockito.mock(MessageType.class);
  private static final Field bookName = Mockito.mock(Field.class);
  private static final Field bookAuthor = Mockito.mock(Field.class);
  private static final MessageType bookMessage = Mockito.mock(MessageType.class);
  private static final ProtoFile protoFile = Mockito.mock(ProtoFile.class);
  private static final ImmutableList<ProtoFile> sourceProtoFiles = ImmutableList.of(protoFile);

  private static final String DEFAULT_PACKAGE = "library";
  private static final String SIMPLE_SHELF_PATH = "shelves/{shelf_id}";
  private static final String SIMPLE_BOOK_PATH = "shelves/{shelf_id}/books/{book_id}";
  private static final String SIMPLE_ARCHIVED_BOOK_PATH =
      "archives/{archive_path}/books/{book_id=**}";
  private static final String ASTERISK_SHELF_PATH = "shelves/*";
  private static final String ASTERISK_BOOK_PATH = "bookShelves/*";

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
                    .setNamePattern(SIMPLE_SHELF_PATH)
                    .setEntityName("shelf"))
            .addCollections(
                CollectionConfigProto.newBuilder()
                    .setNamePattern(SIMPLE_ARCHIVED_BOOK_PATH)
                    .setEntityName("archived_book"))
            .addFixedResourceNameValues(
                FixedResourceNameValueProto.newBuilder()
                    .setEntityName("deleted_book")
                    .setFixedValue("_deleted-book_"))
            .addInterfaces(
                InterfaceConfigProto.newBuilder()
                    .addCollections(
                        CollectionConfigProto.newBuilder()
                            .setNamePattern(SIMPLE_SHELF_PATH)
                            .setEntityName("shelf"))
                    .addCollections(
                        CollectionConfigProto.newBuilder()
                            .setNamePattern(SIMPLE_BOOK_PATH)
                            .setEntityName("book")))
            .build();

    Mockito.when(shelfName.getParent()).thenReturn(shelfMessage);
    Mockito.when(shelfName.getType()).thenReturn(TypeRef.fromPrimitiveName("string"));
    Mockito.when(shelfName.getSimpleName()).thenReturn("name");

    Mockito.when(shelfMessage.getFullName()).thenReturn("library.Shelf");
    Mockito.when(shelfMessage.getFields()).thenReturn(ImmutableList.of(shelfName, shelfTheme));
    Mockito.when(shelfMessage.getSimpleName()).thenReturn("Shelf");

    Mockito.when(bookName.getParent()).thenReturn(bookMessage);
    Mockito.when(bookName.getSimpleName()).thenReturn("name");
    Mockito.when(bookName.getType()).thenReturn(TypeRef.fromPrimitiveName("string"));

    Mockito.when(bookMessage.getFullName()).thenReturn("library.Book");
    Mockito.when(bookMessage.getSimpleName()).thenReturn("Book");
    Mockito.when(bookMessage.getFields()).thenReturn(ImmutableList.of(bookAuthor, bookName));

    Mockito.doReturn(Resource.newBuilder().setPath(ASTERISK_BOOK_PATH).build())
        .when(protoParser)
        .getResource(bookName);
    Mockito.doReturn(Resource.newBuilder().setPath(ASTERISK_SHELF_PATH).build())
        .when(protoParser)
        .getResource(shelfName);
    Mockito.doReturn(null).when(protoParser).getResourceSet(Mockito.any());

    Mockito.when(protoFile.getSimpleName()).thenReturn("library");
    Mockito.when(protoFile.getMessages()).thenReturn(ImmutableList.of(bookMessage, shelfMessage));
  }

  @Test
  public void testCreateResourceNamesWithProtoFilesOnly() {
    DiagCollector diagCollector = new BoundedDiagCollector();
    ConfigProto emptyConfigProto = ConfigProto.getDefaultInstance();
    String defaultPackage = "";

    ResourceNameMessageConfigs messageConfigs =
        ResourceNameMessageConfigs.createMessageResourceTypesConfig(
            sourceProtoFiles, diagCollector, emptyConfigProto, defaultPackage, protoParser);
    assertThat(diagCollector.getErrorCount()).isEqualTo(0);

    assertThat(messageConfigs.getResourceTypeConfigMap().size()).isEqualTo(2);
    ResourceNameMessageConfig bookMessageConfig =
        messageConfigs.getResourceTypeConfigMap().get("library.Book");
    assertThat(bookMessageConfig.fieldEntityMap().get("name")).isEqualTo("book");
    ResourceNameMessageConfig shelfMessageConfig =
        messageConfigs.getResourceTypeConfigMap().get("library.Shelf");
    assertThat(shelfMessageConfig.fieldEntityMap().get("name")).isEqualTo("shelf");
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

    ResourceNameMessageConfigs messageConfigs =
        ResourceNameMessageConfigs.createMessageResourceTypesConfig(
            sourceProtoFiles, diagCollector, configProto, DEFAULT_PACKAGE, protoParser);
    assertThat(diagCollector.getErrorCount()).isEqualTo(0);
  }

  @Test
  public void testCreateResourceNameConfigs() {
    DiagCollector diagCollector = new BoundedDiagCollector();
    Map<String, ResourceNameConfig> resourceNameConfigs =
        GapicProductConfig.createResourceNameConfigs(
            diagCollector, configProto, sourceProtoFiles, TargetLanguage.CSHARP, protoParser);
    assertThat(resourceNameConfigs.size()).isEqualTo(5);

    assertThat((resourceNameConfigs.get("shelf")) instanceof SingleResourceNameConfig).isTrue();
    SingleResourceNameConfig shelfResourceNameConfig =
        (SingleResourceNameConfig) resourceNameConfigs.get("shelf");

    // Use ASTERISK_SHELF_PATH from protofile instead of SIMPLE_SHELF_PATH from gapic config.
    assertThat(shelfResourceNameConfig.getNamePattern()).isEqualTo(ASTERISK_SHELF_PATH);
    assertThat(
            diagCollector
                .getDiags()
                .stream()
                .anyMatch(
                    d ->
                        d.getMessage().contains("from protofile clashes with GAPIC config")
                            && d.getKind().equals(Kind.WARNING)))
        .isTrue();

    assertThat(diagCollector.getErrorCount()).isEqualTo(0);
  }

  @Test
  public void testCreateFlattenings() {
    String createShelfMethodName = "CreateShelf";
    Method createShelvesMethod = Mockito.mock(Method.class);
    Mockito.when(createShelvesMethod.getSimpleName()).thenReturn(createShelfMethodName);
    MessageType createShelvesRequest = Mockito.mock(MessageType.class);
    MessageType createShelvesResponse = Mockito.mock(MessageType.class);
    MessageType bookType = Mockito.mock(MessageType.class);

    Mockito.when(createShelvesMethod.getInputType()).thenReturn(TypeRef.of(createShelvesRequest));
    Mockito.when(createShelvesMethod.getOutputType()).thenReturn(TypeRef.of(createShelvesResponse));
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

    Mockito.doReturn("library.Book").when(protoParser).getResourceType(bookField);
    Mockito.doReturn("library.Shelf").when(protoParser).getResourceType(nameField);

    // ProtoFile contributes flattenings {["name", "book"], ["name"]}.
    Mockito.doReturn(
            Arrays.asList(
                MethodSignature.newBuilder().addFields("name").addFields("book").build(),
                MethodSignature.newBuilder().addFields("name").build()))
        .when(protoParser)
        .getMethodSignatures(methodModel.getProtoMethod());

    String flatteningConfigName = "flatteningGroupName";
    // Gapic config contributes flattenings {["book"]}.
    MethodConfigProto methodConfigProto =
        MethodConfigProto.newBuilder()
            .setName(createShelfMethodName)
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
                    .putFieldEntityMap("name", "shelf"))
            .build();

    DiagCollector diagCollector = new BoundedDiagCollector();
    ResourceNameMessageConfigs messageConfigs =
        ResourceNameMessageConfigs.createMessageResourceTypesConfig(
            sourceProtoFiles, diagCollector, configProto, DEFAULT_PACKAGE, protoParser);
    ImmutableMap<String, ResourceNameConfig> resourceNameConfigs =
        GapicProductConfig.createResourceNameConfigs(
            diagCollector, configProto, sourceProtoFiles, TargetLanguage.CSHARP, protoParser);

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
        .isEqualTo(ASTERISK_SHELF_PATH);

    FlatteningConfig shelfAndBookFlattening = flatteningConfigs.get(1);
    assertThat(Iterables.size(shelfAndBookFlattening.getFlattenedFields())).isEqualTo(2);

    FieldConfig nameConfig2 = shelfAndBookFlattening.getFlattenedFieldConfigs().get("name");
    assertThat(nameConfig2.getResourceNameTreatment())
        .isEqualTo(ResourceNameTreatment.STATIC_TYPES);
    assertThat(((SingleResourceNameConfig) nameConfig2.getResourceNameConfig()).getNamePattern())
        .isEqualTo(ASTERISK_SHELF_PATH);

    FieldConfig bookConfig = shelfAndBookFlattening.getFlattenedFieldConfigs().get("book");
    assertThat(((ProtoTypeRef) bookConfig.getField().getType()).getProtoType().getMessageType())
        .isEqualTo(bookType);
  }
}
