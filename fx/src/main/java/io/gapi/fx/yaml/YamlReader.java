package io.gapi.fx.yaml;

// TODO: MIGRATION import com.google.api.ApiLegacy.Legacy;
// import com.google.api.deploy.Deployments;

import com.google.api.Service;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.CaseFormat;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.google.common.io.Files;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;

import io.gapi.fx.model.Diag;
import io.gapi.fx.model.DiagCollector;
import io.gapi.fx.model.Location;
import io.gapi.fx.model.SimpleLocation;
import io.gapi.fx.yaml.ProtoFieldValueParser.ParseException;
import io.gapi.fx.yaml.ProtoFieldValueParser.UnsupportedTypeException;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.representer.Representer;
import org.yaml.snakeyaml.resolver.Resolver;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * Yaml configuration reader.
 */
public class YamlReader {

   /**
   * Reads protos from Yaml, reporting errors to the diag collector. This expects a top-level
   * field 'type' in the config which must be one of the statically configured config types
   * found in {@link #SUPPORTED_CONFIG_TYPES}.
   *
   * <p>Returns proto {@link Message} representing the config, or null if
   * errors detected while processing the input.
   */
  @Nullable public static Message read(DiagCollector collector, String inputName, String input) {
    return new YamlReader(collector, inputName, SUPPORTED_CONFIG_TYPES).read(input);
  }

  /**
   * Same as {@link #read(DiagCollector, String, String)} but allows to specify a map from
   * message type names to message prototypes which represents the allowed top-level types.
   */
  @Nullable public static Message read(DiagCollector collector, String inputName, String input,
      Map<String, Message> supportedConfigTypes) {
    return new YamlReader(collector, inputName, supportedConfigTypes).read(input);
  }

  /**
   * Same as {@link #read(DiagCollector, String, String, Map)} but reads the input from a file.
   */
  @Nullable public static Message read(DiagCollector collector, File input,
      Map<String, Message> supportedConfigTypes) {
    try {
      String fileContent = Files.toString(input, Charset.forName("UTF8"));
      return read(collector, input.getName(), fileContent, supportedConfigTypes);
    } catch (IOException e) {
      collector.addDiag(Diag.error(SimpleLocation.TOPLEVEL,
          "Cannot read configuration file '%s': %s", input.getName(), e.getMessage()));
      return null;
    }
  }

  // An instance of the snakeyaml reader which does not do any implicit conversions.
  private static final Yaml YAML =
      new Yaml(new Constructor(), new Representer(), new DumperOptions(), new Resolver());

  // Supported configuration types. (May consider to move this out here for more generic
  // use.)
  @VisibleForTesting
  static final Map<String, Message> SUPPORTED_CONFIG_TYPES =
      ImmutableMap.<String, Message>of(
          Service.getDescriptor().getFullName(), Service.getDefaultInstance());
          // TODO: MIGRATION
          //Legacy.getDescriptor().getFullName(), Legacy.getDefaultInstance(),
          //Deployments.getDescriptor().getFullName(), Deployments.getDefaultInstance());

  private static final String TYPE_KEY = "type";
  private static final Predicate<String> TYPE_KEY_FILTER = new Predicate<String>() {
    @Override public boolean apply(String key) {
      return !key.equals(TYPE_KEY);
    }
  };

  private static final Set<String> WRAPPER_TYPES = ImmutableSet.<String>builder()
      .add("google.protobuf.DoubleValue")
      .add("google.protobuf.FloatValue")
      .add("google.protobuf.Int64Value")
      .add("google.protobuf.Int32Value")
      .add("google.protobuf.UInt64Value")
      .add("google.protobuf.UInt32Value")
      .add("google.protobuf.Int32Value")
      .add("google.protobuf.BoolValue")
      .add("google.protobuf.StringValue")
      .add("google.protobuf.BytesValue")
      .build();

  private final DiagCollector diag;
  private final String inputName;
  private final Map<String, Message> supportedConfigTypes;

