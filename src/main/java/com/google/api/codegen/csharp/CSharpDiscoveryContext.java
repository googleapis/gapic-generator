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
package com.google.api.codegen.csharp;

import com.google.api.Service;
import com.google.api.codegen.ApiaryConfig;
import com.google.api.codegen.DiscoveryContext;
import com.google.auto.value.AutoValue;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Field;
import com.google.protobuf.Method;
import com.google.protobuf.Type;

import java.util.ArrayList;
import java.util.List;

public class CSharpDiscoveryContext extends DiscoveryContext implements CSharpContext {

  private static final ImmutableMap<Field.Kind, String> FIELD_TYPE_MAP =
      ImmutableMap.<Field.Kind, String>builder()
          .put(Field.Kind.TYPE_UNKNOWN, "object")
          .put(Field.Kind.TYPE_BOOL, "bool")
          .put(Field.Kind.TYPE_INT32, "int")
          .put(Field.Kind.TYPE_UINT32, "uint")
          .put(Field.Kind.TYPE_INT64, "long")
          .put(Field.Kind.TYPE_UINT64, "ulong")
          .put(Field.Kind.TYPE_FLOAT, "float")
          .put(Field.Kind.TYPE_DOUBLE, "double")
          .put(Field.Kind.TYPE_STRING, "string")
          .build();

  private static final ImmutableMap<Field.Kind, String> DEFAULTVALUE_MAP =
      ImmutableMap.<Field.Kind, String>builder()
          .put(Field.Kind.TYPE_UNKNOWN, "null")
          .put(Field.Kind.TYPE_BOOL, "false")
          .put(Field.Kind.TYPE_INT32, "0")
          .put(Field.Kind.TYPE_UINT32, "0U")
          .put(Field.Kind.TYPE_INT64, "0L")
          .put(Field.Kind.TYPE_UINT64, "0UL")
          .put(Field.Kind.TYPE_FLOAT, "0.0f")
          .put(Field.Kind.TYPE_DOUBLE, "0.0")
          .put(Field.Kind.TYPE_STRING, "\"\"")
          .build();

  private CSharpContextCommon csharpCommon;

  public CSharpDiscoveryContext(Service service, ApiaryConfig apiaryConfig) {
    super(service, apiaryConfig);
  }

  @Override
  public void resetState(CSharpContextCommon csharpCommon) {
    this.csharpCommon = csharpCommon;
  }

  public String addImport(String namespace) {
    return csharpCommon.addImport(namespace);
  }

  private String packageNameAndImport(String suffix) {
    String packageName =
        CSharpContextCommon.s_underscoresToPascalCase(getApiaryConfig().getServiceCanonicalName());
    String versionName = getApiaryConfig().getServiceVersion().replace('.', '_');
    String namespace = "Google.Apis." + packageName + "." + versionName + suffix;
    addImport(namespace);
    return packageName;
  }

  private List<String> buildDescription(String raw) {
    final int lineLength = 100;
    return FluentIterable.from(Splitter.on('\n').split(raw))
        .transformAndConcat(
            new Function<String, Iterable<String>>() {
              @Override
              public Iterable<String> apply(String line) {
                List<String> lines = new ArrayList<>();
                line = line.trim();
                while (line.length() > lineLength) {
                  int i = lineLength;
                  for (; i >= 0; i--) {
                    if (Character.isWhitespace(line.charAt(i))) {
                      break;
                    }
                  }
                  if (i <= 0) {
                    // Just truncate at lineLength if it can't be split
                    i = lineLength;
                  }
                  lines.add(line.substring(0, i).trim());
                  line = line.substring(i).trim();
                }
                if (line.length() > 0) {
                  lines.add(line);
                }
                return lines;
              }
            })
        .transform(
            new Function<String, String>() {
              @Override
              public String apply(String line) {
                return "// " + line;
              }
            })
        .toList();
  }

