package io.gapi.fx.processors.normalizer;

import com.google.api.Service;
import com.google.common.collect.Lists;
import com.google.protobuf.Api;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.Enum;
import com.google.protobuf.Field.Cardinality;
import com.google.protobuf.Field.Kind;
import com.google.protobuf.SourceContext;
import com.google.protobuf.Type;

import io.gapi.fx.aspects.versioning.model.VersionAttribute;
import io.gapi.fx.model.EnumType;
import io.gapi.fx.model.EnumValue;
import io.gapi.fx.model.Field;
import io.gapi.fx.model.Interface;
import io.gapi.fx.model.MessageType;
import io.gapi.fx.model.Method;
import io.gapi.fx.model.Model;
import io.gapi.fx.model.TypeRef;
import io.gapi.fx.model.Visitor;
import io.gapi.fx.util.VisitsBefore;

import java.util.List;

/**
 * Visits each element in the model and generates the normalized descriptor representation.
 */
class DescriptorNormalizer extends Visitor {

  /**
   * If experiment is set, the generated service descriptor will not contain google.protobuf.Option
   * instances for default values derived from the proto2 descriptor. Consumers of the service
   * descriptor must compute those defaults. b/24131550 tracks to remove this flag and make it the
   * default.
   */
  private static final String EMPTY_DESCRIPTOR_DEFAULTS_EXPERIMENT = "empty-descriptor-defaults";

  private final Model model;
  private final boolean includeDefaults;
  private final List<Api> apis = Lists.newArrayList();
  private final List<Type> types = Lists.newArrayList();
  private final List<Enum> enums = Lists.newArrayList();

  DescriptorNormalizer(Model model) {
    super(model.getScoper(), false);
    this.model = model;
    this.includeDefaults = !model.isExperimentEnabled(EMPTY_DESCRIPTOR_DEFAULTS_EXPERIMENT);

  }

  void run(Service.Builder builder) {
    visit(model);

    builder.clearApis();
    builder.clearTypes();
    builder.clearEnums();
    builder.addAllApis(apis);
    builder.addAllTypes(types);
    builder.addAllEnums(enums);
  }

  @VisitsBefore void normalize(Interface iface) {
    Api.Builder coreApiBuilder = Api.newBuilder().setName(iface.getFullName());
    coreApiBuilder.setSourceContext(
        SourceContext.newBuilder()
            .setFileName(iface.getFile().getLocation().getDisplayString()));
    coreApiBuilder.setSyntax(iface.getSyntax());

    for (Method method : iface.getReachableMethods()) {
      com.google.protobuf.Method.Builder coreMethodBuilder =
          com.google.protobuf.Method.newBuilder().setName(method.getSimpleName())
          .setRequestTypeUrl(generateTypeUrl(method.getInputType()))
          .setResponseTypeUrl(generateTypeUrl(method.getOutputType()));

      coreMethodBuilder.setRequestStreaming(method.getRequestStreaming());
      coreMethodBuilder.setResponseStreaming(method.getResponseStreaming());
      coreMethodBuilder.addAllOptions(DescriptorNormalization.getMethodOptions(
          method.getDescriptor().getOptions(),
          false, /* TODO: MIGRATION method.getDescriptor().isStream() */
          includeDefaults));
      coreApiBuilder.addMethods(coreMethodBuilder);
    }

    coreApiBuilder.addAllOptions(DescriptorNormalization.getOptions(iface.getProto(),
        includeDefaults));
    coreApiBuilder.setVersion(iface.getAttribute(VersionAttribute.KEY).majorVersion());
    apis.add(coreApiBuilder.build());
  }

  @VisitsBefore void normalize(MessageType message) {
    Type.Builder coreTypeBuilder = Type.newBuilder().setName(message.getFullName());
    coreTypeBuilder.setSourceContext(SourceContext.newBuilder()
        .setFileName(message.getFile().getLocation().getDisplayString()));
    coreTypeBuilder.setSyntax(message.getSyntax());

    for (Field field : message.getReachableFields()) {
      com.google.protobuf.Field.Builder coreFieldBuilder =
          com.google.protobuf.Field.newBuilder()
          .setName(field.getSimpleName())
          .setNumber(field.getNumber())
          .setKind(toCoreFieldKind(field.getProto()))
          .setCardinality(toCoreFieldCardinality(field.getProto()))
          .setJsonName(field.getJsonName());

      // According to go/anytype only non-primitive type requires type url
      if (field.getType().isEnum() || field.getType().isMessage()) {
        coreFieldBuilder.setTypeUrl(generateTypeUrl(field.getType()));
      }

      FieldDescriptorProto proto = field.getProto();

      if (proto.hasOneofIndex()) {
        // Index in the containing type's oneof_decl is zero-based.
        // Index in google.protobuf.type.Field.oneof_index is one-based.
        coreFieldBuilder.setOneofIndex(field.getProto().getOneofIndex() + 1);
      }
      if (proto.getOptions().hasPacked()) {
        coreFieldBuilder.setPacked(proto.getOptions().getPacked());
      }
      if (proto.hasDefaultValue()) {
        coreFieldBuilder.setDefaultValue(proto.getDefaultValue());
      }
      coreFieldBuilder.addAllOptions(DescriptorNormalization.getOptions(field.getProto(),
          includeDefaults));
      coreTypeBuilder.addFields(coreFieldBuilder.build());
    }

    coreTypeBuilder.addAllOptions(DescriptorNormalization.getOptions(message.getProto(),
        includeDefaults));
    coreTypeBuilder.addAllOneofs(DescriptorNormalization.getOneofs(message.getProto()));
    types.add(coreTypeBuilder.build());
  }

  @VisitsBefore void normalize(EnumType enumType) {
    Enum.Builder coreEnumBuilder = Enum.newBuilder().setName(enumType.getFullName());
    coreEnumBuilder.setSourceContext(SourceContext.newBuilder()
        .setFileName(enumType.getFile().getLocation().getDisplayString()));
    coreEnumBuilder.setSyntax(enumType.getSyntax());

    for (EnumValue value : enumType.getReachableValues()) {
      com.google.protobuf.EnumValue.Builder coreEnumValueBuilder =
          com.google.protobuf.EnumValue.newBuilder();

      // Use simple name for enum value, as otherwise client has to use
      // fully qualified name in the request, see b/17065168.
      coreEnumValueBuilder.setName(value.getSimpleName()).setNumber(value.getNumber());

      coreEnumValueBuilder.addAllOptions(DescriptorNormalization.getOptions(value.getProto(),
          includeDefaults));
      coreEnumBuilder.addEnumvalue(coreEnumValueBuilder.build());
    }

    coreEnumBuilder.addAllOptions(DescriptorNormalization.getOptions(enumType.getProto(),
        includeDefaults));

    enums.add(coreEnumBuilder.build());
  }

  private static java.lang.String generateTypeUrl(TypeRef type) {
    java.lang.String name;
    if (type.isMessage()) {
      name = type.getMessageType().getFullName();
    } else {
      name = type.getEnumType().getFullName();
    }
    return DescriptorNormalization.TYPE_SERVICE_BASE_URL + "/" + name;
  }

  private static Cardinality toCoreFieldCardinality(FieldDescriptorProto proto) {
    return Cardinality.valueOf(proto.getLabel().getNumber());
  }

  private static Kind toCoreFieldKind(FieldDescriptorProto proto) {
    return Kind.valueOf(proto.getType().getNumber());
  }
}
