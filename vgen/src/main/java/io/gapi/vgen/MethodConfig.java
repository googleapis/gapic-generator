package io.gapi.vgen;

import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.SimpleLocation;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

// TODO(garrettjones) consider using AutoValue in this class and related classes.
/**
 * MethodConfig represents the code-gen config for a method, and includes the
 * specification of features like page streaming and parameter flattening.
 */
public class MethodConfig {

  private final PageStreamingConfig pageStreaming;
  private final FlatteningConfig flattening;
  private final String retryCodesConfigName;
  private final String retryParamsConfigName;
  private final Iterable<Field> requiredFields;
  private final Iterable<Field> optionalFields;
  private final BundlingConfig bundling;
  private final boolean hasRequestObjectMethod;

  /**
   * Creates an instance of MethodConfig based on MethodConfigProto, linking it
   * up with the provided method. On errors, null will be returned, and
   * diagnostics are reported to the diag collector.
   */
  @Nullable
  public static MethodConfig createMethodConfig(
      DiagCollector diagCollector,
      final MethodConfigProto methodConfig,
      Method method,
      ImmutableSet<String> retryCodesConfigNames,
      ImmutableSet<String> retryParamsConfigNames) {

    boolean error = false;

    PageStreamingConfig pageStreaming;
    if (PageStreamingConfigProto.getDefaultInstance().equals(methodConfig.getPageStreaming())) {
      pageStreaming = null;
    } else {
      pageStreaming =
          PageStreamingConfig.createPageStreaming(
              diagCollector, methodConfig.getPageStreaming(), method);
      if (pageStreaming == null) {
        error = true;
      }
    }

    FlatteningConfig flattening;
    if (FlatteningConfigProto.getDefaultInstance().equals(methodConfig.getFlattening())) {
      flattening = null;
    } else {
      flattening =
          FlatteningConfig.createFlattening(diagCollector, methodConfig.getFlattening(), method);
      if (flattening == null) {
        error = true;
      }
    }

    BundlingConfig bundling;
    if (BundlingConfigProto.getDefaultInstance().equals(methodConfig.getBundling())) {
      bundling = null;
    } else {
      bundling =
          BundlingConfig.createBundling(diagCollector, methodConfig.getBundling(), method);
      if (bundling == null) {
        error = true;
      }
    }

    String retryCodesName = methodConfig.getRetryCodesName();
    if (!retryCodesName.isEmpty() && !retryCodesConfigNames.contains(retryCodesName)) {
      diagCollector.addDiag(
          Diag.error(
              SimpleLocation.TOPLEVEL,
              "Retry codes config used but not defined: '%s' (in method %s)",
              retryCodesName, method.getFullName()));
      error = true;
    }

    String retryParamsName = methodConfig.getRetryParamsName();
    if (!retryParamsConfigNames.isEmpty() && !retryParamsConfigNames.contains(retryParamsName)) {
      diagCollector.addDiag(
          Diag.error(
              SimpleLocation.TOPLEVEL,
              "Retry parameters config used but not defined: %s (in method %s)",
              retryParamsName, method.getFullName()));
      error = true;
    }

    boolean hasRequestObjectMethod = methodConfig.getRequestObjectMethod();

    List<String> requiredFieldNames = methodConfig.getRequiredFieldsList();
    ImmutableSet.Builder<Field> builder = ImmutableSet.builder();
    for (String fieldName : requiredFieldNames) {
      Field requiredField = method.getInputMessage().lookupField(fieldName);
      if (requiredField != null) {
        builder.add(requiredField);
      } else {
        Diag.error(SimpleLocation.TOPLEVEL, "Required field '%s' not found (in method %s)",
            fieldName, method.getFullName());
        error = true;
      }
    }
    Set<Field> requiredFields = builder.build();

    Iterable<Field> optionalFields = Iterables.filter(
        new ServiceMessages().flattenedFields(method.getInputType()),
        new Predicate<Field>() {
          @Override
          public boolean apply(Field input) {
            return !(methodConfig.getRequiredFieldsList().contains(input.getSimpleName()));
          }
        });

    if (error) {
      return null;
    } else {
      return new MethodConfig(pageStreaming, flattening, retryCodesName, retryParamsName, bundling,
          hasRequestObjectMethod, requiredFields, optionalFields);
    }
  }

  private MethodConfig(
      PageStreamingConfig pageStreaming,
      FlatteningConfig flattening,
      String retryCodesConfigName,
      String retryParamsConfigName,
      BundlingConfig bundling,
      boolean hasRequestObjectMethod,
      Iterable<Field> requiredFields,
      Iterable<Field> optionalFields) {
    this.pageStreaming = pageStreaming;
    this.flattening = flattening;
    this.retryCodesConfigName = retryCodesConfigName;
    this.retryParamsConfigName = retryParamsConfigName;
    this.bundling = bundling;
    this.hasRequestObjectMethod = hasRequestObjectMethod;
    this.requiredFields = requiredFields;
    this.optionalFields = optionalFields;
  }

  /**
   * Returns true if this method has page streaming configured.
   */
  public boolean isPageStreaming() {
    return pageStreaming != null;
  }

  /**
   * Returns the page streaming configuration of the method.
   */
  public PageStreamingConfig getPageStreaming() {
    return pageStreaming;
  }

  /**
   * Returns true if this method has flattening configured.
   */
  public boolean isFlattening() {
    return flattening != null;
  }

  /**
   * Returns the flattening configuration of the method.
   */
  public FlatteningConfig getFlattening() {
    return flattening;
  }

  /**
   * Returns the name of the retry codes config this method uses.
   */
  public String getRetryCodesConfigName() {
    return retryCodesConfigName;
  }

  /**
   * Returns the name of the retry params config this method uses.
   */
  public String getRetryParamsConfigName() {
    return retryParamsConfigName;
  }

  /**
   * Returns true if this method has bundling configured.
   */
  public boolean isBundling() {
    return bundling != null;
  }

  /**
   * Returns the bundling configuration of the method.
   */
  public BundlingConfig getBundling() {
    return bundling;
  }

  /**
   * Returns whether the generation of the method taking a request object
   * is turned on.
   */
  public boolean hasRequestObjectMethod() {
    return hasRequestObjectMethod;
  }

  /**
   * Returns the set of fields of the method that are always required.
   */
  public Iterable<Field> getRequiredFields() {
    return requiredFields;
  }

  /**
   * Returns the set of fields of the method that are not always required.
   */
  public Iterable<Field> getOptionalFields() {
    return optionalFields;
  }
}
