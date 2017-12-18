/* Copyright 2016 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.api.codegen.util;

import com.google.common.base.Throwables;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/** Utility class for constructing an instance of a class. */
public final class ClassInstantiator {

  private ClassInstantiator() {}

  public interface ErrorReporter {
    void error(String message, Object... args);
  }

  @SuppressWarnings("unchecked")
  public static <LP> LP createClass(
      String className,
      Class<LP> coerceType,
      Class<?>[] ctorParam,
      Object[] ctorArg,
      String classDescription,
      ErrorReporter errorReporter) {
    Class<?> classType;
    try {
      classType = Class.forName(className);
    } catch (ClassNotFoundException e) {
      errorReporter.error(
          "Cannot resolve %s class '%s'. Is it in the class path?", classDescription, className);
      return null;
    }
    if (!coerceType.isAssignableFrom(classType)) {
      errorReporter.error(
          "the %s class '%s' does not extend the expected class '%s'",
          classDescription, classType.getName(), coerceType.getName());
      return null;
    }
    Constructor<?> ctor;
    try {
      ctor = classType.getConstructor(ctorParam);
    } catch (NoSuchMethodException | SecurityException e) {
      StringBuilder error =
          new StringBuilder(
              String.format(
                  "the %s class '%s' does not have the expected constructor with parameters:",
                  classDescription, classType.getName()));
      for (Class c : ctorParam) {
        error.append(" ");
        error.append(c.getName());
      }
      errorReporter.error(error.toString());
      return null;
    }
    try {
      // Unchecked cast here. This is safe though, since we make sure that coerceType is assignable
      // from classType.
      return (LP) ctor.newInstance(ctorArg);
    } catch (InstantiationException
        | IllegalAccessException
        | IllegalArgumentException
        | InvocationTargetException e) {
      // At this point, this is likely a bug and not a user error, so propagate exception.
      throw Throwables.propagate(e);
    }
  }
}
