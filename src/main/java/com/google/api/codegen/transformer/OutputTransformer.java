/* Copyright 2018 Google LLC
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
package com.google.api.codegen.transformer;

import com.google.api.codegen.Accessor;
import com.google.api.codegen.OutputSpec;
import com.google.api.codegen.config.FieldModel;
import com.google.api.codegen.config.MethodModel;
import com.google.api.codegen.config.TypeModel;
import com.google.common.base.Preconditions;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

class OutputTransformer {
  private static final String RESPONSE_PLACEHOLDER = "$resp";

  static List<OutputSpec> defaultOutputSpecs(MethodModel method) {
    if (method.isOutputTypeEmpty()) {
      return Collections.emptyList();
    }
    return Collections.singletonList(
        OutputSpec.newBuilder()
            .setPrint(OutputSpec.PrintStmt.newBuilder().setSpec("%s").addArgs(RESPONSE_PLACEHOLDER))
            .build());
  }

  static OutputSpec toLanguage(OutputSpec config, MethodContext context) {
    OutputSpec.Builder lang = OutputSpec.newBuilder();
    switch (config.getStmtCase()) {
      case LOOP:
        throw new UnsupportedOperationException("loop not implemented yet");
      case PRINT:
        lang.setPrint(toLanguage(config.getPrint(), context));
        break;
    }
    return lang.build();
  }

  private static OutputSpec.PrintStmt toLanguage(
      OutputSpec.PrintStmt config, MethodContext context) {
    return OutputSpec.PrintStmt.newBuilder()
        .setSpec(context.getNamer().getPrintSpec(config.getSpec()))
        .addAllArgsElems(
            config
                .getArgsList()
                .stream()
                .map(a -> accessor(a, context))
                .collect(Collectors.toList()))
        .build();
  }

  private static Accessor accessor(String config, MethodContext context) {
    String[] configElems = config.split("\\.");
    if (configElems.length == 0) {
      return Accessor.getDefaultInstance();
    }

    Accessor.Builder accessor = Accessor.newBuilder();
    TypeModel type;
    if (configElems[0].equals(RESPONSE_PLACEHOLDER)) {
      accessor.setVariable(context.getNamer().getSampleResponseVarName());
      type = context.getMethodModel().getOutputType();
    } else {
      throw new UnsupportedOperationException("local variable not implemented yet");
    }

    for (int i = 1; i < configElems.length; i++) {
      String fieldName = configElems[i];
      FieldModel field =
          Preconditions.checkNotNull(
              type.getField(configElems[i]), "type %s does not have field %s", type, fieldName);
      Preconditions.checkArgument(
          !field.isRepeated() && !field.isMap(), "%s.%s is not scalar", type, fieldName);
      type = field.getType();
      accessor.addFields(context.getNamer().getFieldGetFunctionName(field));
    }
    return accessor.build();
  }
}
