package io.gapi.fx.tools;

import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * Class for tool options.
 */
public class ToolOptions {
  private ToolOptions() {}

  /**
   * Represents an option.
   */
  @AutoValue
  public static abstract class Option<T> {
    public abstract Key<T> key();
    public abstract String name();
    public abstract String description();
    @Nullable public abstract T defaultValue();
  }

  private static final List<Option<?>> registeredOptions = Lists.newArrayList();

  /**
   * Creates a new option from a type.
   */
  @SuppressWarnings("unchecked")
  public static <T> Option<T> createOption(Class<T> type, String name,
      String description, T defaultValue) {
    return createOption(TypeLiteral.get(type), name, description, defaultValue);
  }

  /**
   * Creates a new option from a type literal.
   */
  @SuppressWarnings("unchecked")
  public static <T> Option<T> createOption(TypeLiteral<T> type, String name,
      String description, T defaultValue) {
    Option<T> option = (Option<T>) (Object) new AutoValue_ToolOptions_Option<T>(
        Key.get(type, Names.named(name)), name, description, defaultValue);
    registeredOptions.add(option);
    return option;
  }

  public static Iterable<Option<?>> allOptions() {
    return registeredOptions;
  }

  public static final Option<String> DESCRIPTOR_SET = createOption(
      String.class,
      "descriptor",
      "The descriptor set representing the compiled protos the tool works on.",
      "");

  public static final Option<List<String>> CONFIG_FILES = createOption(
      new TypeLiteral<List<String>>(){},
      "configs",
      "The list of Yaml configuration files.",
      ImmutableList.<String>of());

  public static final Option<List<String>> PROTO_SOURCES = createOption(
      new TypeLiteral<List<String>>(){},
      "(protoSources)", // Enclose in braces to not interpret as a flag
      "The sources which are considered to be owned (in contrast to imported).",
      ImmutableList.<String>of());

  public static final Option<List<String>> EXPERIMENTS = createOption(
      new TypeLiteral<List<String>>(){},
      "experiments",
      "Any experiments to be applied by the tool.",
      ImmutableList.<String>of());

  public static final Option<String> VISIBILITY_LABELS = createOption(
      String.class,
      "visibility_labels",
      "The visibility labels for the normalized service config, delimited by comma.",
      null);

  public static final Option<String> DATA_PATH = createOption(
      String.class,
      "dataPath",
      "A path to lookup data (like doc files). Separated by the platforms path separator.",
      "");

  public static final Option<String> OUTPUT_ENDPOINT = createOption(
      String.class,
      "output_endpoint",
      "The endpoint to generate the discovery doc for. Defaults to the name of the service config.",
      "");

  public static final Option<String> EXTENSION_DESCRIPTOR_SET = createOption(
      String.class,
      "extension_descriptor",
      "A proto descriptor set with extensions to be processed (for proto2).",
      "");

  private final Map<Key<?>, Object> options = Maps.newHashMap();

  /**
   * Returns new tool options with given basic information.
   */
  public static ToolOptions create(String descriptorSet, Iterable<String> sourceFiles) {
    return new ToolOptions().set(DESCRIPTOR_SET, descriptorSet)
        .set(PROTO_SOURCES, ImmutableList.copyOf(sourceFiles));
  }

  /**
   * Sets an option.
   */
  @SuppressWarnings("unchecked")
  @Nullable
  public <T> ToolOptions set(Option<T> option, T value) {
    options.put(option.key(), Preconditions.checkNotNull(value));
    return this;
  }

  /**
   * Gets an option, or its default value if it is not set.
   */
  @SuppressWarnings("unchecked")
  public <T> T get(Option<T> option) {
    Object value = options.get(option.key());
    if (value == null) {
      return option.defaultValue();
    }
    return (T) value;
  }


}