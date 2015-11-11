package io.gapi.vgen;

import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.MessageType;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.TypeRef;
import com.google.protobuf.Empty;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

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
   * Inputs a list of methods and returns only those which are page streaming
   */
  public Iterable<Method> filterPageStreamingMethods(ApiConfig config, List<Method> methods) {
    Predicate<Method> isPageStreaming = new Predicate<Method>() {
      @Override
      public boolean apply(Method method) {
        return config.getMethodConfig(method).isPageStreaming();
      }
    };

    return Iterables.filter(methods, isPageStreaming);
  }

  /**
   * For a page streaming request, if there is only one field in the response, then returns
   * that type, else returns the response type.
   */
  public TypeRef pageStreamingElementTypeRef(TypeRef returnType) {
    TypeRef elementType = pageStreamingElementTypeRefIfExists(returnType);
    if (elementType == null) {
      throw new IllegalStateException("pageStreamingElementTypeRef: no appropriate page streaming"
          + " element found.");
    }
    return elementType;
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

  private TypeRef pageStreamingElementTypeRefIfExists(TypeRef returnType) {
    TypeRef elementType = null;
    for (Field field : returnType.getMessageType().getFields()) {
      if (field.getSimpleName().equals("next_page_token")) {
        continue;
      }
      if (elementType == null) {
        elementType = field.getType();
      } else {
        // we found multiple content fields, so we need to return the full Response type
        elementType = returnType;
        break;
      }
    }
    return elementType;
  }

}