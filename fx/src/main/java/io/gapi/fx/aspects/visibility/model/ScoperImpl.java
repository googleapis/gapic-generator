package io.gapi.fx.aspects.visibility.model;

import io.gapi.fx.aspects.http.model.HttpAttribute;
import io.gapi.fx.model.Diag;
import io.gapi.fx.model.Element;
import io.gapi.fx.model.EnumType;
import io.gapi.fx.model.EnumValue;
import io.gapi.fx.model.Field;
import io.gapi.fx.model.FieldSelector;
import io.gapi.fx.model.Interface;
import io.gapi.fx.model.MessageType;
import io.gapi.fx.model.Method;
import io.gapi.fx.model.Model;
import io.gapi.fx.model.ProtoElement;
import io.gapi.fx.model.Scoper;
import io.gapi.fx.model.TypeRef;
import io.gapi.fx.model.Visitor;
import io.gapi.fx.util.Visits;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.Map;
import java.util.Set;

/**
 * Implementation of scopers. Applications use the factory methods, or subclass and override the
 * {@link #inScope(ProtoElement)} method of this class. The class computes the transitive closure
 * of reachable elements from a given set of roots and uses it to implement the {@link Scoper}
 * interface. During construction of a scoper, errors may be produced if scoping rules are
 * violated. See the documentation of API visibility for the rules.
 */
public abstract class ScoperImpl implements Scoper {

  /**
   * Returns a scoper which restricts a model to the elements reachable via the given roots.
   */
  public static Scoper create(Iterable<ProtoElement> roots) {
    return new ScoperImpl(roots, "") {
      @Override public boolean inScope(ProtoElement elem) {
        return true;
      }
    };
  }

  private final Iterable<? extends ProtoElement> roots;
  private final Set<ProtoElement> reachable = Sets.newHashSet();
  private final Set<ProtoElement> hasUnreachableDescendants = Sets.newHashSet();
  private final Map<Set<String>, Scoper> cachedScopers = Maps.newHashMap();
  protected final Map<ProtoElement, String> reasonForUnreachable = Maps.newLinkedHashMap();
  private final String errorContext;

  /**
   * A predicate characterizing reachability.
   */
  private final Predicate<ProtoElement> reachablePredicate = Predicates.in(reachable);

  /**
   * Determines whether the given proto element is in scope. This method must be implemented by
   * sub-classes.
   */
  public abstract boolean inScope(ProtoElement elem);


   /**
   * Constructs a scoper for the given roots. Computes reachability transitively based
   * on the {@link #inScope(ProtoElement)} predicate. This may produced errors
   * in the case elements are not in scope which are required by in-scope ones, e.g.
   * request/response messages of methods are not in scope which are used by in-scope
   * methods.
   */
  protected ScoperImpl(Iterable<? extends ProtoElement> roots, String errorContext) {
    this.errorContext = Preconditions.checkNotNull(errorContext);
    this.roots = Preconditions.checkNotNull(roots);
    Reacher reacher = new Reacher();
    reacher.visitInScope(roots);
  }

  @Override
  public boolean isReachable(ProtoElement elem) {
    return reachable.contains(elem);
  }

  @Override
  public boolean hasUnreachableDescendants(ProtoElement elem) {
    return hasUnreachableDescendants.contains(elem);
  }

  @Override
  public <E extends ProtoElement> Iterable<E> filter(Iterable<E> elems) {
    return FluentIterable.from(elems).filter(reachablePredicate);
  }

