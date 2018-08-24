package com.google.api.codegen.util;

import com.google.api.Resource;
import com.google.api.tools.framework.model.Field;

import java.util.Optional;
import java.util.stream.Collectors;

// Utils for parsing proto annotations.
public class ProtoAnnotations {
  public static final String RESOURCE_ANNOTATION = "google.api.resources";

  public static String getResourcePath(Field field) {
     return field.getOptionFields()
         .entrySet()
         .stream()
         .filter(entry -> entry.getKey().getFullName().equals("google.api.resource"))
         .map(entry -> ((Resource) entry.getValue()).getPath())
         .findFirst()
         .orElse(null);
  }
}
