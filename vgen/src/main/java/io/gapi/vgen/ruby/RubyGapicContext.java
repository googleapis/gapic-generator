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
package io.gapi.vgen.ruby;

import com.google.api.tools.framework.aspects.documentation.model.DocumentationUtil;
import com.google.api.tools.framework.aspects.documentation.model.ElementDocumentationAttribute;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.MessageType;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.ProtoElement;
import com.google.api.tools.framework.model.ProtoFile;
import com.google.api.tools.framework.model.TypeRef;
import com.google.api.tools.framework.model.TypeRef.Cardinality;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;

import io.gapi.vgen.ApiConfig;
import io.gapi.vgen.GapicContext;
import io.gapi.vgen.MethodConfig;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

/**
 * A GapicContext specialized for Ruby.
 */
public class RubyGapicContext extends GapicContext {

  public RubyGapicContext(Model model, ApiConfig apiConfig) {
    super(model, apiConfig);
  }

  // Snippet Helpers
  // ===============

  /**
   * Returns the Ruby filename which holds the gRPC service definition.
   */
  public String getGrpcFilename(Interface service) {
    return service.getFile().getProto().getName().replace(".proto", "_services");
  }

  /**
   * Return comments lines for a given proto element, extracted directly from the proto doc
   */
  public List<String> defaultComments(ProtoElement element) {
    if (!element.hasAttribute(ElementDocumentationAttribute.KEY)) {
      return ImmutableList.<String>of("");
    }
    return convertToCommentedBlock(RDocCommentFixer.rdocify(
        DocumentationUtil.getScopedDescription(element)));
  }

