package io.gapi.fx.aspects.visibility;

import io.gapi.fx.model.Field;
import io.gapi.fx.model.MessageType;
import io.gapi.fx.model.Scoper;
import io.gapi.fx.model.TypeRef;
import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Sets;
import com.google.protobuf.FieldMask;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * Helper class for building field masks from scoped messages.
 */
class FieldMaskBuilder {

  /**
   * Returns a {@link FieldMask} representing a message's nested visible fields,
   * or null if the message is entirely visible.
   *
   * <p>Cyclic patterns are present just once in the mask.
   * Example:
   * <pre>
   *   message Sample {
   *     optional string id = 1;
   *     optional string name = 2;
   *     optional Sample sample = 3;
   *   }
   * </pre>
   *
   * If `name` is hidden, the following FieldMask will be created:
   * <pre>
   *   mask {
   *     paths: "id"
   *     paths: "sample"
   *   }
   * </pre>
   * This differs from the documentation for `FieldMask` because there are no restrictions
   * on embedded repeated fields.
   */
  @Nullable
  static FieldMask getVisibleFieldMask(MessageType message) {
    Set<String> paths = buildFieldMaskPaths(
        message,
        "" /* path prefix */,
        new HashSet<MessageType>() /* cyclicTypes */);
    if (paths.equals(getFieldNames(message))) {
      // All the top-level fields were fully visible. Return null to indicate the message
      // doesn't require a FieldMask.
      return null;
    }
    return FieldMask.newBuilder().addAllPaths(paths).build();
  }

  private static Set<String> getFieldNames(MessageType message) {
    return FluentIterable.from(message.getFields()).transform(new Function<Field, String>() {
      @Override public String apply(Field field) {
        return field.getSimpleName();
      }

    }).toSet();
  }

  /**
   * Recursively traverses message's fields to build field mask paths.
   */
  private static Set<String> buildFieldMaskPaths(MessageType message, String prefix,
      Set<MessageType> cyclicTypes) {
    Scoper scoper = message.getModel().getScoper();
    Set<String> paths = Sets.newTreeSet();
    for (Field field : message.getReachableFields()) {
      // Skips the Key field (field number = 1) if 'message' is a MapEntry. We only need to process
      // the Value field (field number = 2) because Key&Value shouldn't be treated as fields of
      // MapEntry's in FieldMasks. The sub-fields of a Map field are the fields of its Value field's
      // sub-fields.
      if (message.isMapEntry() && field.getNumber() != 2) {
        continue;
      }
      if (field.getType().isMessage()
          && (scoper.hasUnreachableDescendants(field.getType().getMessageType())
              || containsDuplicateReachableCyclicField(field, cyclicTypes))) {
        // We can't add the whole field because some nested part of it is hidden. Traverse
        // the tree to find fields which are entirely visible.
        if (field.getType().isCyclic()) {
          MessageType cyclicType = field.getType().getMessageType();
          // Stops expanding the tree when the type of "field" has already occurred on the path from
          // the root to "field".
          if (cyclicTypes.contains(cyclicType) || message == cyclicType) {
            if (!message.isMapEntry()) {
              paths.add(prefix + field.getSimpleName());
            } else {
              // Avoids adding a "value" field of a map to the path.
              paths.add(prefix);
            }
            continue;
          }
          cyclicTypes.add(cyclicType);
        }
        // Recursively build path for message field with hidden descendants.
        paths.addAll(buildFieldMaskPaths(field.getType().getMessageType(),
            (message.isMapEntry() ? prefix : prefix + field.getSimpleName() + "."), cyclicTypes));
        if (field.getType().isCyclic()) {
          cyclicTypes.remove(field.getType().getMessageType());
        }
      } else {
        // It's a simple visible field.
        paths.add(prefix + field.getSimpleName());
      }
    }
    return paths;
  }

  /*
   * Checks recursively whether the field and its nested types contain reachable message type that
   * is already in the given cyclic types set. Returns true if it does, otherwise false.
   */
  private static boolean containsDuplicateReachableCyclicField(
      Field field, Set<MessageType> cyclicTypes) {
    TypeRef type = field.getType();
    if (type.isMessage()) {
      for (Field innerField : type.getMessageType().getReachableFields()) {
        if (innerField.getType().isMessage()) {
          if (cyclicTypes.contains(innerField.getType().getMessageType())) {
            return true;
          } else if (!innerField.getType().isCyclic()){
            if (containsDuplicateReachableCyclicField(innerField, cyclicTypes)) {
              return true;
            }
          }
        }
      }
    }
    return false;
  }
}
