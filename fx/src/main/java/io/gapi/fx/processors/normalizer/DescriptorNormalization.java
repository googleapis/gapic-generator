package io.gapi.fx.processors.normalizer;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.protobuf.Any;
import com.google.protobuf.BoolValue;
import com.google.protobuf.ByteString;
import com.google.protobuf.BytesValue;
import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.EnumDescriptorProto;
import com.google.protobuf.DescriptorProtos.EnumOptions;
import com.google.protobuf.DescriptorProtos.EnumValueDescriptorProto;
import com.google.protobuf.DescriptorProtos.EnumValueOptions;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldOptions;
import com.google.protobuf.DescriptorProtos.MessageOptions;
import com.google.protobuf.DescriptorProtos.MethodDescriptorProto;
import com.google.protobuf.DescriptorProtos.MethodOptions;
import com.google.protobuf.DescriptorProtos.OneofDescriptorProto;
import com.google.protobuf.DescriptorProtos.ServiceDescriptorProto;
import com.google.protobuf.DescriptorProtos.ServiceOptions;
import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.FloatValue;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Int64Value;
import com.google.protobuf.Message;
import com.google.protobuf.Option;
import com.google.protobuf.StringValue;
import com.google.protobuf.UInt32Value;
import com.google.protobuf.UInt64Value;

import java.util.List;
import java.util.Map;

/**
 * Descriptor normalization utility methods.
 */
public final class DescriptorNormalization {
  private  DescriptorNormalization() {}

  static final java.lang.String TYPE_SERVICE_BASE_URL = "type.googleapis.com";

  private static final Predicate<FieldDescriptor> HAS_DEFAULT_VALUE =
      new Predicate<FieldDescriptor>() {
        @Override
        public boolean apply(FieldDescriptor input) {
          return input.hasDefaultValue();
        }
  };

  private static final List<FieldDescriptor> DEFAULT_ENUM_OPTIONS =
      FluentIterable.from(EnumOptions.getDescriptor().getFields())
      .filter(HAS_DEFAULT_VALUE)
      .toList();

  private static final List<FieldDescriptor> DEFAULT_ENUM_VALUE_OPTIONS =
      FluentIterable.from(EnumValueOptions.getDescriptor().getFields())
      .filter(HAS_DEFAULT_VALUE)
      .toList();

  private static final List<FieldDescriptor> DEFAULT_FIELD_OPTIONS =
      FluentIterable.from(FieldOptions.getDescriptor().getFields())
      .filter(HAS_DEFAULT_VALUE)
      .toList();

  private static final List<FieldDescriptor> DEFAULT_MESSAGE_OPTIONS =
      FluentIterable.from(MessageOptions.getDescriptor().getFields())
      .filter(HAS_DEFAULT_VALUE)
      .toList();

  private static final List<FieldDescriptor> DEFAULT_METHOD_OPTIONS =
      FluentIterable.from(MethodOptions.getDescriptor().getFields())
      .filter(HAS_DEFAULT_VALUE)
      .toList();

  private static final List<FieldDescriptor> DEFAULT_SERVICE_OPTIONS =
      FluentIterable.from(ServiceOptions.getDescriptor().getFields())
      .filter(HAS_DEFAULT_VALUE)
      .toList();

  // TODO: MIGRATION
//  private static final List<FieldDescriptor> DEFAULT_STREAM_OPTIONS =
//      FluentIterable.from(StreamOptions.getDescriptor().getFields())
//      .filter(HAS_DEFAULT_VALUE)
//      .toList();

  public static List<Option> getOptions(EnumDescriptorProto descriptor) {
    return getOptions(descriptor, true);
  }

  public static List<Option> getOptions(EnumDescriptorProto descriptor, boolean withDefaults) {
    return toCoreOptions(maybeCombineOptionsWithDefault(withDefaults,
        descriptor.getOptions().getAllFields(), DEFAULT_ENUM_OPTIONS));
  }

  public static List<Option> getOptions(EnumValueDescriptorProto descriptor) {
    return getOptions(descriptor, true);
  }

  public static List<Option> getOptions(EnumValueDescriptorProto descriptor, boolean withDefaults) {
    return toCoreOptions(maybeCombineOptionsWithDefault(withDefaults,
        descriptor.getOptions().getAllFields(), DEFAULT_ENUM_VALUE_OPTIONS));
  }

