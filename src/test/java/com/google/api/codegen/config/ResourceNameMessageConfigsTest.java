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

import com.google.api.codegen.CollectionConfigProto;
import com.google.api.codegen.CollectionOneofProto;
import com.google.api.codegen.ConfigProto;
import com.google.api.codegen.FixedResourceNameValueProto;
import com.google.api.codegen.InterfaceConfigProto;
import com.google.api.codegen.ResourceNameMessageConfigProto;
import com.google.api.codegen.common.TargetLanguage;
import com.google.api.codegen.util.ProtoParser;
import com.google.api.tools.framework.model.BoundedDiagCollector;
import com.google.api.tools.framework.model.Diag.Kind;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.MessageType;
import com.google.api.tools.framework.model.ProtoFile;
import com.google.api.tools.framework.model.TypeRef;
import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

public class ResourceNameMessageConfigsTest {
  private static final ProtoParser protoParser = Mockito.mock(ProtoParser.class);
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
  private static final String ASTERISK_SHELF_PATH = "shelves/{shelf_id}";
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

    Mockito.when(protoParser.getResourcePath(bookName)).thenReturn(ASTERISK_BOOK_PATH);
    Mockito.when(protoParser.getResourcePath(shelfName)).thenReturn(ASTERISK_SHELF_PATH);

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
            new LinkedList<>(), diagCollector, emptyConfigProto, defaultPackage, protoParser);
    assertThat(diagCollector.getErrorCount()).isEqualTo(0);
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
    Mockito.when(protoParser.getResourceEntityName(Mockito.any())).thenCallRealMethod();
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
}