  private final Deque<String> location = Queues.newArrayDeque();

  private YamlReader(DiagCollector diag, String inputName,
      Map<String, Message> supportedConfigTypes) {
    this.inputName = inputName;
    this.diag = diag;
    this.supportedConfigTypes = supportedConfigTypes;
  }

  private Message read(String input) {
    try {
      int initialErrorCount = diag.getErrorCount();
      location.push("<toplevel>");
      Object tree;
      try {
        tree = YAML.load(input);
      } catch (Exception e) {
        error("Parsing error: " + e.getMessage());
        return null;
      }

      // Identify the configuration type.
      if (!(tree instanceof Map)) {
        error("Expected a map as a root object.");
        return null;
      }
      @SuppressWarnings("unchecked")
      Map<String, Object> map = (Map<String, Object>) tree;
      Object typeName = map.get(TYPE_KEY);
      if (!(typeName instanceof String)) {
        error("Expected a field '%s' specifying the configuration type name in root object.",
            TYPE_KEY);
        return null;
      }
      Message prototype = supportedConfigTypes.get(typeName);
      if (prototype == null) {
        error("The specified configuration type '%s' is unknown.",
            typeName);
        return null;
      }

      // Read the config.
      Message.Builder builder = prototype.toBuilder();
      read(builder, Maps.filterKeys(map, TYPE_KEY_FILTER));
      return diag.getErrorCount() == initialErrorCount ? builder.build() : null;
    } finally {
      location.clear();
    }
  }

  private void read(Message.Builder builder, Object node) {
    if (node == null) {
      return;
    }

    Descriptor messageType = builder.getDescriptorForType();

    if (WRAPPER_TYPES.contains(messageType.getFullName())) {
      // Message is a wrapper type. Directly read value into wrapped field.
      FieldDescriptor wrapperField = builder.getDescriptorForType().findFieldByName("value");
      read(builder, wrapperField, node);
      return;
    }

    if (!(node instanceof Map)) {
      error("Expected a map to merge with '%s'.", messageType.getFullName());
      return;
    }
    if (builder.getDescriptorForType().getOptions().getDeprecated()) {
      warning("The type '%s' is deprecated.", messageType.getFullName());
    }
    @SuppressWarnings("unchecked")
    Map<String, Object> map = (Map<String, Object>) node;
    for (String key : map.keySet()) {
      FieldDescriptor field = messageType.findFieldByName(key);
      if (field == null) {
        error("Found field '%s' which is unknown in '%s'.", key,
            builder.getDescriptorForType().getFullName());
      } else {
        if (field.getOptions().getDeprecated()) {
          warning("The field '%s' is deprecated.", field.getName());
        }
        location.push(field.getName());
        read(builder, field, map.get(key));
        location.pop();
      }
    }
  }

  private void read(Message.Builder builder, FieldDescriptor field, Object value) {
    if (field.getType() == FieldDescriptor.Type.MESSAGE) {
      if (field.isRepeated()) {
        // Handle map field.
        if (field.getMessageType().getOptions().getMapEntry()) {
          Map<Object, Object> map = expectMap(field, value);
          Map<Object, Object> protoMap = getMutableMapFromBuilder(builder, field);
          FieldDescriptor keyField = field.getMessageType().getFields().get(0);
          FieldDescriptor valueField = field.getMessageType().getFields().get(1);
          for (Map.Entry<Object, Object> entry : map.entrySet()) {
            // Proto Map key does not support Message type.
            Object keyObj = convert(keyField, entry.getKey());
            if (valueField.getType() == FieldDescriptor.Type.MESSAGE) {
              // We need to get MapEntry builder first from parent builder, and then use the
              // map entry builder to get the value field builder.
              Message.Builder valueBuilder = builder.newBuilderForField(field)
                  .newBuilderForField(valueField);
              read(valueBuilder, entry.getValue());
              protoMap.put(keyObj, valueBuilder.build());
            } else {
              protoMap.put(keyObj, convert(valueField, entry.getValue()));
            }
          }
        } else {
          List<Object> list = expectList(field, value);
          for (Object elem : list) {
            read(addRepeatedFieldBuilder(builder, field), elem);
          }
        }
      } else {
        read(builder.getFieldBuilder(field), value);
      }
    } else {
      if (field.isRepeated()) {
        List<Object> list = expectList(field, value);
        for (Object elem : list) {
          elem = convert(field, elem);
          if (elem != null) {
            builder.addRepeatedField(field, elem);
          }
        }
      } else {
        value = convert(field, value);
        if (value != null) {
          builder.setField(field, value);
        }
      }
    }
  }

