package io.gapi.vgen;

import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.FieldSelector;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.SimpleLocation;
import com.google.common.collect.ImmutableList;

import javax.annotation.Nullable;

/**
 * BundlingConfig represents the bundling configuration for a method.
 */
public class BundlingConfig {
  private final int elementCountThreshold;
  private final long requestByteThreshold;
  private final long delayThresholdMillis;
  private final Field bundledField;
  private final ImmutableList<FieldSelector> discriminatorFields;
  private final Field subresponseField;

  /**
   * Creates an instance of BundlingConfig based on BundlingConfigProto, linking it
   * up with the provided method. On errors, null will be returned, and diagnostics
   * are reported to the diag collector.

   */
  @Nullable
  public static BundlingConfig createBundling(DiagCollector diagCollector,
      BundlingConfigProto bundlingConfig, Method method) {

    String bundledFieldName = bundlingConfig.getBundleDescriptor().getBundledField();
    Field bundledField =
        method.getInputType().getMessageType().lookupField(bundledFieldName);
    if (bundledField == null) {
      diagCollector.addDiag(Diag.error(SimpleLocation.TOPLEVEL,
          "Bundled field missing for bundle config: method = %s, message type = %s, field = %s",
          method.getFullName(), method.getInputType().getMessageType().getFullName(),
          bundledFieldName));
    }

    ImmutableList.Builder<FieldSelector> discriminatorsBuilder = ImmutableList.builder();
    for (String discriminatorName : bundlingConfig.getBundleDescriptor()
        .getDiscriminatorFieldsList()) {
      FieldSelector selector =
          FieldSelector.resolve(method.getInputType().getMessageType(), discriminatorName);
      if (selector == null) {
        diagCollector.addDiag(Diag.error(SimpleLocation.TOPLEVEL,
            "Discriminator field missing for bundle config: method = %s, message type = %s, "
                + "field = %s", method.getFullName(),
                method.getInputType().getMessageType().getFullName(), discriminatorName));
      }
      discriminatorsBuilder.add(selector);
    }

    String subresponseFieldName = bundlingConfig.getBundleDescriptor().getSubresponseField();
    Field subresponseField;
    if (subresponseFieldName.isEmpty()) {
      subresponseField = method.getInputType().getMessageType().lookupField(subresponseFieldName);
    } else {
      subresponseField = null;
    }

    int elementCountThreshold = bundlingConfig.getThresholds().getElementCountThreshold();
    long requestByteThreshold = bundlingConfig.getThresholds().getRequestByteThreshold();
    long delayThresholdMillis = bundlingConfig.getThresholds().getDelayThresholdMillis();

    if (bundledFieldName == null) {
      return null;
    }

    if (elementCountThreshold == 0 && requestByteThreshold == 0 && delayThresholdMillis == 0) {
      diagCollector.addDiag(Diag.error(SimpleLocation.TOPLEVEL,
          "No threshold was specified as a positive number: method = %s, message type = %s",
          method.getFullName(), method.getInputType().getMessageType().getFullName()));
    }

    return new BundlingConfig(elementCountThreshold, requestByteThreshold, delayThresholdMillis,
        bundledField, discriminatorsBuilder.build(), subresponseField);
  }

  private BundlingConfig(int elementCountThreshold, long requestByteThreshold,
      long delayThresholdMillis, Field bundledField,
      ImmutableList<FieldSelector> discriminatorFields,
      Field subresponseField) {
    this.elementCountThreshold = elementCountThreshold;
    this.requestByteThreshold = requestByteThreshold;
    this.delayThresholdMillis = delayThresholdMillis;
    this.bundledField = bundledField;
    this.discriminatorFields = discriminatorFields;
    this.subresponseField = subresponseField;
  }

  public int getElementCountThreshold() {
    return elementCountThreshold;
  }

  public long getRequestByteThreshold() {
    return requestByteThreshold;
  }

  public long getDelayThresholdMillis() {
    return delayThresholdMillis;
  }

  public Field getBundledField() {
    return bundledField;
  }

  public ImmutableList<FieldSelector> getDiscriminatorFields() {
    return discriminatorFields;
  }

  public boolean hasSubresponseField() {
    return subresponseField != null;
  }

  @Nullable
  public Field getSubresponseField() {
    return subresponseField;
  }
}