  @Override
  public Scoper restrict(Model model, final Set<String> visibilityLabels) {
    // Try to use a cached scoper. Caching scopers aids performance, but
    // more importantly, avoids reporting errors twice if the same scoper
    // setting is used repeatedly.
    Scoper cached = cachedScopers.get(visibilityLabels);
    if (cached != null) {
      return cached;
    }

    // Check whether the given combination of labels is a declared combination.
    // Say a model is to be scoped to contain visibilityLabels='{A,B}' features,
    // and we have declaredVisibilityCombinations = {{A},{A,B,C}}. {A,B} is not a valid
    // combination because it is not included in the declaredVisibilityCombinations.
    if (!model.getDeclaredVisibilityCombinations().contains(visibilityLabels)) {
      error(model,
          "Visibility '%s' is not configured for this service. Supported visibilities are: %s.",
          VisibilityUtil.DISPLAY_VISIBILITY.apply(visibilityLabels),
          Joiner.on(", ").join(
              FluentIterable.from(model.getDeclaredVisibilityCombinations())
                            .transform(VisibilityUtil.DISPLAY_VISIBILITY)));
    }

    // Construct and return new scoper.
    final ScoperImpl that = this;
    final String errorContext = String.format("Current active visibility is '%s'",
        VisibilityUtil.DISPLAY_VISIBILITY.apply(visibilityLabels));
    Scoper result = new ScoperImpl(roots, errorContext) {
      @Override public boolean inScope(ProtoElement elem) {
        if (!that.inScope(elem)) {
          this.reasonForUnreachable.put(elem, that.reasonForUnreachable.get(elem));
          return false;
        }
        VisibilityAttribute attrib = getEffectiveVisibilityAttribute(elem);
        if (attrib == null) {
          // No visibility restriction.
          return true;
        }
        Set<String> requiredLabels = attrib.getVisibilityLabels();
        if (requiredLabels.isEmpty()) {
          // No visibility restriction.
          return true;
        }
        // If one of the required labels is granted, we are fine.
        for (String label : requiredLabels) {
          if (visibilityLabels.contains(label)) {
            return true;
          }
        }
        this.reasonForUnreachable.put(elem, String.format(
            "It is hidden because its required visibility '%s' is not available",
            VisibilityUtil.DISPLAY_VISIBILITY.apply(requiredLabels),
            VisibilityUtil.DISPLAY_VISIBILITY.apply(visibilityLabels)));
        return false;
      }
    };
    cachedScopers.put(visibilityLabels, result);
    return result;
  }

  // Compute the effective visibility attribute, inheriting from parents if needed.
  private static VisibilityAttribute getEffectiveVisibilityAttribute(ProtoElement elem) {
    VisibilityAttribute attrib = elem.getAttribute(VisibilityAttribute.KEY);
    if (attrib != null && !attrib.getVisibilityLabels().isEmpty()) {
      return attrib;
    }
    if (elem.getParent() != null) {
      // Inherit from parent.
      return getEffectiveVisibilityAttribute(elem.getParent());
    }
    return null;
  }

  @Override
  public Scoper restrict(final Predicate<ProtoElement> predicate, String errorContext) {
    final ScoperImpl that = this;
    return new ScoperImpl(roots, errorContext) {
      @Override public boolean inScope(ProtoElement elem) {
        return that.inScope(elem) && predicate.apply(elem);
      }
    };
  }

  // Report an error.
  private void error(Element elem, String message, Object... params) {
    errorSince(0, elem, message, params);
  }

  // Report an error from a given config version on, otherwise a warning.
  private void errorSince(int version, Element elem, String message, Object... params) {
    if (!errorContext.isEmpty()) {
      message = message + " " + errorContext + ".";
    }
    Diag diag = elem.getModel().getConfigVersion() >= version
        ? Diag.error(elem.getLocation(), message, params)
        : Diag.warning(elem.getLocation(),
            message + String.format(
                " Note: this will be an error for config version %s and later.", version),
            params);
    elem.getModel().addDiag(diag);
  }

  private String reasonForUnreachable(ProtoElement elem) {
    if (reasonForUnreachable.containsKey(elem)) {
      return reasonForUnreachable.get(elem);
    }
    return "**Oops**: this looks like a bug, please report";
  }

  /**
   * A visitor which marks elements and its descendants as reachable, following the rules
   * for visibility propagation. Emits errors if elements are not in scope which are
   * required to be.
   */
  private class Reacher extends Visitor {

    // The set of elements visited so far. Used to shortcut visitation.
    private final Set<ProtoElement> visited = Sets.newHashSet();

    // Reach an interface. All methods will be reached which are in scope.
    @Visits void reach(Interface iface) {
      markAsReachable(iface);
      visitInScope(iface.getMethods());
    }

    // Reach a method. Both input and output must be in scope.
    @Visits void reach(Method method) {
      mustBeInScope(method, method.getInputMessage());
      mustBeInScope(method, method.getOutputMessage());
      markAsReachable(method);

      // Check whether any HTTP bounded fields are visible.
      HttpAttribute http = method.getAttribute(HttpAttribute.KEY);
      if (http != null) {
        for (HttpAttribute binding : http.getAllBindings()) {
          Iterable<FieldSelector> selectors = binding.getPathSelectors();
          if (!binding.bodyCapturesUnboundFields()) {
            // Only add body fields if '*' is not used, otherwise they aren't required.
            selectors = Iterables.concat(selectors, binding.getBodySelectors());
          }
          for (FieldSelector selector : selectors) {
            for (Field field : selector.getFields()) {
              if (!isReachable(field)) {
                errorSince(2, field,
                    "Field '%s' required by HTTP binding of method '%s' cannot be hidden. %s.",
                    field.getFullName(), method.getFullName(),
                    reasonForUnreachable(field));
              }
            }
          }
        }
      }
    }

