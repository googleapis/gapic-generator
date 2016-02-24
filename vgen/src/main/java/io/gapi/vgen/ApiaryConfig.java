package io.gapi.vgen;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Table;

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
}
