package com.google.api.codegen.config;

import static com.google.common.truth.Truth.assertThat;

import com.google.api.MethodSignature;
import com.google.api.codegen.ConfigProto;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Test;
import org.mockito.Mockito;

public class FlatteningConfigsTest {
  private static final ProtoParser protoParser = ResourceNameMessageConfigsTest.protoParser;
  private static final ConfigProto baseConfigProto = ResourceNameMessageConfigsTest.configProto;
  private static final ImmutableList<ProtoFile> sourceProtoFiles =
      ResourceNameMessageConfigsTest.sourceProtoFiles;
  private static final String DEFAULT_PACKAGE = ResourceNameMessageConfigsTest.DEFAULT_PACKAGE;
  private static final String ASTERISK_SHELF_PATH =
      ResourceNameMessageConfigsTest.ASTERISK_SHELF_PATH;

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

    Mockito.when(protoParser.getResourceType(bookField)).thenReturn("library.Book");
    Mockito.when(protoParser.getResourceType(nameField)).thenReturn("library.Shelf");

    // ProtoFile contributes flattenings {["name", "book"], ["name"]}.
    Mockito.when(protoParser.getMethodSignatures(createShelvesMethod))
        .thenReturn(
            Arrays.asList(
                MethodSignature.newBuilder().addFields("name").addFields("book").build(),
                MethodSignature.newBuilder().addFields("name").build()));

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
        baseConfigProto
            .toBuilder()
            .getInterfaces(0)
            .toBuilder()
            .addMethods(methodConfigProto)
            .build();

    ConfigProto configProto2 =
        baseConfigProto
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
            sourceProtoFiles, diagCollector, configProto2, DEFAULT_PACKAGE, protoParser);
    ImmutableMap<String, ResourceNameConfig> resourceNameConfigs =
        GapicProductConfig.createResourceNameConfigs(
            diagCollector, configProto2, sourceProtoFiles, TargetLanguage.CSHARP, protoParser);

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
