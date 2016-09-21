package com.google.api.codegen.util.csharp;

import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.NameFormatter;
import com.google.api.codegen.util.NamePath;

public class CSharpNameFormatter implements NameFormatter {

  @Override
  public String className(Name name) {
    return name.toUpperCamel();
  }

  @Override
  public String varName(Name name) {
    return name.toLowerCamel();
  }

  @Override
  public String varReference(Name name) {
    return name.toLowerCamel();
  }

  @Override
  public String methodName(Name name) {
    return name.toUpperCamel();
  }

  @Override
  public String staticFunctionName(Name name) {
    return name.toUpperCamel();
  }

  @Override
  public String inittedConstantName(Name name) {
    return name.toUpperCamel();
  }

  @Override
  public String keyName(Name name) {
    throw new RuntimeException();
  }

  @Override
  public String qualifiedName(NamePath namePath) {
    return namePath.toDotted();
  }

  @Override
  public String packageFilePathPiece(Name name) {
    throw new RuntimeException();
  }

  @Override
  public String classFileNameBase(Name name) {
    throw new RuntimeException();
  }

}