    // Reach a message. If all fields are unreachable, do not mark the message as reachable.
    @Visits void reach(MessageType message) {
      markAsReachable(message);
      visitInScope(message.getFields());

      // Check whether required fields are hidden, and whether the message has unreachable
      // descendants.
      for (Field field : message.getFields()) {
        if (!isReachable(field)) {
          hasUnreachableDescendants.add(message);
          if (field.isRequired()) {
            error(field, "A required field cannot be hidden. %s.",
                reasonForUnreachable(field));
          }
        } else {
          TypeRef type = field.getType();
          if (type.isMessage() && hasUnreachableDescendants(type.getMessageType())
              || type.isEnum() && hasUnreachableDescendants(type.getEnumType())) {
            hasUnreachableDescendants.add(message);
          }
        }
      }
    }

    // Reach a field. The type of the field must be in scope.
    @Visits void reach(Field field) {
      TypeRef type = field.getType();
      if (type.isMap()) {
        visitInScope(type.getMessageType());
        if (type.getMapValueField().getType().isMessage()) {
          // Be sure to generate error message only for the value type, not for the internal
          // key-value message. However, the later needs to be included, therefore we did
          // visitInScope above.
          mustBeInScope(field, type.getMapValueField().getType().getMessageType());
        }
      } else if (type.isMessage()) {
        mustBeInScope(field, type.getMessageType());
      } else if (type.isEnum()) {
        mustBeInScope(field, type.getEnumType());
      }
      markAsReachable(field);
    }

    // Reach an enum. If all values are unreachable, mark the enum as unreachable. Otherwise
    // there must be at least one value which is the default.
    @Visits void reach(EnumType enumType) {
      markAsReachable(enumType);
      visitInScope(enumType.getValues());

      // Check if default value is hidden, and whether the enum has unreachable descendants.
      for (EnumValue value : enumType.getValues()) {
        if (value.getIndex() == 0 && !isReachable(value)) {
          error(value, "The default value of '%s' cannot be hidden. %s.",
              enumType.getFullName(),
              reasonForUnreachable(value));
        }
        if (!isReachable(value)) {
          hasUnreachableDescendants.add(enumType);
        }
      }
    }

    // Reach an enum value.
    @Visits void reach(EnumValue enumValue) {
      markAsReachable(enumValue);
    }

    // Requires that the given element is in scope, emits an error if not.
    private void mustBeInScope(ProtoElement context, ProtoElement elem) {
      visitInScope(elem);
      if (!isReachable(elem)) {
        // Check whether this is map field. In that case, suppress the error because we report
        // it for the entire map.
        if (context instanceof Field) {
          Field field = (Field) context;
          if (((MessageType) field.getParent()).isMapEntry()) {
            return;
          }
        }
        error(elem, "'%s' is hidden but required by visible '%s'. %s.",
            elem.getFullName(), context.getFullName(),
            reasonForUnreachable(elem));
      }
    }

    // Visits the element if it is in scope.
    private void visitInScope(ProtoElement elem) {
      if (!visited.add(elem)) {
        // Already visited, don't do it again.
        return;
      }
      if (!inScope(elem)) {
        // Not in scope, don't visit.
        return;
      }
      // Visit.
      visit(elem);
    }

    // Visits each of the elements if they are in scope.
    private void visitInScope(Iterable<? extends ProtoElement> elems) {
      for (ProtoElement elem : elems) {
        visitInScope(elem);
      }
    }

    // Marks an element as reachable. This also marks all parents as reachable.
    private void markAsReachable(ProtoElement elem) {
      reachable.add(elem);
      ProtoElement parent = elem.getParent();
      while (parent != null) {
        if (reachable.add(parent)) {
          // Parent was not reachable, check whether it is inScope and produce an error if not.
          if (!inScope(parent)) {
            errorSince(2, parent,
                "Parent '%s' of visible element '%s' cannot be hidden. %s.",
                parent.getFullName(), elem.getFullName(),
                reasonForUnreachable(parent));
          }
        }
        parent = parent.getParent();
      }
    }
  }
}
