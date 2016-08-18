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
package com.google.api.codegen.py;

import com.google.api.codegen.py.PythonImport.ImportType;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.MessageType;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.ProtoElement;
import com.google.api.tools.framework.model.ProtoFile;
import com.google.common.base.Strings;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PythonImportHandler {

  /**
   * Bi-map from short names to PythonImport objects for imports. Should only be modified through
   * addImport() to maintain the invariant that elements of this map are in 1:1 correspondence with
   * those in fileImports.
   */
  private final BiMap<String, PythonImport> stringImports = HashBiMap.create();

  /**
   * Bi-map from proto files to short names for imports. Should only be modified through
   * addImport() to maintain the invariant that elements of this map are in 1:1 correspondence with
   * those in stringImports.
   */
  private final BiMap<ProtoFile, String> fileImports = HashBiMap.create();

  public PythonImportHandler(Interface service) {
    // Add non-service-specific imports.
    addImportStandard("json");
    addImportStandard("os");
    addImportStandard("pkg_resources");
    addImportStandard("platform");
    addImportExternal("google.gax");

    addImportExternal("google.gax", "api_callable");
    addImportExternal("google.gax", "config");
    addImportExternal("google.gax", "path_template");

    // Add method request-type imports.
    for (Method method : service.getMethods()) {
      addImport(
          method.getInputMessage().getFile(),
          PythonImport.create(
              ImportType.APP,
              method.getFile().getProto().getPackage(),
              PythonProtoElements.getPbFileName(method.getInputMessage())));
      for (Field field : method.getInputType().getMessageType().getMessageFields()) {
        MessageType messageType = field.getType().getMessageType();
        addImport(
            messageType.getFile(),
            PythonImport.create(
                ImportType.APP,
                messageType.getFile().getProto().getPackage(),
                PythonProtoElements.getPbFileName(messageType)));
      }
    }
  }

  public PythonImportHandler(ProtoFile file) {
    for (MessageType message : file.getMessages()) {
      for (Field field : message.getMessageFields()) {
        MessageType messageType = field.getType().getMessageType();
        // Don't include imports to messages in the same file.
        if (!messageType.getFile().equals(file)) {
          addImport(
              messageType.getFile(),
              PythonImport.create(
                  ImportType.APP,
                  messageType.getFile().getProto().getPackage(),
                  PythonProtoElements.getPbFileName(messageType)));
        }
      }
    }
  }

  public PythonImportHandler(Method method) {
    addImport(
        method.getInputMessage().getFile(),
        PythonImport.create(
            ImportType.APP,
            method.getFile().getProto().getPackage(),
            PythonProtoElements.getPbFileName(method.getInputMessage())));
  }

  // Independent import handler to support fragment generation from discovery sources
  public PythonImportHandler() {}

  /**
   * Returns the path to a proto element. If fullyQualified is false, returns the fully qualified
   * path.
   *
   * For example, with message `Hello.World` under import `hello`, if fullyQualified is true: for
   * `path.to.hello.Hello.World`, it returns `path.to.hello.Hello.World` false: for
   * `path.to.hello.Hello.World`, it returns `hello.Hello.World`
   */
  public String elementPath(ProtoElement elt, boolean fullyQualified) {
    String prefix = PythonProtoElements.prefixInFile(elt);
    String path;

    if (fullyQualified) {
      path = elt.getFile().getProto().getPackage() + "." + PythonProtoElements.getPbFileName(elt);
    } else {
      path = fileToModule(elt.getFile());
    }

    if (Strings.isNullOrEmpty(path)) {
      // path is either empty or the prefix string.
      path = prefix;
    } else {
      // If path isn't empty:
      if (!Strings.isNullOrEmpty(prefix)) {
        // If prefix isn't empty, append it to path.
        path += "." + prefix;
      }
    }
    path += "." + elt.getSimpleName();
    return path;
  }

  /*
   * Adds an import to the import maps.
   */
  private PythonImport addImport(ProtoFile file, PythonImport imp) {
    // No conflict
    if (stringImports.get(imp.shortName()) == null) {
      fileImports.put(file, imp.shortName());
      stringImports.put(imp.shortName(), imp);
      return imp;

      // Redundant import
    } else if (stringImports.get(imp.shortName()).importString().equals(imp.importString())) {
      return imp;

      // Conflict
    } else {
      String oldShortName = imp.shortName();
      PythonImport formerImp = stringImports.remove(oldShortName);
      ProtoFile formerFile = fileImports.inverse().remove(oldShortName);

      PythonImport disambiguatedNewImp = imp.disambiguate();
      PythonImport disambiguatedOldImp = formerImp.disambiguate();

      // If we mangled both names, un-mangle the older one; otherwise we'll be in an infinite
      // mangling cycle.
      if (disambiguatedNewImp.shortName().equals(oldShortName + "_")
          && disambiguatedOldImp.shortName().equals(oldShortName + "_")) {
        disambiguatedOldImp = formerImp;
      }

      addImport(formerFile, disambiguatedOldImp);
      return addImport(file, disambiguatedNewImp);
    }
  }

  // Helper methods to support generating imports from snippets for discovery fragment generation.
  // Some are written with overloads since snippet engine currently does not support varargs.

  public PythonImport addImport(ImportType type, String... names) {
    return addImport(null, PythonImport.create(type, names));
  }

  public String addImportStandard(String moduleName) {
    return addImport(ImportType.STDLIB, moduleName).shortName();
  }

  public String addImportStandard(String moduleName, String attributeName) {
    return addImport(ImportType.STDLIB, moduleName, attributeName).shortName();
  }

  public String addImportExternal(String moduleName) {
    return addImport(ImportType.THIRD_PARTY, moduleName).shortName();
  }

  public String addImportExternal(String moduleName, String attributeName) {
    return addImport(ImportType.THIRD_PARTY, moduleName, attributeName).shortName();
  }

  public String addImportLocal(String moduleName, String attributeName) {
    return addImport(ImportType.APP, moduleName, attributeName).shortName();
  }

  /**
   * Calculate the imports map and return a sorted set of python import output strings.
   */
  public List<String> calculateImports() {
    // Order by import type, then lexicographically
    List<String> stdlibResult = new ArrayList<>();
    List<String> thirdPartyResult = new ArrayList<>();
    List<String> appResult = new ArrayList<>();
    for (PythonImport protoImport : stringImports.values()) {
      switch (protoImport.type()) {
        case STDLIB:
          stdlibResult.add(protoImport.importString());
          break;
        case THIRD_PARTY:
          thirdPartyResult.add(protoImport.importString());
          break;
        case APP:
          appResult.add(protoImport.importString());
          break;
      }
    }
    Collections.sort(stdlibResult);
    Collections.sort(thirdPartyResult);
    Collections.sort(appResult);

    List<String> all = new ArrayList<>();
    if (stdlibResult.size() > 0) {
      all.addAll(stdlibResult);
      all.add("");
    }
    if (thirdPartyResult.size() > 0) {
      all.addAll(thirdPartyResult);
      all.add("");
    }
    all.addAll(appResult);
    return all;
  }

  public String fileToModule(ProtoFile file) {
    if (fileImports.containsKey(file)) {
      return fileImports.get(file);
    } else {
      return "";
    }
  }
}
