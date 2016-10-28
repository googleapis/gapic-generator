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
package com.google.api.codegen.ruby;

import com.google.api.codegen.GapicContext;
import com.google.api.codegen.config.ApiConfig;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.transformer.ApiMethodTransformer;
import com.google.api.codegen.transformer.GrpcStubTransformer;
import com.google.api.codegen.transformer.MethodTransformerContext;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.StandardImportTypeTransformer;
import com.google.api.codegen.transformer.SurfaceTransformerContext;
import com.google.api.codegen.transformer.ruby.RubyFeatureConfig;
import com.google.api.codegen.transformer.ruby.RubyModelTypeNameConverter;
import com.google.api.codegen.transformer.ruby.RubySurfaceNamer;
import com.google.api.codegen.util.ruby.RubyTypeTable;
import com.google.api.codegen.viewmodel.GrpcStubView;
import com.google.api.codegen.viewmodel.ImportTypeView;
import com.google.api.codegen.viewmodel.OptionalArrayMethodView;
import com.google.api.tools.framework.aspects.documentation.model.DocumentationUtil;
import com.google.api.tools.framework.aspects.documentation.model.ElementDocumentationAttribute;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.MessageType;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.ProtoContainerElement;
import com.google.api.tools.framework.model.ProtoElement;
import com.google.api.tools.framework.model.ProtoFile;
import com.google.api.tools.framework.model.TypeRef;
import com.google.api.tools.framework.model.TypeRef.Cardinality;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

/** A GapicContext specialized for Ruby. */
public class RubyGapicContext extends GapicContext implements RubyContext {

  public RubyGapicContext(Model model, ApiConfig apiConfig) {
    super(model, apiConfig);
  }

  // Snippet Helpers
  // ===============

  /** Returns the Ruby filename which holds the gRPC service definition. */
  public String getGrpcFilename(Interface service) {
    return getBasename(service.getFile()) + "_services_pb";
  }

  public String getBasename(ProtoFile protoFile) {
    return protoFile.getSimpleName().replace(".proto", "");
  }

  /** Return comments lines for a given proto element, extracted directly from the proto doc */
  public List<String> defaultComments(ProtoElement element) {
    if (!element.hasAttribute(ElementDocumentationAttribute.KEY)) {
      return ImmutableList.<String>of();
    }
    return convertToCommentedBlock(
        RDocCommentFixer.rdocify(DocumentationUtil.getScopedDescription(element)));
  }

  /** Returns type information for a field in YARD style. */
  private String fieldTypeCardinalityComment(Field field) {
    TypeRef type = field.getType();

    String cardinalityComment;
    String closing;
    if (type.getCardinality() == Cardinality.REPEATED) {
      if (type.isMap()) {
        cardinalityComment = "Hash{" + rubyTypeName(type.getMapKeyField().getType()) + " => ";
        field = type.getMapValueField();
        closing = "}";
      } else {
        cardinalityComment = "Array<";
        closing = ">";
      }
    } else {
      cardinalityComment = "";
      closing = "";
    }
    String typeComment = rubyTypeName(field.getType());
    return String.format("%s%s%s", cardinalityComment, typeComment, closing);
  }

  /** Returns a YARD comment string for the field as a parameter to a function. */
  private String fieldParamComment(Field field, String paramComment) {
    String commentType = fieldTypeCardinalityComment(field);
    String comment =
        String.format("@param %s [%s]", wrapIfKeywordOrBuiltIn(field.getSimpleName()), commentType);
    if (paramComment == null) {
      paramComment = DocumentationUtil.getScopedDescription(field);
    }
    if (!Strings.isNullOrEmpty(paramComment)) {
      paramComment = RDocCommentFixer.rdocify(paramComment);
      comment += "\n  " + paramComment.replaceAll("(\\r?\\n)", "\n  ");
    }
    return comment + "\n";
  }

