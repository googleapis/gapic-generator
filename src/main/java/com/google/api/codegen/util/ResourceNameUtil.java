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

import com.google.api.tools.framework.model.Field;
import com.google.common.base.Ascii;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;

/**
 * NamePath represents a fully-qualified name, separated by something like
 * dots or slashes.
 */
public class ResourceNameUtil {

  public static String getResourceName(Field field) {
    // TODO(michaelbausor): do this in a less horrific way
    return field
        .getProto()
        .getOptions()
        .getUnknownFields()
        .getField(50000)
        .getLengthDelimitedList()
        .get(0)
        .toStringUtf8();
  }

  public static boolean hasResourceName(Field field) {
    // TODO(michaelbausor): do this check in a less horrific way
    return field
            .getProto()
            .getOptions()
            .getUnknownFields()
            .getField(50000)
            .getLengthDelimitedList()
            .size()
        > 0;
  }
}
