package com.google.api.codegen.transformer.csharp;

import com.google.api.codegen.CollectionConfig;
import com.google.api.codegen.MethodConfig;
import com.google.api.codegen.ServiceMessages;
import com.google.api.codegen.transformer.ModelTypeFormatterImpl;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.transformer.java.JavaModelTypeNameConverter;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.csharp.CSharpNameFormatter;
import com.google.api.codegen.util.csharp.CSharpTypeTable;
import com.google.api.codegen.util.java.JavaNameFormatter;
import com.google.api.codegen.util.java.JavaTypeTable;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.TypeRef;

public class CSharpSurfaceNamer extends SurfaceNamer {

  public CSharpSurfaceNamer(String implicitPackageName) {
    super(
        new CSharpNameFormatter(),
        new ModelTypeFormatterImpl(new CSharpModelTypeNameConverter(implicitPackageName)),
        new CSharpTypeTable(implicitPackageName));
  }

  @Override
  public String getFullyQualifiedApiWrapperClassName(Interface service, String packageName) {
    return packageName + "." + getApiWrapperClassName(service);
  }

  @Override
  public String getStaticLangReturnTypeName(Method method, MethodConfig methodConfig) {
    if (ServiceMessages.s_isEmptyType(method.getOutputType())) {
      return "void";
    }
    return getModelTypeFormatter().getFullNameFor(method.getOutputType());
  }

  @Override
  public String getApiWrapperClassName(Interface interfaze) {
    return className(Name.upperCamel(interfaze.getSimpleName(), "Client"));
  }

  @Override
  public String getCallableName(Method method) {
    return "_" + varName(Name.upperCamel("Call", method.getSimpleName()));
  }

  @Override
  public String getPathTemplateName(CollectionConfig collectionConfig) {
    return inittedConstantName(Name.from(collectionConfig.getEntityName(), "template"));
  }

  @Override
  public String getFieldGetFunctionName(TypeRef type, Name identifier) {
    return methodName(identifier);
  }

  @Override
  public String getAndSavePagedResponseTypeName(
      ModelTypeTable typeTable, TypeRef... parameterizedTypes) {
    String[] typeList = new String[parameterizedTypes.length];
    for (int i = 0; i < typeList.length; i++) {
      typeList[i] = typeTable.getFullNameForElementType(parameterizedTypes[i]);
    }
    return typeTable.getAndSaveNicknameForContainer(
        "Google.Api.Gax.PagedEnumerable", typeList);
  }

}