  /** Return a YARD comment string for the field, as the attribute of a message. */
  private String fieldAttributeComment(Field field) {
    String comment =
        "@!attribute [rw] "
            + field.getSimpleName()
            + "\n"
            + "  @return ["
            + fieldTypeCardinalityComment(field)
            + "]";
    String fieldComment = DocumentationUtil.getScopedDescription(field);
    if (!Strings.isNullOrEmpty(fieldComment)) {
      fieldComment = RDocCommentFixer.rdocify(fieldComment);
      comment += "\n    " + fieldComment.replaceAll("(\\r?\\n)", "\n    ");
    }
    return comment + "\n";
  }

  /** Return YARD return type string for the given method, or null if the return type is nil. */
  @Nullable
  private String returnTypeComment(Method method, MethodConfig config) {
    MessageType returnMessageType = method.getOutputMessage();
    if (returnMessageType.getFullName().equals("google.protobuf.Empty")) {
      return null;
    }

    String classInfo = rubyTypeName(method.getOutputType());

    if (config.isPageStreaming()) {
      String resourceType = rubyTypeName(config.getPageStreaming().getResourcesField().getType());
      return "@return [Google::Gax::PagedEnumerable<"
          + resourceType
          + ">]\n"
          + "  An enumerable of "
          + resourceType
          + " instances.\n"
          + "  See Google::Gax::PagedEnumerable documentation for other\n"
          + "  operations such as per-page iteration or access to the response\n"
          + "  object.";
    } else {
      return "@return [" + classInfo + "]";
    }
  }

  /**
   * Return comments lines for a given method, consisting of proto doc and parameter type
   * documentation.
   */
  public List<String> methodComments(Interface service, Method method) {
    MethodConfig config = getApiConfig().getInterfaceConfig(service).getMethodConfig(method);

    // Generate parameter types
    StringBuilder paramTypesBuilder = new StringBuilder();
    for (Field field :
        removePageTokenFromFields(method.getInputType().getMessageType().getFields(), config)) {
      if (config.isPageStreaming()
          && field.equals((config.getPageStreaming().getPageSizeField()))) {
        paramTypesBuilder.append(
            fieldParamComment(
                field,
                "The maximum number of resources contained in the underlying API\n"
                    + "response. If page streaming is performed per-resource, this\n"
                    + "parameter does not affect the return value. If page streaming is\n"
                    + "performed per-page, this determines the maximum number of\n"
                    + "resources in a page."));
      } else {
        paramTypesBuilder.append(fieldParamComment(field, null));
      }
    }
    paramTypesBuilder.append(
        "@param options [Google::Gax::CallOptions] \n"
            + "  Overrides the default settings for this call, e.g, timeout,\n"
            + "  retries, etc.");
    String paramTypes = paramTypesBuilder.toString();

    String returnType = returnTypeComment(method, config);

    // Generate comment contents
    StringBuilder contentBuilder = new StringBuilder();
    if (method.hasAttribute(ElementDocumentationAttribute.KEY)) {
      contentBuilder.append(
          RDocCommentFixer.rdocify(DocumentationUtil.getScopedDescription(method)));
      if (!Strings.isNullOrEmpty(paramTypes)) {
        contentBuilder.append("\n\n");
      }
    }
    contentBuilder.append(paramTypes);
    if (returnType != null) {
      contentBuilder.append("\n" + returnType);
    }

    contentBuilder.append("\n@raise [Google::Gax::GaxError] if the RPC is aborted.");
    return convertToCommentedBlock(contentBuilder.toString());
  }

  /** Return the list of messages within element which should be documented in Ruby. */
  public ImmutableList<MessageType> filterDocumentingMessages(ProtoContainerElement element) {
    ImmutableList.Builder<MessageType> builder = ImmutableList.builder();
    for (MessageType msg : element.getMessages()) {
      // Doesn't have to document map entries in Ruby because Hash is used.
      if (!msg.isMapEntry()) {
        builder.add(msg);
      }
    }
    return builder.build();
  }

