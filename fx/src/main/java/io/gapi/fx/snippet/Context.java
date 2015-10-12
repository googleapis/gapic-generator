package io.gapi.fx.snippet;

import io.gapi.fx.snippet.SnippetSet.SnippetKey;
import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Queues;

import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * Represents a context for snippet evaluation. The context holds snippet
 * definitions and global and local variables.
 */
public class Context {

  private static final Map<String, Object> BUILTIN =
      ImmutableMap.<String, Object>builder()
        .put("BREAK", Doc.BREAK)
        .put("SOFT_BREAK", Doc.SOFT_BREAK)
        .put("EMPTY", Doc.EMPTY)
        .put("TRUE", Values.TRUE)
        .put("FALSE", Values.FALSE)
        .put("COMMA_BREAK", Doc.text(",").add(Doc.BREAK))
        .build();

  private final Map<SnippetKey, Snippet> snippets;
  private final Multimap<String, Snippet> snippetsByName;
  private final ImmutableMap<String, Object> globals;
  private final Deque<Map<String, Object>> locals = Queues.newArrayDeque();

  /**
   * Construct a context with given snippets and globals.
   */
  Context(Map<SnippetKey, Snippet> snippets, Map<String, Object> globals) {
    this.snippets = snippets;
    this.snippetsByName = ArrayListMultimap.create();
    for (Map.Entry<SnippetKey, Snippet> entry : snippets.entrySet()) {
      snippetsByName.put(entry.getKey().name(), entry.getValue());
    }
    this.globals = ImmutableMap.copyOf(globals);
  }

  /**
   * Construct a context with given snippets.
   */
  Context(Map<SnippetKey, Snippet> snippets) {
    this(snippets, ImmutableMap.<String, Object>of());
  }

  /**
   * Enters a new local scope. Subsequent calls to {@link #bind(String, Object)} introduce
   * a variable in this scope.
   */
  void enterScope() {
    locals.push(Maps.<String, Object>newLinkedHashMap());
  }

  /**
   * Exists a scope, forgetting all variables defined within.
   */
  void exitScope() {
    locals.pop();
  }

  /**
   * Binds a variable in the innermost scope.
   */
  void bind(String name, Object value) {
    Preconditions.checkNotNull(value);
    locals.peek().put(name, value);
  }

  /**
   * Forks the given context into a new one with same snippets and globals, and empty locals.
   */
  Context fork() {
    return new Context(snippets, globals);
  }

  /**
   * Gets a variable definition, walking scopes inside-out.
   */
  Object getVar(String name) {
    // First try to resolve in locals in scope.
    Iterator<Map<String, Object>> iter = locals.iterator();
    while (iter.hasNext()) {
      Object result = iter.next().get(name);
      if (result != null) {
        return result;
      }
    }

    // Next try to resolve as a snippet. This only succeeds if there is a unique binding
    // for the snippet independent of its arity.
    Collection<Snippet> snippetResult = snippetsByName.get(name);
    if (snippetResult.size() == 1) {
      return snippetResult.iterator().next();
    }

    // Next try to resolve in globals in scope.
    Object result = globals.get(name);
    if (result != null) {
      return result;
    }

    // Finally look up builtin variables.
    return BUILTIN.get(name);
  }

  /**
   * Gets a snippet definitions.
   */
  @Nullable Snippet getSnippet(Location location, String name, int arity) {
    // First try to resolve as a variable which represents a snippet.
    Snippet result;
    Object varValue = getVar(name);
    if (varValue != null && varValue instanceof Snippet) {
      result = (Snippet) varValue;
      if (result.params().size() == arity) {
        return result;
      }
      result = null;
    }

    // Next try to resolve as private snippet.
    result = snippets.get(SnippetKey.create(makePrivateSnippetName(location, name), arity));
    if (result != null) {
      return result;
    }

    // Finally try to resolve as non-private snippet.
    return snippets.get(SnippetKey.create(name, arity));
  }

  /**
   * Gets the name of a private snippet, based on the input name and the snippet name proper.
   */
  static String makePrivateSnippetName(Location location, String name) {
    return name + "#" + location.baseInputName();
  }
}
