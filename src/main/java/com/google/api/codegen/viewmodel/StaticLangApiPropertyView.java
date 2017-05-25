package com.google.api.codegen.viewmodel;

import com.google.api.codegen.discovery.Schema;
import com.google.auto.value.AutoValue;
import java.util.List;

/**
 * The ViewModel for a Discovery doc property.
 */
@AutoValue
public abstract class StaticLangApiPropertyView implements ViewModel {

  public abstract Schema.Type typeName();

  // The (possibly-transformed version of the) Discovery doc key for the property.
  public abstract String fieldName();

  public abstract String description();

  public abstract String defaultValue();

  // Assume all Discovery doc enums are Strings.
  public abstract List<String> enumValues();

  public abstract boolean repeated();

  // There can be arbitrarily nested fields inside of this field.
  public abstract List<StaticLangApiPropertyView> properties();

  // A reference to another Schema.
  public abstract StaticLangApiSchemaClassView ref();

  public static StaticLangApiPropertyView.Builder newBuilder() {
    return new AutoValue_StaticLangApiPropertyView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract StaticLangApiPropertyView.Builder typeName(StaticLangApiSchemaClassView val);

    public abstract StaticLangApiPropertyView.Builder fieldName(String val);

    public abstract StaticLangApiPropertyView.Builder description(String val);

    public abstract StaticLangApiPropertyView.Builder defaultValue(String val);

    public abstract StaticLangApiPropertyView.Builder enumValues(List<String> val);

    public abstract StaticLangApiPropertyView.Builder repeated(boolean val);

    public abstract StaticLangApiPropertyView.Builder properties(List<StaticLangApiPropertyView> val);

    public abstract StaticLangApiPropertyView build();
  }


  /*
   "kind": {
     "type": "string",
     "description": "[Output Only] Type of the resource. Always compute#accessConfig for access configs.",
     "default": "compute#accessConfig"
    },
    "name": {
     "type": "string",
     "description": "Name of this access configuration."
    },
    "natIP": {
     "type": "string",
     "description": "An external IP address associated with this instance. Specify an unused static external IP address available to the project or leave this field undefined to use an IP from a shared ephemeral IP address pool. If you specify a static external IP address, it must live in the same region as the zone of the instance."
    },
    "type": {
     "type": "string",
     "description": "The type of configuration. The default and only option is ONE_TO_ONE_NAT.",
     "default": "ONE_TO_ONE_NAT",
     "enum": [
      "ONE_TO_ONE_NAT"
     ],
     "enumDescriptions": [
      ""
   */
}
