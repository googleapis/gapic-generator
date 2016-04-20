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
package io.gapi.vgen;

import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.SimpleLocation;
import com.google.api.tools.framework.model.stages.Merged;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Base class for creating code generators, by dynamic class loading of a
 * {@link LanguageProviderBase}.
 */
public final class GeneratorBuilderUtil {

  private GeneratorBuilderUtil() {}

  public interface ErrorReporter {
    void error(String message, Object... args);
  }

  public static LanguageProvider createLanguageProvider(ConfigProto configProto, Model model) {
    Preconditions.checkNotNull(model);
    Preconditions.checkNotNull(configProto);

    model.establishStage(Merged.KEY);
    if (model.getErrorCount() > 0) {
      return null;
    }

    ApiConfig apiConfig = ApiConfig.createApiConfig(model, configProto);
    if (apiConfig == null) {
      return null;
    }

    return GeneratorBuilderUtil.createLanguageProvider(
        configProto.getLanguageProvider(),
        LanguageProvider.class,
        new Class<?>[] {Model.class, ApiConfig.class},
        new Object[] {model, apiConfig},
        new GeneratorBuilderUtil.ErrorReporter() {
          @Override
          public void error(String message, Object... args) {
            model.addDiag(Diag.error(SimpleLocation.TOPLEVEL, message, args));
          }
        });
  }

  public static <LP> LP createLanguageProvider(
      String languageProviderName,
      Class<LP> coerceType,
      Class<?>[] ctorParam,
      Object[] ctorArg,
      ErrorReporter errorReporter) {
    Class<?> providerType;
    try {
      providerType = Class.forName(languageProviderName);
    } catch (ClassNotFoundException e) {
      errorReporter.error(
          "Cannot resolve provider class '%s'. Is it in the class path?", languageProviderName);
      return null;
    }
    if (!coerceType.isAssignableFrom(providerType)) {
      errorReporter.error(
          "the provider class '%s' does not extend the expected class '%s'",
          providerType.getName(),
          coerceType.getName());
      return null;
    }
    Constructor<?> ctor;
    try {
      ctor = providerType.getConstructor(ctorParam);
    } catch (NoSuchMethodException | SecurityException e) {
      StringBuilder error =
          new StringBuilder(
              String.format(
                  "the provider class '%s' does not have the expected constructor with parameters:",
                  providerType.getName()));
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
