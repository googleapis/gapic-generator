package io.gapi.fx.snippet;

import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import java.util.List;

import javax.annotation.Nullable;

/**
 * Internal representation of a snippet definition.
 */
@AutoValue
abstract class Snippet {

  enum SnippetKind {
    REGULAR, ABSTRACT, OVERRIDE, PRIVATE
  }

  /**
   * The input name of the location describes the sequence of inputs from where this snippet was
   * obtained, separated by {@code File.pathSeparator}. If input A extends input B and B contains
   * this snippet, this will be {@code A;B}. The path is used to check consistency of overrides. (A
   * snippet can only override a snippet which was defined in its path. In particular, if A extends
   * both B and C, but B and C are unrelated, then a snippet from B cannot be overridden by a
   * snippet from C).
   */
  abstract Location location();

  abstract String name();
  abstract SnippetKind kind();
  @Nullable abstract Snippet overridden();
  abstract Layout layout();
  abstract ImmutableList<String> params();
  abstract ImmutableList<Elem> content();

  static Snippet create(Location location, String name, SnippetKind snippetKind,
      @Nullable Snippet overridden, Layout layout, Iterable<String> params, List<Elem> elems) {
    return new AutoValue_Snippet(location, name, snippetKind, overridden, layout,
        ImmutableList.copyOf(params), ImmutableList.copyOf(elems));
  }

  /**
   * Returns true if the snippet is bindable to an interface. That is true if it is not private,
   * or overrides a non-internal snippet.
   */
  boolean isBindable() {
    if (kind() == SnippetKind.OVERRIDE && overridden() != null) {
      return overridden().isBindable();
    }
    return kind() != SnippetKind.PRIVATE;
  }

  /**
   * Evaluates the snippet within the given context.
   */
  Doc eval(Context context, List<?> args) {
    Preconditions.checkArgument(args.size() == params().size());
    context = context.fork();
    context.enterScope();
    int i = 0;
    for (String param : params()) {
      context.bind(param, args.get(i++));
    }
    return evalElems(context, content()).group(layout().groupKind()).nest(layout().nest()).align();
  }

  /**
   * Helper to evaluate the iterable of elements into a document, composing them via
   * {@link Doc#add(Doc)}.
   */
  static Doc evalElems(Context context, Iterable<Elem> elems) {
    Doc result = Doc.EMPTY;
    for (Elem elem : elems) {
      result = result.add(Values.convertToDoc(elem.eval(context)));
    }
    return result;
  }

  /**
   * A display name for the snippet.
   */
  String displayName() {
    return String.format("%s(%s)", name(), params().size());
  }
}
