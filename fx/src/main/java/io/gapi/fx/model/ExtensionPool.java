package io.gapi.fx.model;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Ordering;
import com.google.common.collect.Queues;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorSet;

import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Facilitates easy look up of extensions by name.
 */
public class ExtensionPool {
  public static final ExtensionPool EMPTY =
      new ExtensionPool(null, ImmutableMap.<String, ImmutableMultimap<String, Extension>>of());

  private static final Ordering<Entry<String, Extension>> FIELD_NUMBER_ORDERING =
      new Ordering<Entry<String, Extension>>() {
        @Override
        public int compare(
            Entry<String, Extension> left, Entry<String, Extension> right) {
          return left.getValue().getProto().getNumber() - right.getValue().getProto().getNumber();
        }
      };

  private final ImmutableMap<String, ImmutableMultimap<String, Extension>> extensions;
  private final FileDescriptorSet descriptor;

  public static final ExtensionPool create(FileDescriptorSet extensionDescriptor) {
    return new Builder().setFileDescriptorSet(extensionDescriptor).build();
  }

  private ExtensionPool(
      FileDescriptorSet descriptor,
      ImmutableMap<String, ImmutableMultimap<String, Extension>> extensions) {
    this.descriptor = descriptor;
    this.extensions = extensions;
  }

  /**
   * Returns an {@link Iterable} of extension names to {@link Extension}. Extension names
   * are a fully qualified name surrounded by parentheses. The path of the name is determined by
   * where the extension is defined. For example, given the extension defined below, the field name
   * is "(foo.bar.baz)". The returned {@link Iterable} is sorted by field number.
   *
   * <pre>
   * syntax = "proto2";
   *
   * package = foo.bar;
   *
   * import "extendee.proto";
   *
   * extend extendee.ExtendeeMessage {
   *   optional baz = 1;
   * }
   * </pre>
   */
  public Iterable<Entry<String, Extension>> getSortedExtensionsByTypeName(String name) {
    ImmutableMultimap<String, Extension> messageExtensions = extensions.get(name);
    if (messageExtensions == null) {
      return Lists.newArrayList();
    }
    return FIELD_NUMBER_ORDERING.immutableSortedCopy(messageExtensions.entries());
  }

  public FileDescriptorSet getDescriptor() {
    return descriptor;
  }

  /**
   * Encapsulates source information for an extension field.
   */
  public static class Extension {
    private final FieldDescriptorProto proto;
    private final DescriptorProtos.SourceCodeInfo.Location location;
    private final String path;
    private final Location fileLocation;

    private Extension(FieldDescriptorProto proto, DescriptorProtos.SourceCodeInfo.Location location,
        String path, Location fileLocation) {
      this.proto = proto;
      this.location = location;
      this.path = path;
      this.fileLocation = fileLocation;
    }

    public FieldDescriptorProto getProto() {
      return proto;
    }

    public DescriptorProtos.SourceCodeInfo.Location getLocation() {
      return location;
    }

    public String getPath() {
      return path;
    }

    public Location getFileLocation() {
      return fileLocation;
    }
  }

  private static class Builder {
    private static final Joiner DOT_JOINER = Joiner.on('.');
    private final Map<String, Multimap<String, Extension>> builder = Maps.newHashMap();
    private final Deque<String> fullNameSegments = Queues.newArrayDeque();
    private final Deque<Integer> pathSegments = Queues.newArrayDeque();
    private FileDescriptorSet descriptor;
    private ImmutableListMultimap<String, DescriptorProtos.SourceCodeInfo.Location> locationMap;
    private FileDescriptorProto currentFile;

    public ExtensionPool build() {
      ImmutableMap.Builder<String, ImmutableMultimap<String, Extension>> builder =
          ImmutableMap.builder();
      for (Entry<String, Multimap<String, Extension>> entry : this.builder.entrySet()) {
        builder.put(entry.getKey(), ImmutableMultimap.copyOf(entry.getValue()));
      }
      return new ExtensionPool(descriptor, builder.build());
    }

