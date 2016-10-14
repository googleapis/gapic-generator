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
import com.google.api.codegen.DiscoveryImporter;
import com.google.auto.value.AutoValue;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Api;
import com.google.protobuf.Field;
import com.google.protobuf.Field.Kind;
import com.google.protobuf.Method;
import com.google.protobuf.Type;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;

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
          .build();

  private static final ImmutableSet<String> RESERVED_WORDS =
      ImmutableSet.<String>builder()
          .add("abstract")
          .add("as")
          .add("base")
          .add("bool")
          .add("break")
          .add("by")
          .add("byte")
          .add("case")
          .add("catch")
          .add("char")
          .add("checked")
          .add("class")
          .add("const")
          .add("continue")
          .add("decimal")
          .add("default")
          .add("delegate")
          .add("descending")
          .add("do")
          .add("double")
          .add("else")
          .add("enum")
          .add("event")
          .add("explicit")
          .add("extern")
          .add("finally")
          .add("fixed")
          .add("float")
          .add("for")
          .add("foreach")
          .add("from")
          .add("goto")
          .add("group")
          .add("if")
          .add("implicit")
          .add("in")
          .add("int")
          .add("interface")
          .add("internal")
          .add("into")
          .add("is")
          .add("lock")
          .add("long")
          .add("namespace")
          .add("new")
          .add("null")
          .add("object")
          .add("operator")
          .add("orderby")
          .add("out")
          .add("override")
          .add("params")
          .add("private")
          .add("public")
          .add("readonly")
          .add("ref")
          .add("return")
          .add("sbyte")
          .add("sealed")
          .add("select")
          .add("short")
          .add("sizeof")
          .add("stackalloc")
          .add("static")
          .add("string")
          .add("struct")
          .add("switch")
          .add("this")
          .add("throw")
          .add("try")
          .add("typeof")
          .add("unit")
          .add("ulong")
          .add("unchecked")
          .add("unsafe")
          .add("ushort")
          .add("using")
          .add("var")
          .add("virtual")
          .add("void")
          .add("volatile")
          .add("where")
          .add("while")
          .add("yield")
          .add("FALSE")
          .add("TRUE")
          .build();

  private static final String REQUEST_FIELD_NAME = "requestBody";

  private CSharpContextCommon csharpCommon;
  private String serviceNamespace;

  public CSharpDiscoveryContext(Service service, ApiaryConfig apiaryConfig) {
    super(service, apiaryConfig);
    serviceNamespace =
        "Google.Apis."
            + CSharpContextCommon.s_underscoresToPascalCase(apiaryConfig.getServiceCanonicalName())
            + "."
            + apiaryConfig.getServiceVersion().replace('.', '_');
  }

  @Override
  public void resetState(CSharpContextCommon csharpCommon) {
    this.csharpCommon = csharpCommon;
  }

  public String addImport(String namespace) {
    return csharpCommon.addImport(namespace);
  }

  // Put any using aliases at the end of other usings, with a line gap.
  public Iterable<String> sortUsings(Iterable<String> usings) {
    Predicate<String> isAlias =
        new Predicate<String>() {
          @Override
          public boolean apply(String using) {
            return using.contains("=");
          }
        };
    FluentIterable<String> fluentUsings =
        FluentIterable.from(usings)
            .transform(
                new Function<String, String>() {
                  @Override
                  public String apply(String using) {
                    return "using " + using + ";";
                  }
                });
    Iterable<String> aliases =
        fluentUsings.anyMatch(isAlias)
            ? FluentIterable.of(new String[] {""}).append(fluentUsings.filter(isAlias))
            : Collections.<String>emptyList();
    return fluentUsings.filter(Predicates.<String>not(isAlias)).append(aliases);
  }

  public String using(String fullTypeName) {
    if (fullTypeName.startsWith(serviceNamespace)) {
      // Special handling of nested types within the service class, except .Data classes.
      // Return the full nested name to place in generated source code.
      if (fullTypeName.startsWith(serviceNamespace + ".Data")) {
        // Add an alias for Data, required due to class name clashes in some libraries.
        addImport("Data = " + serviceNamespace + ".Data");
      }
      addImport(serviceNamespace);
      return fullTypeName.substring(serviceNamespace.length() + 1);
    }
    // Remove any generic information, if present.
    int genericIndex = fullTypeName.indexOf('<');
    String nonGenericTypeName =
        genericIndex >= 0 ? fullTypeName.substring(0, genericIndex) : fullTypeName;
    int typeIndex = nonGenericTypeName.lastIndexOf('.');
    if (typeIndex < 0) {
      throw new IllegalArgumentException("fullTypeName must be fully specified.");
    }
    addImport(nonGenericTypeName.substring(0, typeIndex));
    return fullTypeName.substring(typeIndex + 1);
  }

  private String usingData(String dataRelativeTypeName) {
    return "empty$".equals(dataRelativeTypeName)
        ? "void"
        : using(serviceNamespace + ".Data." + dataRelativeTypeName);
  }

  @AutoValue
  public abstract static class ParamInfo {
    public static ParamInfo create(
        String typeName, String name, String defaultValue, String description, String sample) {
      return new AutoValue_CSharpDiscoveryContext_ParamInfo(
          typeName, name, defaultValue, description, sample);
    }

    public abstract String typeName();

    public abstract String name();

    public abstract String defaultValue();

    public abstract String description();

    public abstract String sample();
  }

  @AutoValue
  public abstract static class PageStreamingInfo {
    public static PageStreamingInfo create(String resourceFieldName, String resourceTypeName) {
      return new AutoValue_CSharpDiscoveryContext_PageStreamingInfo(
          resourceFieldName, resourceTypeName);
    }

    public abstract String resourceFieldName();

    public abstract String resourceTypeName();
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
        String responseTypeName,
        boolean hasRequestField,
        String requestFieldTypeName,
        String requestFieldName,
        boolean isPageStreaming,
        PageStreamingInfo pageStreamingInfo) {
      return new AutoValue_CSharpDiscoveryContext_SampleInfo(
          namespace,
          serviceTypeName,
          serviceVarName,
          methodName,
          params,
          paramList,
          resourcePath,
          requestTypeName,
          responseTypeName,
          hasRequestField,
          requestFieldTypeName,
          requestFieldName,
          isPageStreaming,
          pageStreamingInfo);
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

    public abstract boolean hasRequestField();

    @Nullable
    public abstract String requestFieldTypeName();

    @Nullable
    public abstract String requestFieldName();

    public abstract boolean isPageStreaming();

    @Nullable
    public abstract PageStreamingInfo pageStreaming();
  }

  private String fixReservedWordVar(String s) {
    if (RESERVED_WORDS.contains(s)) {
      return s + "_";
    }
    return s;
  }

  @Override
  public String getSampleVarName(String typeName) {
    return fixReservedWordVar(upperCamelToLowerCamel(getSimpleName(typeName)));
  }

  public SampleInfo getSampleInfo(Method method) {
    try {
      return getSampleInfo0(method);
    } catch (Exception e) {
      e.printStackTrace();
      throw e;
    }
  }

  public SampleInfo getSampleInfo0(Method method) {
    final ApiaryConfig apiary = getApiaryConfig();
    String packageName =
        CSharpContextCommon.s_underscoresToPascalCase(apiary.getServiceCanonicalName());
    String namespace = packageName + "Sample";
    String serviceTypeName = using(serviceNamespace + "." + packageName + "Service");
    String serviceVarName = CSharpContextCommon.s_underscoresToCamelCase(packageName) + "Service";
    String methodName =
        CSharpContextCommon.s_underscoresToPascalCase(getSimpleName(method.getName()));
    boolean hasRequestField = hasRequestField(method);
    final Type methodType = apiary.getType(method.getRequestTypeUrl());

    final String requestTypeName =
        FluentIterable.from(apiary.getResources(method.getName()))
            .transform(
                new Function<String, String>() {
                  @Override
                  public String apply(String resourceName) {
                    return CSharpContextCommon.s_underscoresToPascalCase(resourceName) + "Resource";
                  }
                })
            .append(methodName + "Request")
            .join(Joiner.on('.'));

    List<ParamInfo> params =
        FluentIterable.from(getFlatMethodParams(method))
            .transform(
                new Function<String, ParamInfo>() {
                  @Override
                  public ParamInfo apply(String paramName) {
                    Field field = getField(methodType, paramName);
                    String typeName = typeName(methodType, field, requestTypeName);
                    String defaultValue = defaultValue(methodType, field, typeName);
                    String name =
                        fixReservedWordVar(CSharpContextCommon.s_underscoresToCamelCase(paramName));
                    String description = getDescription(methodType.getName(), paramName);
                    String sample = getDefaultSample(methodType, field);
                    return ParamInfo.create(typeName, name, defaultValue, description, sample);
                  }
                })
            .toList();
    Iterable<String> requestFieldParam =
        hasRequestField ? ImmutableList.of(REQUEST_FIELD_NAME) : ImmutableList.<String>of();
    String paramList =
        FluentIterable.from(requestFieldParam)
            .append(
                FluentIterable.from(params)
                    .transform(
                        new Function<ParamInfo, String>() {
                          @Override
                          public String apply(ParamInfo paramInfo) {
                            return paramInfo.name();
                          }
                        }))
            .join(Joiner.on(", "));

    String resourcePath =
        FluentIterable.from(apiary.getResources(method.getName()))
            .transform(
                new Function<String, String>() {
                  @Override
                  public String apply(String resourceName) {
                    return CSharpContextCommon.s_underscoresToPascalCase(resourceName);
                  }
                })
            .join(Joiner.on('.'));

    String responseTypeName = usingData(method.getResponseTypeUrl());
    String requestFieldTypeName = null;
    if (hasRequestField) {
      requestFieldTypeName = usingData(getRequestField(method).getTypeUrl());
    }
    boolean isPageStreaming = isPageStreaming(method);
    PageStreamingInfo pageStreamingInfo;
    if (isPageStreaming) {
      Type responseType = apiary.getType(method.getResponseTypeUrl());
      Field resourceField = getFirstRepeatedField(responseType);
      String resourceTypeName = elementTypeName(responseType, resourceField, requestTypeName);
      // Used to handle inconsistency in page-streaming methods for Bigquery API.
      if (isBigqueryPageStreamingMethod(method)) {
        resourceTypeName = resourceTypeName + "Data";
      }
      pageStreamingInfo =
          PageStreamingInfo.create(
              CSharpContextCommon.s_underscoresToPascalCase(resourceField.getName()),
              resourceTypeName);
    } else {
      pageStreamingInfo = null;
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
        responseTypeName,
        hasRequestField,
        requestFieldTypeName,
        REQUEST_FIELD_NAME,
        isPageStreaming,
        pageStreamingInfo);
  }

  private String defaultValue(Type parentType, Field field, String typeName) {
    if (field.getCardinality() == Field.Cardinality.CARDINALITY_REPEATED) {
      return "new " + typeName + "()";
    } else if (field.getKind() == Kind.TYPE_ENUM) {
      return "(" + typeName + ") 0";
    } else if (field.getKind() == Kind.TYPE_STRING) {
      return getDefaultString(parentType, field).getDefine();
    } else {
      return DEFAULTVALUE_MAP.get(field.getKind());
    }
  }

  private String elementTypeName(Type parentType, Field field, String requestTypeName) {
    ApiaryConfig apiary = getApiaryConfig();
    String fieldName = field.getName();
    String fieldTypeName = field.getTypeUrl();
    Type fieldType = apiary.getType(fieldTypeName);
    if (isMapField(parentType, fieldName)) {
      Field keyField = apiary.getField(fieldType, "key");
      Field valueField = apiary.getField(fieldType, "value");
      String keyTypeName = typeName(fieldType, keyField, requestTypeName);
      String valueTypeName = typeName(fieldType, valueField, requestTypeName);
      return using(
          "System.Collections.Generic.KeyValuePair<" + keyTypeName + ", " + valueTypeName + ">");
    }
    if (field.getCardinality() == Field.Cardinality.CARDINALITY_REPEATED) {
      Field elementField =
          field.toBuilder().setCardinality(Field.Cardinality.CARDINALITY_REQUIRED).build();
      return typeName(parentType, elementField, requestTypeName);
    }
    throw new IllegalArgumentException("Not a collection type.");
  }

  public String typeName(Type parentType, Field field, String requestTypeName) {
    ApiaryConfig apiary = getApiaryConfig();
    String fieldName = field.getName();
    String fieldTypeName = field.getTypeUrl();
    Type fieldType = apiary.getType(fieldTypeName);
    if (isMapField(parentType, fieldName)) {
      Field keyField = apiary.getField(fieldType, "key");
      Field valueField = apiary.getField(fieldType, "value");
      String keyTypeName = typeName(fieldType, keyField, requestTypeName);
      String valueTypeName = typeName(fieldType, valueField, requestTypeName);
      return using(
          "System.Collections.Generic.Dictionary<" + keyTypeName + ", " + valueTypeName + ">");
    }
    if (field.getCardinality() == Field.Cardinality.CARDINALITY_REPEATED) {
      Field elementField =
          field.toBuilder().setCardinality(Field.Cardinality.CARDINALITY_REQUIRED).build();
      String elementTypeName = typeName(parentType, elementField, requestTypeName);
      return using("System.Collections.Generic.List<" + elementTypeName + ">");
    }
    Kind kind = field.getKind();
    if (kind == Kind.TYPE_ENUM) {
      return using(
          serviceNamespace
              + "."
              + requestTypeName
              + "."
              + CSharpContextCommon.s_underscoresToPascalCase(field.getName())
              + "Enum");
    }
    if (kind == Kind.TYPE_MESSAGE) {
      Field elementsField = apiary.getField(fieldType, DiscoveryImporter.ELEMENTS_FIELD_NAME);
      if (elementsField != null) {
        return typeName(fieldType, elementsField, requestTypeName);
      } else {
        return using(serviceNamespace + ".Data." + fieldTypeName);
      }
    }
    String result = FIELD_TYPE_MAP.get(kind);
    if (result != null) {
      // No need to use using(), all primitive types are always in scope.
      return result;
    }
    throw new IllegalArgumentException("Unknown type kind: " + kind);
  }

  // Handlers for Exceptional Inconsistencies
  // ========================================

  // Used to handle inconsistency in page-streaming methods for Bigquery API.
  // Remove if inconsistency is removed in client library.
  protected boolean isBigqueryPageStreamingMethod(Method method) {
    Api api = getApi();
    return api.getName().equals("bigquery")
        && api.getVersion().equals("v2")
        && isPageStreaming(method);
  }
}