  /**
   * Returns type information for a field in YARD style.
   */
  private String fieldTypeCardinalityComment(Field field) {
    TypeRef type = field.getType();

    String cardinalityComment;
    String closing;
    if (type.getCardinality() == Cardinality.REPEATED) {
      if (type.isMap()) {
        cardinalityComment = "Hash{" +
            rubyTypeName(type.getMapKeyField().getType()) + " => ";
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

  /**
   * Returns a YARD comment string for field, consisting of type information and proto comment.
   */
  private String fieldComment(Field field) {
    String commentType = fieldTypeCardinalityComment(field);
    String comment = String.format(
        "@param %s [%s]", wrapIfKeywordOrBuiltIn(field.getSimpleName()), commentType);
    String paramComment = DocumentationUtil.getScopedDescription(field);
    if (!Strings.isNullOrEmpty(paramComment)) {
      if (paramComment.charAt(paramComment.length() - 1) == '\n') {
        paramComment = paramComment.substring(0, paramComment.length() - 1);
      }
      comment += "\n  " + paramComment.replaceAll("(\\r?\\n)", "\n  ");
    }
    return comment + "\n";
  }

  /**
   * Return YARD return type string for the given method, or null if the return type is
   * nil.
   */
  @Nullable
  private String returnTypeComment(Method method) {
    MessageType returnMessageType = method.getOutputMessage();
    if (returnMessageType.getFullName().equals("google.protobuf.Empty")) {
      return null;
    }

    String classInfo = rubyTypeName(method.getOutputType());
    MethodConfig config = getApiConfig().getInterfaceConfig((Interface) method.getParent())
        .getMethodConfig(method);

    if (config.isPageStreaming()) {
      String resourceType = rubyTypeName(config.getPageStreaming().getResourcesField().getType());
      return "@return [\n"
          + "  Google::Gax::PagedEnumerable<" + resourceType + ">,\n"
          + "  " + classInfo + "]\n"
          + "  An enumerable of " + resourceType + " instances unless\n"
          + "  page streaming is disabled through the call options. If page\n"
          + "  streaming is disabled, a single " + classInfo + "\n"
          + "  instance is returned.";
    } else {
      return "@return [" + classInfo + "]";
    }
  }


  /**
   * Return comments lines for a given method, consisting of proto doc and parameter type
   * documentation.
   */
  public List<String> methodComments(Method msg) {
    // Generate parameter types
    StringBuilder paramTypesBuilder = new StringBuilder();
    for (Field field : this.messages().flattenedFields(msg.getInputType())) {
      paramTypesBuilder.append(fieldComment(field));
    }
    paramTypesBuilder.append("@param options [Google::Gax::CallOptions] \n" +
                             "  Overrides the default settings for this call, e.g, timeout,\n" +
                             "  retries, etc.");
    String paramTypes = paramTypesBuilder.toString();

    String returnType = returnTypeComment(msg);

    // Generate comment contents
    StringBuilder contentBuilder = new StringBuilder();
    if (msg.hasAttribute(ElementDocumentationAttribute.KEY)) {
      String text = RDocCommentFixer.rdocify(DocumentationUtil.getScopedDescription(msg));
      text = text.trim();
      contentBuilder.append(text.replaceAll("\\s*\\n\\s*", "\n"));
      if (!Strings.isNullOrEmpty(paramTypes)) {
        contentBuilder.append("\n\n");
      }
    }
    contentBuilder.append(paramTypes);
    if (returnType != null) {
      contentBuilder.append("\n" + returnType);
    }

    contentBuilder.append(
        "\n@raise [Google::Gax::GaxError] if the RPC is aborted.");
    return convertToCommentedBlock(contentBuilder.toString());
  }

  /**
   * Return a non-conflicting safe name if name is a Ruby built-in.
   */
  public String wrapIfKeywordOrBuiltIn(String name) {
    if (KEYWORD_BUILT_IN_SET.contains(name)) {
      return name + "_";
    }
    return name;
  }

  /**
   * Return the default value for the given field. Return null if there is no default value.
   */
  public String defaultValue(Field field) {
    TypeRef type = field.getType();
    // Return empty array if the type is repeated.
    if (type.isMap()) {
      return "{}";
    }
    if (type.getCardinality() == Cardinality.REPEATED) {
      return "[]";
    }
    switch (type.getKind()) {
      case TYPE_MESSAGE:
        return rubyTypeName(type) + ".new";
      case TYPE_ENUM:
        Preconditions.checkArgument(type.getEnumType().getValues().size() > 0,
            "enum must have a value");
        return rubyTypeName(type) + "::" + type.getEnumType().getValues().get(0).getSimpleName();
      default:
        if (type.isPrimitive()) {
          return DEFAULT_VALUE_MAP.get(type.getKind());
        }
        throw new IllegalArgumentException("unknown type kind: " + type.getKind());
    }
  }

  /**
   * Returns the name of Ruby class for the given typeRef.
   */
  public String rubyTypeName(TypeRef typeRef) {
    switch (typeRef.getKind()) {
      case TYPE_MESSAGE:
      case TYPE_ENUM: {
        String fullName = typeRef.getMessageType().getFullName();
        int lastDot = fullName.lastIndexOf('.');
        if (lastDot < 0) {
          return fullName;
        }
        List<String> rubyNames = new ArrayList<>();
        for (String name : fullName.substring(0, lastDot).split("\\.")) {
          rubyNames.add(lowerUnderscoreToUpperCamel(name));
        }
        rubyNames.add(typeRef.getMessageType().getSimpleName());
        return String.join("::", rubyNames);
      }
      default: {
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

  public Iterable<String> getModules() {
    return Splitter.on("::").split(getApiConfig().getPackageName());
  }

  // Constants
  // =========

  /**
   * A map from primitive types to its default value.
   */
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
   * A set of ruby keywords and built-ins.
   * keywords: http://docs.ruby-lang.org/en/2.3.0/keywords_rdoc.html
   */
  private static final ImmutableSet<String> KEYWORD_BUILT_IN_SET =
      ImmutableSet.<String>builder()
      .add("__ENCODING__",
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
           "yield")
      .build();

}