  @AutoValue
  public abstract static class ParamInfo {
    public static ParamInfo create(
        String typeName, String name, String defaultValue, List<String> description) {
      return new AutoValue_CSharpDiscoveryContext_ParamInfo(
          typeName, name, defaultValue, description);
    }

    public abstract String typeName();

    public abstract String name();

    public abstract String defaultValue();

    public abstract List<String> description();
  }

  @AutoValue
  public abstract static class SampleInfo {
    public static SampleInfo create(
        String namespace,
        String serviceTypeName,
        String serviceVarName,
        String methodName,
        List<ParamInfo> params,
        String paramList,
        String resourcePath,
        String requestTypeName,
        String responseTypeName) {
      return new AutoValue_CSharpDiscoveryContext_SampleInfo(
          namespace,
          serviceTypeName,
          serviceVarName,
          methodName,
          params,
          paramList,
          resourcePath,
          requestTypeName,
          responseTypeName);
    }

    public abstract String namespace();

    public abstract String serviceTypeName();

    public abstract String serviceVarName();

    public abstract String methodName();

    public abstract List<ParamInfo> params();

    public abstract String paramList();

    public abstract String resourcePath();

    public abstract String requestTypeName();

    public abstract String responseTypeName();
  }

  public SampleInfo getSampleInfo(Method method) {
    String rawMethodName = method.getName();
    String packageName = packageNameAndImport("");
    String namespace = packageName + "Sample";
    String serviceTypeName = packageName + "Service";
    String serviceVarName = CSharpContextCommon.s_underscoresToCamelCase(packageName) + "Service";
    String methodName = CSharpContextCommon.s_underscoresToPascalCase(getSimpleName(rawMethodName));

    final ApiaryConfig apiary = getApiaryConfig();
    final Type methodType = apiary.getType(method.getRequestTypeUrl());
    List<ParamInfo> params =
        FluentIterable.from(getFlatMethodParams(method))
            .transform(
                new Function<String, ParamInfo>() {
                  @Override
                  public ParamInfo apply(String paramName) {
                    Field field = getField(methodType, paramName);
                    String typeName = FIELD_TYPE_MAP.get(field.getKind());
                    String name = CSharpContextCommon.s_underscoresToCamelCase(paramName);
                    String defaultValue = DEFAULTVALUE_MAP.get(field.getKind());
                    List<String> description =
                        buildDescription(apiary.getDescription(methodType.getName(), paramName));
                    return ParamInfo.create(typeName, name, defaultValue, description);
                  }
                })
            .toList();
    String paramList =
        FluentIterable.from(params)
            .transform(
                new Function<ParamInfo, String>() {
                  @Override
                  public String apply(ParamInfo paramInfo) {
                    return paramInfo.name();
                  }
                })
            .join(Joiner.on(", "));

    String resourcePath =
        FluentIterable.from(apiary.getResources(rawMethodName))
            .transform(
                new Function<String, String>() {
                  @Override
                  public String apply(String resourceName) {
                    return CSharpContextCommon.s_underscoresToPascalCase(resourceName);
                  }
                })
            .join(Joiner.on('.'));

    String requestTypeName =
        FluentIterable.from(apiary.getResources(rawMethodName))
            .transform(
                new Function<String, String>() {
                  @Override
                  public String apply(String resourceName) {
                    return CSharpContextCommon.s_underscoresToPascalCase(resourceName) + "Resource";
                  }
                })
            .append(methodName + "Request")
            .join(Joiner.on('.'));
    String responseTypeName = method.getResponseTypeUrl();
    // TODO: Use a better way of determining if the response type is void
    if (responseTypeName.equals("empty$")) {
      responseTypeName = "";
    } else {
      // Use side-effect of adding the namespace to imports
      packageNameAndImport(".Data");
    }
    return SampleInfo.create(
        namespace,
        serviceTypeName,
        serviceVarName,
        methodName,
        params,
        paramList,
        resourcePath,
        requestTypeName,
        responseTypeName);
  }
}
