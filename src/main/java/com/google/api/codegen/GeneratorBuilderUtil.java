/* Copyright 2016 Google Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.api.codegen;

import com.google.api.codegen.gapic.GapicProvider;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.SimpleLocation;
import com.google.api.tools.framework.model.stages.Merged;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Utility class for creating code generators, by dynamic class loading of a codegen provider.
 */
public final class GeneratorBuilderUtil {

  private GeneratorBuilderUtil() {}

  public interface ErrorReporter {
    void error(String message, Object... args);
  }

  /**
   * Constructs a codegen provider from a configuration; the configuration cannot contain
   * parameterized type information, so we must suppress the type safety warning here.
   */
  @SuppressWarnings("unchecked")
  public static GapicProvider<Object> createCodegenProvider(
      ConfigProto configProto,
      TemplateProto template,
      final Model model,
      InputElementView<Object> view) {
    Preconditions.checkNotNull(model);
    Preconditions.checkNotNull(configProto);

    model.establishStage(Merged.KEY);
    if (model.getErrorCount() > 0) {
      for (Diag diag : model.getDiags()) {
        System.err.println(diag.toString());
      }
      return null;
    }

    ApiConfig apiConfig = ApiConfig.createApiConfig(model, configProto);
    if (apiConfig == null) {
      return null;
    }

    return GeneratorBuilderUtil.createClass(
        template.getCodegenProvider(),
        GapicProvider.class,
        new Class<?>[] {Model.class, ApiConfig.class, InputElementView.class},
        new Object[] {model, apiConfig, view},
        "codegen provider",
        new GeneratorBuilderUtil.ErrorReporter() {
          @Override
          public void error(String message, Object... args) {
            model.addDiag(Diag.error(SimpleLocation.TOPLEVEL, message, args));
          }
        });
  }

  /**
   * Constructs an InputElementView from a configuration; the configuration cannot contain
   * parameterized type information, so we must suppress the type safety warning here.
   */
  @SuppressWarnings("unchecked")
  public static InputElementView<Object> createView(
      TemplateProto template, final DiagCollector diagCollector) {
    return GeneratorBuilderUtil.createClass(
        template.getInputElementView(),
        InputElementView.class,
        new Class<?>[] {},
        new Object[] {},
        "input element view",
        new GeneratorBuilderUtil.ErrorReporter() {
          @Override
          public void error(String message, Object... args) {
            diagCollector.addDiag(Diag.error(SimpleLocation.TOPLEVEL, message, args));
          }
        });
  }

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
          classDescription,
          classType.getName(),
          coerceType.getName());
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
                  classDescription,
                  classType.getName()));
      for (Class c : ctorParam) {
        error.append(" ");
        error.append(c.getName());
      }
      errorReporter.error(error.toString());
      return null;
    }
    try {
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