    public Builder setFileDescriptorSet(FileDescriptorSet descriptorSet) {
      Preconditions.checkState(this.descriptor == null, "can only add one FileDescriptorSet");
      this.descriptor = descriptorSet;
      for (FileDescriptorProto fileDescriptor : descriptorSet.getFileList()) {
        add(fileDescriptor);
      }
      return this;
    }

    private void add(FileDescriptorProto file) {
      currentFile = file;
      fullNameSegments.push(file.getPackage());
      locationMap = buildLocationMap(file);
      pathSegments.push(FileDescriptorProto.EXTENSION_FIELD_NUMBER);
      add(file.getExtensionList());
      pathSegments.pop();
      pathSegments.push(FileDescriptorProto.MESSAGE_TYPE_FIELD_NUMBER);
      for (int i = 0; i < file.getMessageTypeCount(); i++) {
        pathSegments.push(i);
        add(file.getMessageType(i));
        pathSegments.pop();
      }
      pathSegments.pop();
      fullNameSegments.pop();
    }

    private void add(DescriptorProto message) {
      fullNameSegments.push(message.getName());
      pathSegments.push(DescriptorProto.EXTENSION_FIELD_NUMBER);
      add(message.getExtensionList());
      pathSegments.pop();
      for (DescriptorProto nested : message.getNestedTypeList()) {
        add(nested);
      }
      fullNameSegments.pop();
    }

    private void add(List<FieldDescriptorProto> extensions) {
      for (int i = 0; i < extensions.size(); i++) {
        pathSegments.push(i);
        FieldDescriptorProto extensionProto = extensions.get(i);
        String extendee = resolve(extensionProto.getExtendee());
        Multimap<String, Extension> messageExtensions = builder.get(extendee);
        if (messageExtensions == null) {
          messageExtensions = ArrayListMultimap.create();
          builder.put(extendee, messageExtensions);
        }
        String path = DOT_JOINER.join(pathSegments.descendingIterator());
        DescriptorProtos.SourceCodeInfo.Location location = locationMap.get(path).get(0);
        // Since paths are only unique within a file, we need a synthetic path to make them unique,
        // given that paths are used to uniquely identify elements in a ProtoFile, and we're
        // stuffing elements from another file into it.
        path = currentFile.getName() + ":" + path;
        Location fileLocation = new SimpleLocation(String.format(
            "%s:%d:%d", currentFile.getName(), location.getSpan(0) + 1, location.getSpan(1) + 1));
        Extension extension = new Extension(extensionProto, location, path, fileLocation);
        messageExtensions.put(getExtensionFieldName(extensionProto.getName()), extension);
        pathSegments.pop();
      }
    }

    private String resolve(String name) {
      if (name.startsWith(".")) {
        return name.substring(1);
      }
      // TODO(tangd): implement relative extendee naming. Haven't seen it used in protoc output.
      throw new IllegalStateException("ExtensionPool relative name resolution not implemented");
    }

    private String getExtensionFieldName(String shortName) {
      String prefix = DOT_JOINER.join(fullNameSegments.descendingIterator());
      // It's technically possible not to define a package name.
      if (Strings.isNullOrEmpty(prefix)) {
        return String.format("(%s)", shortName);
      }
      return String.format("(%s.%s)", prefix, shortName);
    }

    private static ImmutableListMultimap<String, DescriptorProtos.SourceCodeInfo.Location>
        buildLocationMap(FileDescriptorProto file) {
      return Multimaps.<String, DescriptorProtos.SourceCodeInfo.Location>index(
          file.getSourceCodeInfo().getLocationList(),
          new Function<DescriptorProtos.SourceCodeInfo.Location, String>() {
            @Override
            public String apply(DescriptorProtos.SourceCodeInfo.Location location) {
              return DOT_JOINER.join(location.getPathList());
            }
          });
    }
  }
}
