package io.gapi.vgen;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Table;
import com.google.protobuf.Type;

import java.util.List;
import java.util.Map;

/**
 * ApiaryConfig contains additional information about discovery docs
 * parsed by {@link DiscoeryImporter} that are not easily fit into {@link com.google.api.Service}
 * itself.
 */
public class ApiaryConfig {
  // TODO(pongad): Consider using accessor methods for these fields.
  // Keep it as they are for now so that code shared with tcoffee@ doesn't break.
  // Fix after merge to master.

  /**
   * Maps method to an ordered list of parameters that the method takes.
   */
  final ListMultimap<String, String> apiParams = ArrayListMultimap.<String, String>create();

  /**
   * Maps (type name, field name) to description of that field.
   */
  final Table<String, String, String> fieldDescription = HashBasedTable.<String, String, String>create();

  /**
   * Maps method to an order list of resources the method is namespaced under.
   */
  final ListMultimap<String, String> resources = ArrayListMultimap.<String, String>create();

  /**
   * A field is in this set if it is an "additional property" in the discovery doc.
   * Indexed by type name then field name
   */
  final Table<String, String, Boolean> additionalProperties = HashBasedTable.<String, String, Boolean>create();

  /**
   * Specifies the format of the field.
   * A field is in this map only if the type of the field is string.
   * Indexed by type name then field name.
   */
  final Table<String, String, String> stringFormat = HashBasedTable.<String, String, String>create();

  // Type lookup map from DiscoveryImporter
  Map<String, Type> types;

  /**
   * @return the ordered list of parameters accepted by the given method
   */
  public List<String> getApiParams(String methodName) {
    return apiParams.get(methodName);
  }

  /**
   * @return the ordered list of resources comprising the namespace of the given method
   */
  public List<String> getResources(String methodName) {
    return resources.get(methodName);
  }

  /**
   * @return the additionalProperties
   */
  public Table<String, String, Boolean> getAdditionalProperties() {
    return additionalProperties;
  }

  /**
   * @return the stringFormat
   */
  public Table<String, String, String> getStringFormat() {
    return stringFormat;
  }

  /**
   * @return type with given name
   */
  public Type getType(String typeName) {
    return types.get(typeName);
  }
}
