// Copyright 2012 Google Inc. All Rights Reserved.

package io.gapi.fx.util;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;

import net.sf.cglib.reflect.FastMethod;

import java.lang.reflect.InvocationTargetException;

/**
 * A generic visitor class. Implements the visitor style pattern for depth-first traversal
 * of object graphs using dynamic dispatching based on method annotations.
 *
 * <p>An implementation will typically derive from this class an abstract class for a given object
 * model which defines how to visit children by providing methods annotated with
 * {@link Accepts}. A concrete visitor is then defined by deriving another class which defines
 * visitors annotated with {@link Visits} and/or {@link VisitsBefore} and {@link VisitsAfter}.
 *
 * <p>The generic {@link #visit} method first calls @VisitsBefore if present, then
 * calls @Visit if present, and finally @VisitsAfter if present. If no method annotated
 * with @Visits is defined, {@link #defaultVisit} will be called instead. This method in turn
 * calls {@link #accept} to traverse down the tree.
 *
 * <p>As a methodological guidance, you should use @VisitsBefore if you do a proper pre-order
 * traversal, @VisitsAfter if you do a proper post-order traversal, and @Visits only if
 * you do a mix (i.e. need to do some mutual dependent computation before and after
 * descending).
 *
 * <p>The method annotated with @VisitsBefore can return a boolean value, which if false
 * will prevent calls to any @Visits and @VisitsAfter methods for the current element
 * (which will prevent the automatic traversal of child elements).
 *
 * <p>More formally, the abstract pseudo code for the {@link #visit} method is:
 * <pre>
 *   void visit(x) {
 *     if (hasVisitsBefore(x) && !visitsBefore(x)) return;
 *     if (hasVisits(x))
 *       visits(x);
 *     else
 *       defaultVisit(x);
 *     if (hasVisitsAfter(x)) visitsAfter(x);
 *   }
 *   void defaultVisit(x) {
 *     accept(x);
 *   }
 * </pre>
 *
 * <p>Example:
 *
 * Suppose a simple object model for expressions:
 * <pre>
 *   abstract Expr { ... }
 *   class Add extends Expr {
 *     Expr left;
 *     Expr right;
 *   }
 *   class Value extends Expr {
 *     int value;
 *   }</pre>
 *
 * <p>We can now define a general expression visitor as follows (this base class can be used
 * for various concrete visitors on expressions):
 * <pre>
 *   abstract class ExprVisitor extends GenericVisitor&lt;Expr&gt; {
 *     ExprVisitor() { super(Expr.class) }
 *     {@literal @}Accepts void accept(Add add) {
 *       visit(add.left); visit(add.right);
 *     }
 *   }</pre>
 *
 * <p>Next we define a concrete visitor as below:
 * <pre>
 *   class LeafCounter extends ExprVisitor {
 *     int counter = 0;
 *     {@literal @}VisitsAfter void count(Value val) {
 *       counter++;
 *     }
 *   }</pre>
 *
 * <p>As another example, consider an evaluator:
 * <pre>
 *   class Evaluator extends ExprVisitor {
 *     Stack&lt;Integer&gt; stack;
 *     {@literal @}VisitsAfter void eval(Add add) {
 *       stack.push(stack.pop() + stack.pop());
 *     }
 *     {@literal @}VisitsAfter void eval(Value val) {
 *       stack.push(val.value));
 *     }
 *   }</pre>
 *
 * <p>The following funny evaluator uses the @Visits annotation instead of @VisitsAfter as it does
 * some computation before descending:
 *
 * <pre>
 *   class FunnyEvaluator extends ExprVisitor {
 *     Stack&lt;Integer&gt; stack;
 *     {@literal @}Visits void eval(Add add) {
 *       int x = someFunction(); // compute some stuff
 *       accept(add);
 *       stack.push(x + stack.pop() + stack.pop());
 *     }
 *     {@literal @}VisitsAfter void eval(Value val) {
 *       stack.push(val.value));
 *     }
 *   }</pre>
 *
 * <p>Here is a visitor which uses @VisitsBefore to replace the current visitor with another
 * visitor if some condition is met:
 *
 *  <pre>
 *   class LeafCounter extends ExprVisitor {
 *     int counter = 0;
 *     {@literal @}VisitsBefore boolean count(Expr expr) {
 *       if (someCondition(expr)) {
 *         // use a different visitor
 *         new DecreasingLeafCounter().visit(expr);
 *         return false; // stop with this visitor
 *       }
 *       return true; // continue with this visitor
 *     }
 *     {@literal @}VisitsAfter void count(Value val) {
 *       counter++;
 *     }
 *     class DecreasingLeafCounter extends ExprVisitor {
 *       {@literal @}VisitsAfter void count(Value val) {
 *         counter--;
 *       }
 *     }
 *   }</pre>
 *
 * <p>A few additional remarks:
 * <ul>
 *  <li>Methods are dispatched based on the type of their argument. If a precise match
 *      is not found, the superclass is tried, and so on, until the base type of the visitor
 *      is reached. This matches the standard behavior for method dispatching in Java.
 *  </li>
 *  <li>Methods from base types are considered for resolution.
 *  </li>
 *  <li>Methods must not be private or protected and not static to be able to be called via cglib's
 *    fast reflection support.
 *  </li>
 *  <li>Use {@code Object} as your base type if the object model doesn't have a more
 *      specific root type.
 *  </li>
 *  <li>You can customize default visitors for elements and children by overriding the
 *      methods {@link #defaultVisit(Object)} and {@link #defaultAccept(Object)}. See
 *      the javadoc of the available methods for more details.
 *   </li>
 * </ul>
 *
 * @author wgg@google.com (Wolfgang Grieskamp)
 */
