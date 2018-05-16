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
package com.google.api.codegen.util.csharp;

import com.google.api.codegen.util.NamePath;
import com.google.api.codegen.util.TypeAlias;
import com.google.api.codegen.util.TypeName;
import com.google.api.codegen.util.TypeTable;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class CSharpTypeTable implements TypeTable {

  // Constants for aliases that are used outside of this class.
  public static final String ALIAS_SYSTEM = "sys";
  public static final String ALIAS_SYSTEM_THREADING = "st";
  public static final String ALIAS_SYSTEM_COLLECTIONS_GENERIC = "scg";
  public static final String ALIAS_GAX = "gax";
  public static final String ALIAS_GAX_GRPC = "gaxgrpc";

  private static final Map<String, String> wellKnownAliases =
      ImmutableMap.<String, String>builder()
          .put("Google.Api.Gax", ALIAS_GAX)
          .put("Google.Api.Gax.Grpc", ALIAS_GAX_GRPC)
          .put("Google.Api.Gax.ResourceNames", "gaxres")
          .put("Google.Protobuf", "pb")
          .put("Google.Protobuf.WellKnownTypes", "pbwkt")
          .put("Grpc.Core", "grpccore")
          .put("System", ALIAS_SYSTEM)
          .put("System.Collections", "sc")
          .put("System.Collections.Generic", ALIAS_SYSTEM_COLLECTIONS_GENERIC)
          .put("System.Collections.ObjectModel", "sco")
          .put("System.Linq", "linq")
          .put("System.Threading", ALIAS_SYSTEM_THREADING)
          .put("System.Threading.Tasks", "stt")
          .put("Google.LongRunning", "lro")
          .put("Google.Cloud.Iam.V1", "iam")
          .build();

  private final Map<String, String> dynamicAliases = new HashMap<String, String>();

  private final String implicitPackageName;
  private final CSharpAliasMode aliasMode;
  // Full name to nickname map
  private final Map<String, TypeAlias> imports = new HashMap<>();

  public CSharpTypeTable(String implicitPackageName, CSharpAliasMode aliasMode) {
    this.implicitPackageName = implicitPackageName;
    this.aliasMode = aliasMode;
  }

  @Override
  public TypeName getTypeName(String fullName) {
    int firstGenericOpenIndex = fullName.indexOf('<');
    if (firstGenericOpenIndex >= 0) {
      int lastGenericCloseIndex = fullName.lastIndexOf('>');
      String containerTypeName = fullName.substring(0, firstGenericOpenIndex);
      List<String> genericParamNames =
          Splitter.on(',')
              .trimResults()
              .splitToList(fullName.substring(firstGenericOpenIndex + 1, lastGenericCloseIndex));
      return getContainerTypeName(
          containerTypeName, genericParamNames.toArray(new String[genericParamNames.size()]));
    }
    int lastDotIndex = fullName.lastIndexOf('.');
    if (lastDotIndex < 0) {
      return new TypeName(fullName, fullName);
    }
    String shortTypeName = fullName.substring(lastDotIndex + 1);
    String namespace = fullName.substring(0, lastDotIndex);
    switch (aliasMode) {
      case Global:
        // Alias the type namespace, unless it's in this (or parent) namespace.
        // Use well-known alias if possible, otherwise use a dynamically created alias.
        if (!implicitPackageName.startsWith(namespace)) {
          String wellKnownAlias = wellKnownAliases.getOrDefault(namespace, null);
          if (wellKnownAlias != null) {
            shortTypeName = wellKnownAlias + "::" + shortTypeName;
          } else {
            String alias = dynamicAliases.getOrDefault(namespace, null);
            if (alias == null) {
              int[] chars =
                  Splitter.on('.')
                      .splitToList(namespace)
                      .stream()
                      .mapToInt(part -> part.charAt(0))
                      .toArray();
              String aliasNoSuffix = new String(chars, 0, chars.length).toLowerCase();
              for (int i = 0; ; i++) {
                alias = aliasNoSuffix + (i == 0 ? "" : Integer.toString(i));
                if (!dynamicAliases.containsKey(alias) && !wellKnownAliases.containsKey(alias)) {
                  break;
                }
              }
              dynamicAliases.put(namespace, alias);
            }
            shortTypeName = alias + "::" + shortTypeName;
          }
        }
        break;
      case MessagesOnly:
        // Aliase the type namespace if:
        // * It's a type in the API namespace; and
        // * It's a type that shares a name with an imported type; and
        // * The shared name is not this type itself (e.g. when generating Google.LongRunning)
        if (implicitPackageName.startsWith(namespace + ".")) {
          List<String> namespaceList =
              CSharpImports.typeNamesToAlias.getOrDefault(shortTypeName, null);
          if (namespaceList != null && !namespaceList.contains(namespace)) {
            shortTypeName = "apis::" + shortTypeName;
          }
        }
        break;
      default:
        throw new UnsupportedOperationException("Unrecognised aliasMode: " + aliasMode);
    }
    return new TypeName(fullName, shortTypeName);
  }

  @Override
  public TypeName getTypeNameInImplicitPackage(String shortName) {
    String fullName = implicitPackageName + "." + shortName;
    return new TypeName(fullName, shortName);
  }

  @Override
  public NamePath getNamePath(String fullName) {
    return NamePath.dotted(fullName);
  }

  @Override
  public TypeName getContainerTypeName(String containerFullName, String... elementFullNames) {
    TypeName containerTypeName = getTypeName(containerFullName);
    TypeName[] elementTypeNames = new TypeName[elementFullNames.length];
    for (int i = 0; i < elementTypeNames.length; i++) {
      elementTypeNames[i] = getTypeName(elementFullNames[i]);
    }
    String argPattern = Joiner.on(", ").join(Collections.nCopies(elementTypeNames.length, "%i"));
    String pattern = "%s<" + argPattern + ">";
    return new TypeName(
        containerTypeName.getFullName(),
        containerTypeName.getNickname(),
        pattern,
        elementTypeNames);
  }

  @Override
  public TypeTable cloneEmpty() {
    return new CSharpTypeTable(implicitPackageName, aliasMode);
  }

  @Override
  public TypeTable cloneEmpty(String packageName) {
    return new CSharpTypeTable(packageName, aliasMode);
  }

  private String resolveInner(String name) {
    return name.replace('+', '.');
  }

  @Override
  public String getAndSaveNicknameFor(String fullName) {
    return resolveInner(getAndSaveNicknameFor(getTypeName(fullName)));
  }

  @Override
  public String getAndSaveNicknameFor(TypeName typeName) {
    return resolveInner(typeName.getAndSaveNicknameIn(this));
  }

  @Override
  public String getAndSaveNicknameFor(TypeAlias alias) {
    if (!alias.needsImport()) {
      return alias.getNickname();
    }
    // Derive a short name if possible
    if (imports.containsKey(alias.getFullName())) {
      // Short name already there.
      return imports.get(alias.getFullName()).getNickname();
    }
    // TODO: Handle name clashes
    imports.put(alias.getFullName(), alias);
    return alias.getNickname();
  }

  @Override
  public Map<String, TypeAlias> getImports() {
    int lastDotPos = implicitPackageName.lastIndexOf('.');
    String parentNamespace = implicitPackageName.substring(0, Math.max(0, lastDotPos));
    SortedMap<String, TypeAlias> result = new TreeMap<>();
    for (String fullName : imports.keySet()) {
      int index = fullName.lastIndexOf('.');
      if (index >= 0) {
        String using = fullName.substring(0, index);
        if (!implicitPackageName.equals(using)) {
          switch (aliasMode) {
            case Global:
              String alias = wellKnownAliases.getOrDefault(using, null);
              if (alias == null) {
                alias = dynamicAliases.getOrDefault(using, "");
              }
              result.put(using, TypeAlias.create(using, alias));
              break;
            case MessagesOnly:
              if (parentNamespace.equals(using)) {
                result.put(using, TypeAlias.create(using, "apis"));
              } else {
                result.put(using, TypeAlias.create(using, "")); // Value isn't used
              }
              break;
            default:
              throw new UnsupportedOperationException("Unrecognised aliasMode: " + aliasMode);
          }
        }
      }
    }
    return result;
  }

  @Override
  public Map<String, TypeAlias> getAllImports() {
    return new TreeMap<>(imports);
  }

  @Override
  public String getAndSaveNicknameForInnerType(
      String containerFullName, String innerTypeShortName) {
    throw new UnsupportedOperationException("getAndSaveNicknameForInnerType not supported by C#");
  }
}
