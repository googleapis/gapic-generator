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
   * Bi-map from short names to PythonImport objects for imports.
   */
  private final BiMap<String, PythonImport> stringImports = HashBiMap.create();

  /** This constructor is for the main imports of a generated service file */
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
          PythonImport.create(
              ImportType.APP,
              method.getFile().getProto().getPackage(),
              PythonProtoElements.getPbFileName(method.getInputMessage())));
      for (Field field : method.getInputType().getMessageType().getMessageFields()) {
        MessageType messageType = field.getType().getMessageType();
        addImport(
            PythonImport.create(
                ImportType.APP,
                messageType.getFile().getProto().getPackage(),
                PythonProtoElements.getPbFileName(messageType)));
      }
    }
  }

  /** This constructor is used for doc messages. */
  public PythonImportHandler(ProtoFile file) {
    for (MessageType message : file.getMessages()) {
      for (Field field : message.getMessageFields()) {
        MessageType messageType = field.getType().getMessageType();
        // Don't include imports to messages in the same file.
        if (!messageType.getFile().equals(file)) {
          addImport(
              PythonImport.create(
                  ImportType.APP,
                  messageType.getFile().getProto().getPackage(),
                  PythonProtoElements.getPbFileName(messageType)));
        }
      }
    }
  }

  /**
   * This constructor is used for sample-gen where only the required fields of a method are needed
   * to be imported.
   */
  public PythonImportHandler(Iterable<Field> requiredFields) {
    for (Field field : requiredFields) {
      if (!field.getType().isMessage()) {
        continue;
      }
      MessageType messageType = field.getType().getMessageType();
      addImport(
          PythonImport.create(
              ImportType.APP,
              messageType.getFile().getProto().getPackage(),
              PythonProtoElements.getPbFileName(messageType)));
    }
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
      String module = elt.getFile().getProto().getPackage();
      String attribute = PythonProtoElements.getPbFileName(elt);
      path = getShortName(module, attribute);
    }

    path += Strings.isNullOrEmpty(prefix) ? "" : "." + prefix;
    path += "." + elt.getSimpleName();
    return path;
  }

  /*
   * Adds an import to the import maps.
   */
  private PythonImport addImport(PythonImport imp) {
    // No conflict
    if (stringImports.get(imp.shortName()) == null) {
      stringImports.put(imp.shortName(), imp);
      return imp;

      // Redundant import
    } else if (stringImports.get(imp.shortName()).importString().equals(imp.importString())) {
      return imp;

      // Conflict
    } else {
      String oldShortName = imp.shortName();
      PythonImport formerImp = stringImports.remove(oldShortName);

      PythonImport disambiguatedNewImp = imp.disambiguate();
      PythonImport disambiguatedOldImp = formerImp.disambiguate();

      // If we mangled both names, un-mangle the older one; otherwise we'll be in an infinite
      // mangling cycle.
      if (disambiguatedNewImp.shortName().equals(oldShortName + "_")
          && disambiguatedOldImp.shortName().equals(oldShortName + "_")) {
        disambiguatedOldImp = formerImp;
      }

      addImport(disambiguatedOldImp);
      return addImport(disambiguatedNewImp);
    }
  }

  // Helper methods to support generating imports from snippets for discovery fragment generation.
  // Some are written with overloads since snippet engine currently does not support varargs.

  public PythonImport addImport(ImportType type, String... names) {
    return addImport(PythonImport.create(type, names));
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

  /** Returns the name for a module for a module and attribute stored in the input handler. */
  public String getShortName(String module, String attribute) {
    for (PythonImport pyImport : stringImports.inverse().keySet()) {
      if (pyImport.attributeName().equals(attribute) && pyImport.moduleName().equals(module)) {
        return pyImport.shortName();
      }
    }
    return attribute;
  }
}
