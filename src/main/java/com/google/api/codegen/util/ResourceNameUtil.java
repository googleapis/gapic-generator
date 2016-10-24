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
package com.google.api.codegen.util;

import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.config.ResourceNameTreatment;
import com.google.api.gax.protobuf.PathTemplate;
import com.google.api.tools.framework.model.Field;
import com.google.gapic.Format;
import com.google.gapic.ResourceNameFormatProto;
import java.util.List;

public class ResourceNameUtil {

  public static FieldConfig createFieldConfig(Field field) {
    if (hasResourceName(field)) {
      return FieldConfig.createFieldConfig(
          field, ResourceNameTreatment.STATIC_TYPES, getResourceName(field));
    } else {
      return FieldConfig.createDefaultFieldConfig(field);
    }
  }

  public static String getResourceName(Field field) {
    String resourceName =
        field.getProto().getOptions().getExtension(ResourceNameFormatProto.formatName);
    return Name.upperCamel(resourceName).toLowerUnderscore();
  }

  public static boolean hasResourceName(Field field) {
    if (field == null) {
      return false;
    }
    return field.getProto().getOptions().hasExtension(ResourceNameFormatProto.formatName);
  }

  public static PathTemplate getResourceNamePathTemplate(Field field) {
    String resourceName = getResourceName(field);
    List<Format> formatList =
        field.getFile().getProto().getOptions().getExtension(ResourceNameFormatProto.format);
    for (Format format : formatList) {
      if (format.getFormatName().equals(resourceName)) {
        return PathTemplate.create(format.getFormatString());
      }
    }
    throw new IllegalArgumentException(
        "Invalid proto: could not find format named "
            + resourceName
            + " expected by field "
            + field);
  }
}
