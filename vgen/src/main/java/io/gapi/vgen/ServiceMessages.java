package io.gapi.vgen;

import io.gapi.fx.model.Field;
import io.gapi.fx.model.MessageType;
import io.gapi.fx.model.TypeRef;
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
   * Returns true if the request is a page streaming one (with fields like page_size and
   * page_token)
   */
  // TODO (garrettjones) consolidate logic with ProtoPageDescriptor
  public boolean isPageStreamingRequest(TypeRef returnType, TypeRef requestType) {
    if (!requestType.isMessage()) {
      return false;
    }
    MessageType requestMessageType = requestType.getMessageType();
    Field pageTokenField = requestMessageType.lookupField("page_token");
    if (pageTokenField == null) {
      return false;
    }
    if (!returnType.isMessage()) {
      return false;
    }
    MessageType returnMessageType = returnType.getMessageType();
    Field nextPageTokenField = returnMessageType.lookupField("next_page_token");
    if (nextPageTokenField == null) {
      return false;
    }
    TypeRef elementType = pageStreamingElementTypeRefIfExists(returnType);
    if (elementType == null) {
      return false;
    }
    return true;
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
