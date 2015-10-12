package io.gapi.fx.model;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;

import io.gapi.fx.model.stages.Requires;
import io.gapi.fx.model.stages.Resolved;

import java.util.Map;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * Represents a symbol table, an object mapping interfaces and types by name. Established by stage
 * {@link Resolved}.
 *
 */
@Requires(Resolved.class)
@Immutable
public class SymbolTable {

  private final ImmutableMap<String, Interface> interfaceByName;
  private final ImmutableMap<String, TypeRef> typeByName;

  public SymbolTable(Map<String, Interface> interfaceByName, Map<String, TypeRef> typeByName) {
    this.interfaceByName = ImmutableMap.copyOf(interfaceByName);
    this.typeByName = ImmutableMap.copyOf(typeByName);
  }

  /**
   * Get the interface by its full name.
   */
  @Nullable
  public Interface lookupInterface(String fullName) {
    return interfaceByName.get(fullName);
  }

  /**
   * Resolves a interface by a partial name within a given package context, following PB (== C++)
   * conventions.
   */
  @Nullable
  public Interface resolveInterface(String inPackage, String name) {
    for (String cand : nameCandidates(inPackage, name)) {
      Interface endpointInterface = lookupInterface(cand);
      if (endpointInterface != null) {
        return endpointInterface;
      }
    }
    return null;
  }

  /**
   * Get the type by its full name.
   */
  @Nullable
  public TypeRef lookupType(String fullName) {
    return typeByName.get(getTypeNameInSymbolTable(fullName));
  }

  /**
   * Returns the type name used to store in symbol table.
   *
   * <p>Message fullname starts with a '.' if no package is specified in the proto file.
   * Remove the preceding '.' to make it consistent with other types.
   */
  public static String getTypeNameInSymbolTable(String fullName) {
    return fullName = fullName.startsWith(".") ? fullName.substring(1) : fullName;
  }

  /**
   * Resolves a type by its partial name within a given package context, following PB (== C++)
   * conventions. If the given name is a builtin type name for a primitive type in the PB
   * language, a reference for that type will be returned.
   */
  @Nullable
  public TypeRef resolveType(String inPackage, String name) {
    TypeRef type = TypeRef.fromPrimitiveName(name);
    if (type != null) {
      return type;
    }
    for (String cand : nameCandidates(inPackage, name)) {
      type = lookupType(cand);
      if (type != null) {
        return type;
      }
    }
    return null;
  }

  /**
   * Get all interfaces in the symbol table.
   */
  public ImmutableCollection<Interface> getInterfaces() {
    return interfaceByName.values();
  }

  /**
   * Get all declared types in the symbol table.
   */
  public ImmutableCollection<TypeRef> getDeclaredTypes() {
    return typeByName.values();
  }

  /**
   * Returns the candidates for name resolution of a name within a container(e.g. package, message,
   * enum, service elements) context following PB (== C++) conventions. Iterates those names which
   * shadow other names first; recognizes and removes a leading '.' for overriding shadowing. Given
   * a container name {@code a.b.c.M.N} and a type name {@code R.s}, this will deliver in order
   * {@code a.b.c.M.N.R.s, a.b.c.M.R.s, a.b.c.R.s, a.b.R.s, a.R.s, R.s}.
   */
  public static Iterable<String> nameCandidates(String inContainer, String name) {
    // TODO(wgg): we may want to make this a true lazy iterable for performance.
    if (name.startsWith(".")) {
      return FluentIterable.of(new String[]{ name.substring(1) });
    }
    if (inContainer.length() == 0) {
      return FluentIterable.of(new String[]{ name });
    } else {
      int i = inContainer.lastIndexOf('.');
      return FluentIterable.of(new String[]{ inContainer + "." + name })
          .append(nameCandidates(i >= 0 ? inContainer.substring(0, i) : "", name));
    }
  }

  /**
   * Attempts to resolve the given id into a protocol element, applying certain heuristics.
   *
   * <p>First the name is attempted to interpret as a type or as an interface, in that order.
   * If that succeeds, the associated proto element is returned.
   *
   * <p>If resolution does not succeed, the last component of the name is chopped of, and
   * resolution is attempted recursively on the parent name. On success, the chopped name
   * is looked up in the parent depending on its type.
   */
  @Nullable public ProtoElement resolve(String id) {
    TypeRef type = lookupType(id);
    if (type != null) {
      if (type.isMessage()) {
        return type.getMessageType();
      }
      if (type.isEnum()) {
        return type.getEnumType();
      }
      throw new IllegalStateException("Unexpected type resolution.");
    }

    Interface iface = lookupInterface(id);
    if (iface != null) {
      return iface;
    }

    int i = id.lastIndexOf('.');
    if (i < 0) {
      return null;
    }
    String lastName = id.substring(i + 1);
    id = id.substring(0, i);
    ProtoElement parent = resolve(id);
    if (parent != null) {
      if (parent instanceof Interface) {
        return ((Interface) parent).lookupMethod(lastName);
      }
      if (parent instanceof MessageType) {
        return ((MessageType) parent).lookupField(lastName);
      }
      if (parent instanceof EnumType) {
        return ((EnumType) parent).lookupValue(lastName);
      }
    }
    return null;
  }
}
