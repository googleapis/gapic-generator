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

import com.google.api.AnnotationsProto;
import com.google.api.MethodSignature;
import com.google.api.Resource;
import com.google.api.codegen.config.FlatteningConfig;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.ProtoElement;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

// Utils for parsing proto annotations.
public class ProtoAnnotations {
  public static final String RESOURCE_ANNOTATION = "google.api.resources";

  /** Return the path, e.g. "shelves/*" for a resource field. Return null if no path found. */
  public static String getResourcePath(ProtoElement element) {
    return element
        .getOptionFields()
        .entrySet()
        .stream()
        .filter(entry -> entry.getKey().getFullName().equals(RESOURCE_ANNOTATION))
        .map(entry -> ((Resource) entry.getValue()).getPath())
        .findFirst()
        .orElse(null);
  }

  /** Return the entity name, e.g. "shelf" for a resource field. */
  public static String getResourceEntityName(Field field) {
    return field.getSimpleName().toLowerCase();
  }

  /* Return a list of method signatures, aka flattenings, specified on a given method. */
  public static List<FlatteningConfig> getMethodSignatures(Method method, DiagCollector diagCollector) {
    MethodSignature methodSignature = method.getDescriptor().getMethodAnnotation(AnnotationsProto.methodSignature);
    // Let's only recurse once when we look for additional MethodSignatures.
    List<MethodSignature> additionalSignatures = methodSignature.getAdditionalSignaturesList();
    List<MethodSignature> methodSignatures = ImmutableList.<MethodSignature>builder()
        .add(methodSignature)
        .addAll(additionalSignatures).build();



    ImmutableList.Builder<List<FlatteningConfig>> signatures = ImmutableList.builder();
    // TODO(andrealin): Handle function_name.
    methodSignatures.stream().filter(s -> s.getFieldsCount() > 0).forEach(s -> signatures.add(s.getFieldsList()));
    return signatures.build();
  }
}