  public static List<Option> getOptions(FieldDescriptorProto descriptor) {
    return getOptions(descriptor, true);
  }

  public static List<Option> getOptions(FieldDescriptorProto descriptor, boolean withDefaults) {
    return toCoreOptions(maybeCombineOptionsWithDefault(withDefaults,
        descriptor.getOptions().getAllFields(), DEFAULT_FIELD_OPTIONS));
  }

  public static List<Option> getOptions(DescriptorProto descriptor) {
    return getOptions(descriptor, true);
  }

  public static List<Option> getOptions(DescriptorProto descriptor, boolean withDefaults) {
    return toCoreOptions(maybeCombineOptionsWithDefault(withDefaults,
        descriptor.getOptions().getAllFields(), DEFAULT_MESSAGE_OPTIONS));
  }

  public static List<Option> getOptions(MethodDescriptorProto descriptor) {
    return getOptions(descriptor, true);
  }

  public static List<Option> getOptions(MethodDescriptorProto descriptor, boolean withDefaults) {
    return toCoreOptions(maybeCombineOptionsWithDefault(withDefaults,
        descriptor.getOptions().getAllFields(), DEFAULT_METHOD_OPTIONS));
  }

  // TODO: MIGRATION
//  public static List<Option> getOptions(StreamDescriptorProto descriptor) {
//    return getOptions(descriptor, true);
//  }
//
//  public static List<Option> getOptions(StreamDescriptorProto descriptor, boolean withDefaults) {
//    return toCoreOptions(maybeCombineOptionsWithDefault(withDefaults,
//        descriptor.getOptions().getAllFields(), DEFAULT_STREAM_OPTIONS));
//  }

  public static List<Option> getOptions(ServiceDescriptorProto descriptor) {
    return getOptions(descriptor, true);
  }

  public static List<Option> getOptions(ServiceDescriptorProto descriptor, boolean withDefaults) {
    return toCoreOptions(maybeCombineOptionsWithDefault(withDefaults,
        descriptor.getOptions().getAllFields(), DEFAULT_SERVICE_OPTIONS));
  }

  public static List<String> getOneofs(DescriptorProto descriptor) {
    return FluentIterable.from(descriptor.getOneofDeclList()).transform(
      new Function<OneofDescriptorProto, String>() {
        @Override
        public String apply(OneofDescriptorProto from) {
          return from.getName();
        }}).toList();
  }

  static List<Option> getMethodOptions(Map<FieldDescriptor, Object> options, boolean isStream) {
    return getMethodOptions(options, isStream, true);
  }

  static List<Option> getMethodOptions(Map<FieldDescriptor, Object> options, boolean isStream,
      boolean withDefaults) {
    // TODO: MIGRATION
    return toCoreOptions(maybeCombineOptionsWithDefault(withDefaults,
        options, /*isStream ? DEFAULT_STREAM_OPTIONS :*/ DEFAULT_METHOD_OPTIONS));
  }

  /**
   * Converts a list of proto options to {@link Option}. The input options are passed in
   * as {@link Iterable} of {@link FieldDescriptor}:{@link Object} pair to make it reusable across
   * various kinds of options.
   */
  private static List<Option> toCoreOptions(Map<FieldDescriptor, Object> options) {
    ImmutableList.Builder<Option> builder = ImmutableList.builder();

    for (Map.Entry<FieldDescriptor, Object> entry : options.entrySet()) {
      FieldDescriptor optionDescriptor = entry.getKey();
      Option.Builder optionBuilder = Option.newBuilder().setName(optionDescriptor.getFullName());
      if (optionDescriptor.isRepeated()) {
        // For repeated field, convert each one to an Any.proto.
        List<?> values = (List<?>) entry.getValue();
        for (Object value : values) {
          optionBuilder.setValue(toAnyType(optionDescriptor.getType(), value));
        }
      } else {
        optionBuilder.setValue(toAnyType(optionDescriptor.getType(), entry.getValue()));
      }
      builder.add(optionBuilder.build());
    }

    return builder.build();
  }