  /** Return the doccomment for the message. */
  public List<String> methodDocComment(MessageType msg) {
    StringBuilder attributesBuilder = new StringBuilder();
    for (Field field : msg.getFields()) {
      attributesBuilder.append(fieldAttributeComment(field));
    }

    String attributes = attributesBuilder.toString().trim();

    List<String> content = defaultComments(msg);
    if (!Strings.isNullOrEmpty(attributes)) {
      return ImmutableList.<String>builder()
          .addAll(content)
          .addAll(convertToCommentedBlock(attributes))
          .build();
    }
    return content;
  }

  /** Return a non-conflicting safe name if name is a Ruby built-in. */
  public String wrapIfKeywordOrBuiltIn(String name) {
    if (KEYWORD_BUILT_IN_SET.contains(name)) {
      return name + "_";
    }
    return name;
  }

  /** Returns the name of Ruby class for the given proto element. */
  public String rubyTypeNameForProtoElement(ProtoElement element) {
    String fullName = element.getFullName();
    int lastDot = fullName.lastIndexOf('.');
    if (lastDot < 0) {
      return fullName;
    }
    List<String> rubyNames = new ArrayList<>();
    for (String name : fullName.substring(0, lastDot).split("\\.")) {
      if (Character.isUpperCase(name.charAt(0))) {
        rubyNames.add(name);
      } else {
        rubyNames.add(lowerUnderscoreToUpperCamel(name));
      }
    }
    rubyNames.add(element.getSimpleName());
    return Joiner.on("::").join(rubyNames);
  }

  /** Returns the name of Ruby class for the given typeRef. */
  public String rubyTypeName(TypeRef typeRef) {
    switch (typeRef.getKind()) {
      case TYPE_MESSAGE:
        return rubyTypeNameForProtoElement(typeRef.getMessageType());
      case TYPE_ENUM:
        return rubyTypeNameForProtoElement(typeRef.getEnumType());
      default:
        {
          String name = PRIMITIVE_TYPE_NAMES.get(typeRef.getKind());
          if (!Strings.isNullOrEmpty(name)) {
            return name;
          }
          throw new IllegalArgumentException("unknown type kind: " + typeRef.getKind());
        }
    }
  }

  /**
   * Convert the content string into a commented block that can be directly printed out in the
   * generated Ruby files.
   */
  private List<String> convertToCommentedBlock(String content) {
    if (Strings.isNullOrEmpty(content)) {
      return ImmutableList.<String>of();
    }
    ImmutableList.Builder<String> builder = ImmutableList.builder();
    for (String comment : Splitter.on("\n").splitToList(content)) {
      builder.add(comment);
    }
    return builder.build();
  }

  /** Returns the iterable of Ruby module names for the proto element. */
  public Iterable<String> getGrpcModules(ProtoElement element) {
    String fullName = element.getFullName();
    List<String> modules = new ArrayList<>();
    for (String pkgName : Splitter.on(".").splitToList(fullName)) {
      modules.add(lowerUnderscoreToUpperCamel(pkgName));
    }
    return modules;
  }

  public Iterable<String> getApiModules() {
    return Splitter.on("::").splitToList(getApiConfig().getPackageName());
  }

  public OptionalArrayMethodView getMethodView(Interface service, Method method) {
    SurfaceTransformerContext context = getSurfaceTransformerContextFromService(service);
    MethodTransformerContext methodContext = context.asDynamicMethodContext(method);
    ApiMethodTransformer methodTransformer = new ApiMethodTransformer();
    return methodTransformer.generateDynamicLangApiMethod(methodContext);
  }

  public List<GrpcStubView> getStubs(Interface service) {
    GrpcStubTransformer grpcStubTransformer = new GrpcStubTransformer();
    SurfaceTransformerContext context = getSurfaceTransformerContextFromService(service);
    return grpcStubTransformer.generateGrpcStubs(context);
  }

  public List<ImportTypeView> getServiceImports(Interface service) {
    StandardImportTypeTransformer importTypeTransformer = new StandardImportTypeTransformer();
    SurfaceTransformerContext context = getSurfaceTransformerContextFromService(service);
    return importTypeTransformer.generateServiceFileImports(context);
  }

