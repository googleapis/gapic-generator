package com.google.api.codegen.discovery.viewmodel;

import com.google.api.codegen.discovery.Schema;
import com.google.auto.value.AutoValue;

/**
 * Created by andrealin on 5/24/17.
 */
@AutoValue
public abstract class SchemaView {

  // The escaped class name for this Schema.
  public abstract String name();
  public abstract String type();
  public abstract Schema schema();

  public static SchemaView.Builder newBuilder() {
    return new AutoValue_SchemaView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract SchemaView.Builder id(String val);

    public abstract SchemaView.Builder name(String val);

    public abstract SchemaView.Builder schema(Schema val);

    public abstract SchemaView.Builder type(String val);

    public abstract SchemaView.Builder serializedName(String val);

    public abstract SchemaView build();
  }

//"Address": {
//    "id": "Address",
//        "type": "object",
//        "description": "A reserved address resource.",
//        "properties": {
//      "address": {
//        "type": "string",
//            "description": "The static external IP address represented by this resource. Only IPv4 is supported."
//      },
//      "creationTimestamp": {
//        "type": "string",
//            "description": "[Output Only] Creation timestamp in RFC3339 text format."
//      },
//      "description": {
//        "type": "string",
//            "description": "An optional description of this resource. Provide this property when you create the resource."
//      },
//      "id": {
//        "type": "string",
//            "description": "[Output Only] The unique identifier for the resource. This identifier is defined by the server.",
//            "format": "uint64"
//      },
//      "kind": {
//        "type": "string",
//            "description": "[Output Only] Type of the resource. Always compute#address for addresses.",
//            "default": "compute#address"
//      },
//      "name": {
//        "type": "string",
//            "description": "Name of the resource. Provided by the client when the resource is created. The name must be 1-63 characters long, and comply with RFC1035. Specifically, the name must be 1-63 characters long and match the regular expression [a-z]([-a-z0-9]*[a-z0-9])? which means the first character must be a lowercase letter, and all following characters must be a dash, lowercase letter, or digit, except the last character, which cannot be a dash.",
//            "pattern": "[a-z](?:[-a-z0-9]{0,61}[a-z0-9])?",
//            "annotations": {
//          "required": [
//          "compute.addresses.insert"
//      ]
//        }
//      },
//      "region": {
//        "type": "string",
//            "description": "[Output Only] URL of the region where the regional address resides. This field is not applicable to global addresses."
//      },
//      "selfLink": {
//        "type": "string",
//            "description": "[Output Only] Server-defined URL for the resource."
//      },
//      "status": {
//        "type": "string",
//            "description": "[Output Only] The status of the address, which can be either IN_USE or RESERVED. An address that is RESERVED is currently reserved and available to use. An IN_USE address is currently being used by another resource and is not available.",
//            "enum": [
//        "IN_USE",
//            "RESERVED"
//     ],
//        "enumDescriptions": [
//        "",
//            ""
//     ]
//      },
//      "users": {
//        "type": "array",
//            "description": "[Output Only] The URLs of the resources that are using this address.",
//            "items": {
//          "type": "string"
//        }
//      }
//    }
//  },
}