public abstract class GenericVisitor<BaseType> {

  private final Class<BaseType> baseType;
  private final Dispatcher<BaseType> visits;
  private final Dispatcher<BaseType> accepts;
  private final Dispatcher<BaseType> before;
  private final Dispatcher<BaseType> after;

  /**
   * Constructs a generic visitor with {@code baseType} being the root type of the
   * object model. Pass {@code Object.class} if the object model doesn't has such a type.
   */
  protected GenericVisitor(Class<BaseType> baseType) {
    this.baseType = Preconditions.checkNotNull(baseType);
    this.visits = Dispatcher.getDispatcher(baseType, Visits.class, this.getClass());
    this.accepts = Dispatcher.getDispatcher(baseType, Accepts.class, this.getClass());
    this.before = Dispatcher.getDispatcher(baseType, VisitsBefore.class, this.getClass());
    this.after = Dispatcher.getDispatcher(baseType, VisitsAfter.class, this.getClass());
  }

  /**
   * The default method which is invoked if no explicit @Visits method is found for
   * a type. Forwards to {@link #accept(Object)}. Override this to change this
   * behavior.
   */
  protected void defaultVisit(BaseType instance) {
    accept(instance);
  }

  /**
   * The default method which is invoked if no explicit @Accepts method is found for
   * a type. Does nothing. Override this to change this behavior.
   */
  protected void defaultAccept(BaseType instance) {
  }

  /**
   * Visits a given instance. Dispatches to a @Visits method if one
   * is found, otherwise calls {@link #defaultVisit(Object)}.
   *
   * <p>If a @BeforeVisits method with matching type is defined, it
   * will be executed before @Visits. Such a method can return a boolean which,
   * if false, lets visitation stop at this point.
   *
   * <p>If a @AfterVisits method with matching type is defined, it
   * will be executed after @Visits.
   *
   */
  public final void visit(BaseType instance) {
    if (!dispatchBefore(before, instance)) {
      // stop visitation at this point
      return;
    }
    if (!dispatch(visits, instance)) {
      defaultVisit(instance);
    }
    dispatch(after, instance);
  }

  /**
   * Visits the children of a given instance. Dispatches to a @Accepts method if one
   * is found, otherwise calls {@link #defaultAccept(Object)}. Call this in your @Visits
   * method to traverse children. Depending on where the call is placed, pre-order or
   * post-order traversal can be realized.
   *
   * <p>@Accepts methods are usually provided in a base class visitor for the particular
   * data structure to be visited, from which a concrete visitor derives.
   */
  protected final void accept(BaseType instance) {
    if (!dispatch(accepts, instance)) {
      defaultAccept(instance);
    }
  }

  private boolean dispatch(Dispatcher<BaseType> dispatcher, BaseType instance) {
    FastMethod method = getMethod(dispatcher, instance);
    if (method == null) {
      return false;
    }
    try {
      method.invoke(this, new Object[]{ instance });
      return true;
    } catch (InvocationTargetException e) {
      throw Throwables.propagate(e.getCause());
    }
  }

  // Dispatching the @VisitsBefore is slightly different then the reset,
  // as we need to determine whether to continue
  private boolean dispatchBefore(Dispatcher<BaseType> dispatcher, BaseType instance) {
    FastMethod method = getMethod(dispatcher, instance);
    if (method == null) {
      return true; // non-presence of before method means continue execution
    }
    try {
      Object result = method.invoke(this, new Object[]{ instance });
      if (method.getReturnType().equals(Boolean.TYPE)) {
        return Boolean.class.cast(result); // method determines whether to continue
      }
      return true;
    } catch (InvocationTargetException e) {
      throw Throwables.propagate(e.getCause());
    }
  }

  @SuppressWarnings("unchecked") // ensure by construction
  private FastMethod getMethod(Dispatcher<BaseType> dispatcher, BaseType instance) {
    Preconditions.checkNotNull(instance, "instance");
    return dispatcher.getMethod((Class<? extends BaseType>) instance.getClass());
  }
}