  private Object convert(FieldDescriptor field, Object value) {
    if (value instanceof List || value instanceof Map) {
      error("Expected a primitive value for field '%s'.", field.getName());
      return null;
    }

    try {
      if (value == null) {
        return null;
      }
      return ProtoFieldValueParser.parseFieldFromString(field, value.toString());
    } catch (ParseException | UnsupportedTypeException e) {
      error("Parsing of field '%s' failed: %s", field.getName(), e.getMessage());
      return null;
    }
  }

  @SuppressWarnings("unchecked")
  private List<Object> expectList(FieldDescriptor field, Object node) {
    if (node instanceof String) {
      // Allow a singleton as a list.
      return ImmutableList.of(node);
    }
    if (!(node instanceof List)) {
      error("Expected a list for field '%s'.", field.getFullName());
      return ImmutableList.of();
    }
    return (List<Object>) node;
  }

  @SuppressWarnings("unchecked")
  private Map<Object, Object> expectMap(FieldDescriptor field, Object node) {
    if (!(node instanceof Map)) {
      error("Expected a map to merge with '%s'.", field.getFullName());
      return ImmutableMap.of();
    }
    return (Map<Object, Object>) node;
  }

  /**
   * Helper method to get the mutable {@link Map} from message builder for the map field using
   * reflection. The method name is getMutable{fieldName}.
   */
  @SuppressWarnings("unchecked")
  private Map<Object, Object> getMutableMapFromBuilder(Message.Builder builder,
      FieldDescriptor field) {
    try {
      java.lang.reflect.Method method = builder.getClass().getMethod(
          String.format("getMutable%s", CaseFormat.LOWER_UNDERSCORE.to(
              CaseFormat.UPPER_CAMEL, field.getName())));
      return (Map<Object, Object>) method.invoke(builder);
    } catch (NoSuchMethodException | InvocationTargetException e) {
      throw Throwables.propagate(e);
    } catch (IllegalAccessException e) {
      throw Throwables.propagate(e.getCause());
    }
  }

  private void error(String message, Object... params) {
    diag.addDiag(Diag.error(location(), message, params));
  }

  private void warning(String message, Object... params) {
    diag.addDiag(Diag.warning(location(), message, params));
  }

  private Location location() {
    return new SimpleLocation(
        String.format("%s (at %s)", inputName,
            Joiner.on(".").join(Lists.reverse(ImmutableList.copyOf(location)))));
  }

  /**
   * A helper method used to add a builder to a repeated field. Unlike most other builder methods,
   * the method {@code FieldType.BuilderaddXXXBuilder(Container.Builder)} is not exposed via the
   * generic api, so we have to call it via reflection.
   */
  private static Message.Builder addRepeatedFieldBuilder(Message.Builder builder,
      FieldDescriptor field) {
    try {
      java.lang.reflect.Method method = builder.getClass().getMethod(
          "add" + CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, field.getName())
              + "Builder");
      return (Message.Builder) method.invoke(builder);
    } catch (NoSuchMethodException | InvocationTargetException e) {
      throw Throwables.propagate(e);
    } catch (IllegalAccessException e) {
      throw Throwables.propagate(e.getCause());
    }
  }
}
