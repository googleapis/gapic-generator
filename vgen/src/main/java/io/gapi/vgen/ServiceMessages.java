package io.gapi.vgen;

import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.TypeRef;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.protobuf.Empty;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class with methods to work with service methods.
 */
public class ServiceMessages {

  /**
   * Returns true if the message is the empty message.
   */
  public boolean isEmptyType(TypeRef type) {
    return type.isMessage()
        && type.getMessageType().getFullName().equals(Empty.getDescriptor().getFullName());
  }

  /**
   * Inputs a list of methods and returns only those which are page streaming.
   */
  public Iterable<Method> filterPageStreamingMethods(
      final InterfaceConfig config, List<Method> methods) {
    Predicate<Method> isPageStreaming = new Predicate<Method>() {
      @Override
      public boolean apply(Method method) {
        return config.getMethodConfig(method).isPageStreaming();
      }
    };

    return Iterables.filter(methods, isPageStreaming);
  }

  /**
   * Inputs a list of methods and returns only those which are bundling.
   */
  public Iterable<Method> filterBundlingMethods(
      final InterfaceConfig config, List<Method> methods) {
    Predicate<Method> isBundling = new Predicate<Method>() {
      @Override
      public boolean apply(Method method) {
        return config.getMethodConfig(method).isBundling();
      }
    };

    return Iterables.filter(methods, isBundling);
  }

  /**
   * Returns the list of flattened fields from the given request type, excluding
   * fields related to page streaming.
   */
  public Iterable<Field> flattenedFields(TypeRef requestType) {
    List<Field> fields = new ArrayList<>();
    for (Field field : requestType.getMessageType().getFields()) {
      String simpleName = field.getSimpleName();
      if (simpleName.equals("page_size") || simpleName.equals("page_token")) {
        continue;
      }
      fields.add(field);
    }
    return fields;
  }
}
