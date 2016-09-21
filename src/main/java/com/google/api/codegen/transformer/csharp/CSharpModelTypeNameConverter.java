package com.google.api.codegen.transformer.csharp;

import com.google.api.codegen.transformer.ModelTypeNameConverter;
import com.google.api.codegen.util.TypeName;
import com.google.api.codegen.util.TypeNameConverter;
import com.google.api.codegen.util.TypedValue;
import com.google.api.codegen.util.csharp.CSharpTypeTable;
import com.google.api.codegen.util.java.JavaTypeTable;
import com.google.api.tools.framework.model.MessageType;
import com.google.api.tools.framework.model.ProtoElement;
import com.google.api.tools.framework.model.TypeRef;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;

public class CSharpModelTypeNameConverter implements ModelTypeNameConverter {

  /**
   * A map from primitive types in proto to Java counterparts.
   */
  private static final ImmutableMap<Type, String> PRIMITIVE_TYPE_MAP =
      ImmutableMap.<Type, String>builder()
          .put(Type.TYPE_BOOL, "bool")
          .put(Type.TYPE_DOUBLE, "double")
          .put(Type.TYPE_FLOAT, "float")
          .put(Type.TYPE_INT64, "long")
          .put(Type.TYPE_UINT64, "ulong")
          .put(Type.TYPE_SINT64, "long")
          .put(Type.TYPE_FIXED64, "long")
          .put(Type.TYPE_SFIXED64, "long")
          .put(Type.TYPE_INT32, "int")
          .put(Type.TYPE_UINT32, "uint")
          .put(Type.TYPE_SINT32, "int")
          .put(Type.TYPE_FIXED32, "int")
          .put(Type.TYPE_SFIXED32, "int")
          .put(Type.TYPE_STRING, "string")
          .put(Type.TYPE_BYTES, "ByteString") // TODO: Full path
          .build();

  private TypeNameConverter typeNameConverter;

  public CSharpModelTypeNameConverter(String implicitPackageName) {
    this.typeNameConverter = new CSharpTypeTable(implicitPackageName); // TODO: Is it really OK to create a new instance here?
  }

  @Override
  public TypeName getTypeName(TypeRef type) {
    if (type.isMap()) {
      throw new RuntimeException();
    } else if (type.isRepeated()) {
      TypeName listTypeName = typeNameConverter.getTypeName("System.Collections.Generic.IEnumerable");
      TypeName elementTypeName = getTypeNameForElementType(type);
      return new TypeName(
          listTypeName.getFullName(), listTypeName.getNickname(), "%s<%i>", elementTypeName);
    } else {
      return getTypeNameForElementType(type);
    }
  }

  @Override
  public TypeName getTypeNameForElementType(TypeRef type) {
    switch (type.getKind()) {
      case TYPE_MESSAGE:
        return getTypeName(type.getMessageType());
      case TYPE_ENUM:
        return getTypeName(type.getEnumType());
      default:
        String typeName = PRIMITIVE_TYPE_MAP.get(type.getKind());
        return typeNameConverter.getTypeName(typeName);
    }
  }

  @Override
  public TypeName getTypeName(ProtoElement elem) {
    // Handle nested types, construct the required type prefix
    ProtoElement parentEl = elem.getParent();
    String prefix = "";
    while (parentEl != null && parentEl instanceof MessageType) {
      prefix = parentEl.getSimpleName() + ".Types." + prefix;
      parentEl = parentEl.getParent();
    }
    String shortName = elem.getSimpleName();
    return new TypeName(prefix + shortName, shortName);
  }

  @Override
  public TypedValue getZeroValue(TypeRef type) {
return TypedValue.create(getTypeName(type), "ZERO_VALUE"); // TODO: Generate correctly
  }

  @Override
  public String renderPrimitiveValue(TypeRef type, String value) {
    throw new RuntimeException();
  }

}