  /**
   * Convert a protocol buffer field value to {@link Any} with the below rule:
   * <ul>
   *    <li> If the field is a primitive type and can be mapped to a wrapper message type
   *    defined in //tech/type/proto/wrappers.proto, its value is boxed into a wrapper
   *     and goes to Any.value, the type name of the wrapper goes to Any.type_url.
   *    <li> If the field is already a message, its type name goes to Any.type_url,
   *    its value directly goes to Any.value
   *    <li> If the field is an enum value, the name of the enum value is boxed into
   *    tech.type.String and put into Any.value, and tech.type.String goes to Any.type_url.
   */
  private static Any toAnyType(FieldDescriptor.Type protobufType, Object value) {
    Any.Builder builder = Any.newBuilder();
    java.lang.String typeFullName;
    Message wrapperMessage;
    switch (protobufType) {
      case MESSAGE:
        wrapperMessage = (Message) value;
        typeFullName = wrapperMessage.getDescriptorForType().getFullName();
        break;
      case ENUM:
        // NOTE: Erasing the enum type to the String wrapper is currently intentional, to avoid
        // the need to add an enum wrapper type.  This may change in the future.
        typeFullName = StringValue.getDescriptor().getFullName();
        wrapperMessage =
            StringValue.newBuilder().setValue(((EnumValueDescriptor) value).getName()).build();
        break;
      case BOOL:
        typeFullName = BoolValue.getDescriptor().getFullName();
        wrapperMessage = BoolValue.newBuilder().setValue((Boolean) value).build();
        break;
      case DOUBLE:
        typeFullName = DoubleValue.getDescriptor().getFullName();
        wrapperMessage = DoubleValue.newBuilder().setValue((java.lang.Double) value).build();
        break;
      case FLOAT:
        typeFullName = FloatValue.getDescriptor().getFullName();
        wrapperMessage = FloatValue.newBuilder().setValue((java.lang.Float) value).build();
        break;
      case STRING:
        typeFullName = StringValue.getDescriptor().getFullName();
        wrapperMessage = StringValue.newBuilder().setValue((java.lang.String) value).build();
        break;
      case SINT32:
      case SFIXED32:
      case INT32:
        typeFullName = Int32Value.getDescriptor().getFullName();
        wrapperMessage = Int32Value.newBuilder().setValue((Integer) value).build();
        break;
      case SINT64:
      case SFIXED64:
      case INT64:
        typeFullName = Int64Value.getDescriptor().getFullName();
        wrapperMessage = Int64Value.newBuilder().setValue((Long) value).build();
        break;
      case UINT32:
      case FIXED32:
        typeFullName = UInt32Value.getDescriptor().getFullName();
        wrapperMessage = UInt32Value.newBuilder().setValue((Integer) value).build();
        break;
      case UINT64:
      case FIXED64:
        typeFullName = UInt64Value.getDescriptor().getFullName();
        wrapperMessage = UInt64Value.newBuilder().setValue((Long) value).build();
        break;
      case BYTES:
        typeFullName = BytesValue.getDescriptor().getFullName();
        wrapperMessage = BytesValue.newBuilder().setValue(ByteString.copyFrom((byte[]) value))
            .build();
        break;
      default:
        throw new IllegalArgumentException("Type " + protobufType.name()
            + " cannot be converted to Any type.");
    }
    return builder.setTypeUrl(TYPE_SERVICE_BASE_URL + "/" + typeFullName)
        .setValue(wrapperMessage.toByteString()).build();
  }

  /**
   * Combines default options with options that are explicitly set.
   */
  private static Map<FieldDescriptor, Object> maybeCombineOptionsWithDefault(
      boolean withDefaults,
      Map<FieldDescriptor, Object> explicitlySetOptions,
      List<FieldDescriptor> defaultOptions) {
    if (!withDefaults) {
      return explicitlySetOptions;
    }

    Map<FieldDescriptor, Object> allOptions = Maps.newLinkedHashMap();
    for (FieldDescriptor descriptor : defaultOptions) {
      allOptions.put(descriptor, descriptor.getDefaultValue());
    }
    // putAll overrides the default with the explicitly-set values.
    allOptions.putAll(explicitlySetOptions);

    return ImmutableMap.copyOf(allOptions);
  }
}
