package io.gapi.fx.aspects.http;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;

import io.gapi.fx.aspects.ConfigAspectBase;
import io.gapi.fx.aspects.LintRule;
import io.gapi.fx.aspects.http.model.HttpAttribute;
import io.gapi.fx.aspects.http.model.SystemParameter;
import io.gapi.fx.model.FieldSelector;
import io.gapi.fx.model.Method;

import java.util.Set;

/**
 * Style rule to verify if the names of query parameters, of a http request,
 * is not a reserved ESF keyword.
 */
class HttpParameterReservedKeywordRule extends LintRule<Method> {

  HttpParameterReservedKeywordRule(ConfigAspectBase aspect) {
    super(aspect, "param-reserved-keyword", Method.class);
  }

  @Override
  public void run(Method method) {
    if (!method.hasAttribute(HttpAttribute.KEY)) {
      return;
    }
    Set<String> visitedFieldNames = Sets.newHashSet();
    for (HttpAttribute httpAttribute : method.getAttribute(HttpAttribute.KEY).getAllBindings()) {
      for (FieldSelector fieldSelector : httpAttribute.getParamSelectors()) {
        String restParameterName = fieldSelector.getLastField().getJsonName();
        if (!visitedFieldNames.contains(restParameterName)) {
          visitedFieldNames.add(restParameterName);
          if (SystemParameter.isSystemParameter(restParameterName)) {
            warning(method, "Field name '%s' is a reserved keyword, please use a different name. "
                + "The reserved keywords are %s.",
                restParameterName,
                Joiner.on(", ").join(SystemParameter.allSystemParameters()).toLowerCase());
          }
        }
      }
    }
  }
}
