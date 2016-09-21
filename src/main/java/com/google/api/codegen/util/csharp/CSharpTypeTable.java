package com.google.api.codegen.util.csharp;

import com.google.api.codegen.util.NamePath;
import com.google.api.codegen.util.TypeAlias;
import com.google.api.codegen.util.TypeName;
import com.google.api.codegen.util.TypeTable;
import com.google.api.codegen.util.java.JavaTypeTable;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import java.util.HashMap;
import java.util.Map;

public class CSharpTypeTable implements TypeTable {

  private final String implicitPackageName;
  // Fullname to shortname map
  private final Map<String, String> imports = new HashMap<>();

  public CSharpTypeTable(String implicitPackageName) {
    this.implicitPackageName = implicitPackageName;
  }

  @Override
  public TypeName getTypeName(String fullName) {
    int lastDotIndex = fullName.lastIndexOf('.');
    if (lastDotIndex < 0) {
      return new TypeName(fullName, fullName);
    }
    String shortTypeName = fullName.substring(lastDotIndex + 1);
    return new TypeName(fullName, shortTypeName);
  }

  @Override
  public NamePath getNamePath(String fullName) {
    return NamePath.dotted(fullName);
  }

  @Override
  public TypeName getContainerTypeName(String containerFullName, String... elementFullNames) {
    throw new RuntimeException();
  }

  @Override
  public TypeTable cloneEmpty() {
    return new CSharpTypeTable(implicitPackageName);
  }

  @Override
  public String getAndSaveNicknameFor(String fullName) {
    return getAndSaveNicknameFor(getTypeName(fullName));
  }

  @Override
  public String getAndSaveNicknameFor(TypeName typeName) {
    return typeName.getAndSaveNicknameIn(this);
  }

  @Override
  public String getAndSaveNicknameFor(TypeAlias alias) {
    if (!alias.needsImport()) {
      return alias.getNickname();
    }
    // Derive a short name if possible
    if (imports.containsKey(alias.getFullName())) {
      // Short name already there.
      return imports.get(alias.getFullName());
    }
    // TODO: Handle name clashes
    imports.put(alias.getFullName(), alias.getNickname());
    return alias.getNickname();
  }

  @Override
  public Map<String, String> getImports() {
    return FluentIterable.from(imports.keySet())
      .transform(new Function<String, String>() {
        @Override public String apply(String fullName) {
          int index = fullName.lastIndexOf('.');
          if (index < 0) {
            return null;
          } else {
            return fullName.substring(0, index);
          }
        }
      })
      .filter(Predicates.notNull())
      .toMap(Functions.<String>identity());
  }

}
