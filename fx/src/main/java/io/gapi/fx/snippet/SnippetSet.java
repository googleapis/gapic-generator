package io.gapi.fx.snippet;

import static java.nio.charset.StandardCharsets.UTF_8;

import io.gapi.fx.snippet.Snippet.SnippetKind;
import com.google.auto.value.AutoValue;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import com.google.common.primitives.Primitives;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * Represents a snippet set.
 *
 * <p><p> Snippets are a templating system tailored for code generation. In contrast to other
 * templating approaches, snippets produce structured documents based on the principles of a
 * Wadler-Lindig pretty printer (see {@link Doc}). They are thus better suited for producing source
 * code with spacing and indentation requirements. The decoupling of layout of the snippet code from
 * the generated source also allows for producing better readable snippet definitions. Furthermore,
 * snippets support smooth interop with Java, and a rich set of control structures.
 *
 * <p><p>A snippet is defined by a declaration as follows:
 *
 * <p><pre>
 *   {@literal @}snippet add(x,y)
 *     {@x} + {@y}
 *   {@literal @}end
 * </pre>
 *
 * <p>Evaluating {@code add(1,2)} will produce {@code 1 + 2}. More precisely, it will produce
 * {@code Doc.text("1").add(Doc.text(" + ")).add(Doc.text("2"))}.
 *
 * <p><p>Unless specified otherwise, all elements in a snippet are joined within a vertical group
 * with nesting of 0 and the separator {@link Doc#BREAK} for a line break. This can be overridden in
 * the snippet header, for example:
 *
 * <p><pre>
 *   {@literal @}snippet add(x,y) auto 4
 *     {@x}
 *     + {@y}
 *   {@literal @}end
 * </pre>
 *
 * <p>The layout specification following the parameter list of a snippet has the following general
 * syntax:
 *
 * <p><pre>
 *   [ fill | vertical | horizontal | auto ] [ NUMBER ] [ on EXPR ]
 * </pre>
 *
 * <p>Fill, vertical, horizontal, and auto describe the grouping mode, NUMBER the indentation
 * inserted after the first break, and EXPR the separator to use (expressions are described below).
 * The default is {@code vertical 0 BREAK}. A snippet output group is wrapped with
 * {@link Doc#align()}. See the documentation of {@link Doc} for details, or the papers cited there.
 *
 * <p><p>Indentation of subsequent lines in a snippet definition will be trimmed based on the
 * indentation of the first line. Therefore, nesting for control structures does not introduce
 * indentation. However, in the subsequent definition the inner block will be indented as expected:
 *
 * <p><pre>
 *   {@literal @}snippet block(stm)
 *     {
 *       {@stm}
 *     }
 *   {@literal @}end
 * </pre>
 *
 * <p>Lines in the snippet source can be joined to avoid overflow. Such joined lines appear as one
 * logical line to the snippet engine. For example:
 *
 * <p><pre>
 *   {@literal @}snippet add(x,y) auto 4
 *     {@x} \
 *       + {@y}
 *   {@literal @}end
 * </pre>
 *
 * Leading space from a line continuation is trimmed, however, any space before the
 * {@literal \} is not. In order to escape a backslash, use {@literal @\}.
 *
 *
 * <p><p>A snippet expression is a text enclosed as {@expr}. Expressions evaluate to objects and are
 * finally converted to strings before inserted into the snippet output. The following expression
 * forms are supported:
 *
 * <ul>
 *
 * <li>{@code literal}: an integer or a {@code "}-quoted string. A string denotes a value of type
 * {@link Doc}, and Doc methods can be called on it.
 *
 * <li>{@literal @@}: evaluates to {@literal @}.
 *
 * <li>{@code var}: a variable reference. Variables are introduced by parameters, iterators, and
 * globals which are passed into a snippet set at construction time. The predefined variables
 * {@code BREAK}, {@code SOFT_BREAK}, {@code EMPTY} correspond to the according constants in
 * {@link Doc}. {@code TRUE} and {@code FALSE} denote boolean values.
 *
 * <li>{@code name(expr1,...,exprN)}: a call to another snippet.
 *
 * <li>{@code expr.name}: selects the value of a public field in the Java object denoted by
 * {@code expr}, or calls a zero-parameter method of this name.
 *
 * <li>{@code expr.name(expr1,...,exprN)}: calls a public method on the Java object denoted by
 * {@code expr}.
 *
 * <li>{@code expr1 R expr2}, where {@code R} is any of {@code ==}, {@code !=}, {@code &lt;},
 * {@code &lt;=}, {@code >}, {@code >=}. Calls the comparison relation on the operands, resulting in
 * either the constant TRUE or FALSE. For comparison, a {@link Doc} or enum value will be first
 * converted to a string. After that, operands are either compared with their equals method or,
 * for metric relations, must have the same type and implement the {@link Comparable} interface.
 *
 * </ul>
 *
 * <p><p>Values passed and returned to Java methods are converted on the fly to strings, numbers,
 * and enum values as demanded by the context type. When a method is applied on an iterable and the
 * name cannot be resolved, an attempt is made to wrap the iterable in a {@link FluentIterable},
 * making methods like {@code append}, {@code first}, etc. available.
 *
 * <p><p>A few control structures are supported by a snippet. A conditional is written as follows,
 * where the else-part is optional, and the then-part is taken if the condition expression evaluates
 * to a value depending on its type: {@code true} for boolean, a non-zero value for numbers,
 * a non-empty value for strings, a document which contains more than whitespace, or an iterable
 * which contains at least one element:
 *
 * <p><pre>
 *   {@literal @}if expr
 *   ...
 *   {@literal @}else
 *   ...
 *   {@literal @}end
 * </pre>
 *
 * <p><p>A join is written as follows, where {@code expr} must evaluate to an iterable, and the
 * optional layout specifies how the elements in the body are joined (layout has the same syntax as
 * with snippet definitions):
 *
 * <p><pre>
 *   {@literal @}join var : expr [ layout ]
 *   ...
 *   {@literal @}end
 * </pre>
 *
 * <p><p>A let is written as follows, where {@code var} is bound over the scope of the let:
 *
 * <p><pre>
 *   {@literal @}let var1 = expr1, var2 = expr2, ...
 *   ...
 *   {@literal @}end
 * </pre>
 *
 * <p><p>Finally, a switch is written as such, where the default-part is optional:
 *
 * <p><pre>
 *   {@literal @}switch expr
 *   {@literal @}case expr
 *   ...
 *   {@literal @}case expr
 *   ...
 *   {@literal @}default
 *   ...
 *   {@literal @}end
 * </pre>
 *
 *
 * <p><p>Snippet sources can extend other snippet sources using the following syntax:
 *
 * <p><pre>
 *   {@literal @}extends "some/file.snip"
 * </pre>
 *
 * <p>All extends-clauses must be at the beginning of a snippet source.
 *
 * <p>A snippet can be defined private to a file, which is good practice to specify
 * the interface between different files in a snippet set:
 *
 * <p><pre>
 *   {@literal @}private add(x)
 *     ...
 *   {@literal @}end
 * </pre>
 *
 * <p>Definitions coming from extended sources can be overridden if the override-marker is used:
 *
 * <p><pre>
 *   {@literal @}override add(x)
 *     ...
 *   {@literal @}end
 * </pre>
 *
 * <p>It is not possible to override definitions in the same source. Also, a snippet can only be
 * overridden if it is in the direct extension path. For example, if sources A and B are extended by
 * C, and B attempts to override a definition in A (without extending it explicitly), an error will
 * be produced.
 *
 * <p><p>A snippet can be declared abstract:
 *
 * <p><pre>
 *   {@literal @}abstract add(x)
 * </pre>
 *
 * <p>An abstract snippet has no body. Attempting to evaluate an abstract snippet results in a
 * runtime error.
 *
 *
 * <p><p>The {@link #bind(Class, Map)} method allows to bind an interface to a snippet set,
 * implementing this interface via the methods in the set. This allows for a typed access to the
 * snippet definitions from Java. The snippet engine attempts to convert to and from types used in
 * the interface, using standard string-based conversion methods if needed.
 *
 * <p><p>A runtime error is raised if a method in an interface cannot be bound to any snippet. It is
 * allowed to bind an abstract snippet, however. An attempt to evaluate it will result in a runtime
 * error. Hence, abstract snippet are a way to express that certain functionality stays
 * unimplemented in a snippet set.
 *
 * <p><p>It is good practice to declare a snippet which is not intended to be bound to an interface
 * and not used by other snippet files as private:
 *
 * <p><pre>
 *   {@literal @}private add(x)
 *     ...
 *   {@literal @}end
 * </pre>
 *
 * <p>This snippet will be only available for calls from the same snippet file. Its name
 * is unique to the file and cannot clash with similar named snippets from other files.
 */
public class SnippetSet {

  /**
   * An interface supplying source input for the {@literal @}extends command.
   */
  public interface InputSupplier {

    /**
     * Resolve the given snippet set name into a sequence of source lines.
     */
    @Nullable
    Iterable<String> readInput(String snippetSetName) throws IOException;
  }

  /**
   * Represents an issue (error) with either snippet parsing or runtime evaluation.
   */
  @AutoValue
  public abstract static class Issue {

    /**
     * The location of the issue.
     */
    public abstract Location location();

    /**
     * The message describing the issue.
     */
    public abstract String message();

    static Issue create(Location location, String message, Object...args) {
      return new AutoValue_SnippetSet_Issue(location, String.format(message, args));
    }

    @Override public String toString() {
      return String.format("%s:%s: %s",  location().baseInputName(), location().lineNo(),
          message());
    }
  }

  /**
   * Represents a parsing error, with a collection of issues.
   */
  public static class ParseException extends Exception {
    private final ImmutableList<Issue> issues;

    ParseException(Iterable<Issue> issues) {
      super(String.format("snippet parsing error(s):%n%s",
          Joiner.on(String.format("%n")).join(issues)));
      this.issues = ImmutableList.copyOf(issues);
    }

    /**
     * The issues associated with this exception.
     */
    public List<Issue> getIssues() {
      return issues;
    }
  }

  /**
   * Represents an evaluation error, with an issue describing it.
   */
  public static class EvalException extends RuntimeException {
    private final Issue issue;

    EvalException(Issue issue) {
      this(issue, null);
    }

    EvalException(Location location, String message, Object... args) {
      this(Issue.create(location, message, args), null);
    }

    EvalException(Issue issue, Throwable cause) {
      super("snippet evaluation error: " + issue.toString(), cause);
      this.issue = issue;
    }

    /**
     * Returns the issue describing this error.
     */
    public Issue getIssue() {
      return issue;
    }
  }

  /**
   * Represents a key to identify a snippet by its name and number of parameters.
   */
  @AutoValue
  abstract static class SnippetKey {
    abstract String name();
    abstract int arity();

    static SnippetKey create(String name, int arity) {
      return new AutoValue_SnippetSet_SnippetKey(name, arity);
    }
  }

  /**
   * Parses the input and returns a snippet set.
   */
  public static SnippetSet parse(InputSupplier supplier, String inputName) throws ParseException {
    SnippetParser parser = new SnippetParser(supplier, inputName);
    parser.parse();
    if (!parser.errors().isEmpty()) {
      throw new ParseException(parser.errors());
    }
    return parser.result();
  }

  /**
   * Returns an input supplier which works on the given root file. All input names are interpreted
   * relative to this directory, unless absolute.
   */
  public static InputSupplier fileInputSupplier(final File root) {
    return new InputSupplier() {
      @Override public Iterable<String> readInput(String snippetSetName) throws IOException {
        File snippetFile = new File(snippetSetName);
        if (!snippetFile.isAbsolute()) {
          snippetFile = new File(root, snippetSetName);
        }
        return Files.readLines(snippetFile, UTF_8);
      }
    };
  }

  /**
   * Returns an input supplier which works on the given resource root path. All input names are
   * resolved as resource paths relative to this root, and read from the class path.
   */
  public static InputSupplier resourceInputSupplier(final String root) {
    return new InputSupplier() {
      @Override public Iterable<String> readInput(String snippetSetName) throws IOException {
        try {
          return Resources.readLines(Resources.getResource(root + File.separator + snippetSetName),
              UTF_8);
        } catch (Exception e) {
          throw new IOException(e);
        }
      }
    };
  }

  /**
   * Parses the input from a file and returns a snippet set.
   */
  public static SnippetSet parse(final File file) throws ParseException {
    return parse(fileInputSupplier(file.getParentFile()), file.getName());
  }

  /**
   * Returns the given interface type bound to an instance of {@link SnippetSet} parsed from the
   * given snippet resource file.
   */
  public static <T> T createSnippetInterface(Class<T> interfaceType,
      String snippetResourceRoot, String snippetResource) {
    return createSnippetInterface(interfaceType, snippetResourceRoot, snippetResource, null);
  }

  /**
   * Returns the given interface type bound to an instance of {@link SnippetSet} parsed from the
   * given snippet resource file. The passed map is used when binding the snippet set,
   * providing the globals for snippet execution.
   */
  public static <T> T createSnippetInterface(Class<T> interfaceType,
      String snippetResourceRoot, String snippetResource,
      @Nullable Map<String, Object> globals) {
    Preconditions.checkNotNull(interfaceType);
    Preconditions.checkNotNull(snippetResourceRoot);
    Preconditions.checkNotNull(snippetResource);

    try {
      SnippetSet snippets = SnippetSet.parse(SnippetSet.resourceInputSupplier(snippetResourceRoot),
          snippetResource);
      return globals != null ? snippets.bind(interfaceType, globals) : snippets.bind(interfaceType);
    } catch (ParseException e) {
      throw Throwables.propagate(e);
    }
  }

  /**
   * Called by the parser.
   */
  SnippetSet() {}

  private final Map<SnippetKey, Snippet> definitions = Maps.newLinkedHashMap();

  /**
   * Binds the snippet set to the given interface. For all methods in the interface,
   * a snippet of matching name must exist. The execution of methods on the returned
   * proxy object may throw the runtime exception {@link EvalException}.
   *
   * @throws IllegalArgumentException if not all methods are bound.
   */
  public <T> T bind(Class<T> interfaceType) {
    return bind(interfaceType, ImmutableMap.<String, Object>of());
  }

  /**
   * Like {@link #bind(Class)}, but allows to provide a map which defines
   * global variables accessible by the snippets.
   *
   * @throws IllegalArgumentException if not all methods are bound.
   */
  public <T> T bind(Class<T> interfaceType, Map<String, Object> context) {
    Preconditions.checkArgument(interfaceType.isInterface(),
        "%s is not a interface", interfaceType);
    return interfaceType.cast(Proxy.newProxyInstance(interfaceType.getClassLoader(),
        new Class<?>[] { interfaceType },
        new ProxyHandler(interfaceType, context, this)));
  }

  private static class ProxyHandler implements InvocationHandler {

    private final Class<?> interfaceType;
    private final Map<Method, Snippet> bindings = Maps.newLinkedHashMap();
    private final Context context;

    private ProxyHandler(Class<?> interfaceType, Map<String, Object> globals, SnippetSet snippets) {
      this.interfaceType = interfaceType;
      List<String> errors = Lists.newArrayList();
      for (Method method : interfaceType.getMethods()) {
        SnippetKey key = SnippetKey.create(method.getName(), method.getParameterTypes().length);
        Snippet snippet = snippets.definitions.get(key);
        if (snippet != null) {
          if (!snippet.isBindable()) {
            errors.add(String.format("attempt to bind private snippet '%s' against '%s'",
                snippet.displayName(), method.toString()));
          }
          bindings.put(method, snippet);
        } else {
          errors.add(String.format("no snippet '%s(%s)' found for method '%s'", key.name(),
              key.arity(), method.toString()));
        }
      }
      if (!errors.isEmpty()) {
        throw new IllegalArgumentException(String.format("Errors binding interface '%s':%n%s",
            interfaceType, Joiner.on(String.format("%n")).join(errors)));
      }
      this.context = new Context(snippets.definitions, globals);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
     return tryEval(Location.create("program (via " + interfaceType.getSimpleName() + ")", 1),
         Primitives.wrap(method.getReturnType()),
         bindings.get(method), context,
         args == null ? ImmutableList.of() : ImmutableList.copyOf(args));
    }
  }

  /**
   * Returns a context for snippet evaluation which binds all snippets in this set and
   * the given globals.
   */
  public Context baseContext(Map<String, Object> globals) {
    return new Context(definitions, globals);
  }

  /**
   * Returns a context for snippet evaluation which binds all snippets in this set.
   */
  public Context baseContext() {
    return new Context(definitions);
  }

  /**
   * Evaluates the named snippet with given context and requested result type. Returns
   * a value of the requested type.
   *
   * @throws EvalException
   */
  public <T> T eval(Class<T> requestedType, String name, Context context, List<Object> args) {
    Snippet snippet = definitions.get(SnippetKey.create(name,  args.size()));
    if (snippet == null) {
      throw new EvalException(Location.create("program (via eval)", 1),
          "snippet '%s(%s)' undefined", name, args.size());
    }
    return tryEval(Location.create("program", 1), requestedType, snippet, context, args);
  }

  /**
   * Shortcut for evaluating a snippet with the {@link #baseContext()} into a {@link Doc}.
   *
   * @throws EvalException
   */
  public Doc eval(String name, Object... args) {
    return eval(Doc.class, name, baseContext(), ImmutableList.copyOf(args));
  }

  /**
   * Helper for snippet evaluation.
   *
   * @throws EvalException
   */
  static <T> T tryEval(Location location, Class<T> requestedType,
      Snippet snippet, Context context, List<?> args) {
    if (snippet.kind() == SnippetKind.ABSTRACT) {
      throw new EvalException(location, "attempt to evaluate abstract snippet '%s'",
          snippet.displayName());
    }

    Doc result = snippet.eval(context, args);
    return requestedType.cast(Values.convert(location, requestedType, result));
  }

  /**
   * Adds the snippet to the set.
   */
  void add(Snippet def) {
    definitions.put(SnippetKey.create(def.name(), def.params().size()), def);
  }

  /**
   * Returns the current definition of the snippet in the set.
   */
  @Nullable Snippet get(String name, int size) {
    return definitions.get(SnippetKey.create(name, size));
  }
}