  public List<ImportTypeView> getProtoImports(Interface service) {
    StandardImportTypeTransformer importTypeTransformer = new StandardImportTypeTransformer();
    SurfaceTransformerContext context = getSurfaceTransformerContextFromService(service);
    return importTypeTransformer.generateProtoFileImports(context);
  }

  private SurfaceTransformerContext getSurfaceTransformerContextFromService(Interface service) {
    ModelTypeTable modelTypeTable =
        new ModelTypeTable(
            new RubyTypeTable(getApiConfig().getPackageName()),
            new RubyModelTypeNameConverter(getApiConfig().getPackageName()));
    return SurfaceTransformerContext.create(
        service,
        getApiConfig(),
        modelTypeTable,
        new RubySurfaceNamer(getApiConfig().getPackageName()),
        new RubyFeatureConfig());
  }

  // Constants
  // =========

  /** A map from primitive types to its default value. */
  private static final ImmutableMap<Type, String> DEFAULT_VALUE_MAP =
      ImmutableMap.<Type, String>builder()
          .put(Type.TYPE_BOOL, "false")
          .put(Type.TYPE_DOUBLE, "0.0")
          .put(Type.TYPE_FLOAT, "0.0")
          .put(Type.TYPE_INT64, "0")
          .put(Type.TYPE_UINT64, "0")
          .put(Type.TYPE_SINT64, "0")
          .put(Type.TYPE_FIXED64, "0")
          .put(Type.TYPE_SFIXED64, "0")
          .put(Type.TYPE_INT32, "0")
          .put(Type.TYPE_UINT32, "0")
          .put(Type.TYPE_SINT32, "0")
          .put(Type.TYPE_FIXED32, "0")
          .put(Type.TYPE_SFIXED32, "0")
          .put(Type.TYPE_STRING, "\'\'")
          .put(Type.TYPE_BYTES, "\'\'")
          .build();

  private static final ImmutableMap<Type, String> PRIMITIVE_TYPE_NAMES =
      ImmutableMap.<Type, String>builder()
          .put(Type.TYPE_BOOL, "true, false")
          .put(Type.TYPE_DOUBLE, "Float")
          .put(Type.TYPE_FLOAT, "Float")
          .put(Type.TYPE_INT64, "Integer")
          .put(Type.TYPE_UINT64, "Integer")
          .put(Type.TYPE_SINT64, "Integer")
          .put(Type.TYPE_FIXED64, "Integer")
          .put(Type.TYPE_SFIXED64, "Integer")
          .put(Type.TYPE_INT32, "Integer")
          .put(Type.TYPE_UINT32, "Integer")
          .put(Type.TYPE_SINT32, "Integer")
          .put(Type.TYPE_FIXED32, "Integer")
          .put(Type.TYPE_SFIXED32, "Integer")
          .put(Type.TYPE_STRING, "String")
          .put(Type.TYPE_BYTES, "String")
          .build();

  /**
   * A set of ruby keywords and built-ins. keywords:
   * http://docs.ruby-lang.org/en/2.3.0/keywords_rdoc.html
   */
  private static final ImmutableSet<String> KEYWORD_BUILT_IN_SET =
      ImmutableSet.<String>builder()
          .add(
              "__ENCODING__",
              "__LINE__",
              "__FILE__",
              "BEGIN",
              "END",
              "alias",
              "and",
              "begin",
              "break",
              "case",
              "class",
              "def",
              "defined?",
              "do",
              "else",
              "elsif",
              "end",
              "ensure",
              "false",
              "for",
              "if",
              "in",
              "module",
              "next",
              "nil",
              "not",
              "or",
              "redo",
              "rescue",
              "retry",
              "return",
              "self",
              "super",
              "then",
              "true",
              "undef",
              "unless",
              "until",
              "when",
              "while",
              "yield",
              // "options" is here because it's a common keyword argument to
              // specify a CallOptions instance.
              "options")
          .build();
}
