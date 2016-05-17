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

import com.google.auto.value.AutoValue;
import com.google.common.base.Strings;

/*
 * Class to represent one python import.
 */
@AutoValue
abstract class PythonImport {

  public abstract String moduleName();

  public abstract String attributeName();

  public abstract String localName();

  public enum ImportType {
    STDLIB, // From the Python standard library
    THIRD_PARTY, // Not specific to this application and not from the standard library
    APP // Specific to this application
  }

  public abstract ImportType type();

  /*
   * Create a Python import of the given type, specifying (attribute), (module, attribute), or
   * (module, attribute, local) names.
   */
  public static PythonImport create(ImportType type, String... names) {
    String moduleName = "";
    String attributeName = "";
    String localName = "";
    switch (names.length) {
      case 3:
        localName = names[2];
        // fall through
      case 2:
        moduleName = names[0];
        attributeName = names[1];
        break;
      case 1:
        attributeName = names[0];
        break;
      default:
        // fall through
    }
    return new AutoValue_PythonImport(moduleName, attributeName, localName, type);
  }

  public String importString() {
    return (Strings.isNullOrEmpty(moduleName()) ? "" : "from " + moduleName() + " ")
        + "import "
        + attributeName()
        + (Strings.isNullOrEmpty(localName()) ? "" : " as " + localName());
  }

  /*
   * The Python symbol that can be used to reference the package/module being imported
   */
  public String shortName() {
    return Strings.isNullOrEmpty(localName()) ? attributeName() : localName();
  }

  /* Attempts to disambiguate an import by changing the shortName by applying a number of
   * strategies in sequence. If a strategy succeeds in modifying the shortName corresponding to the
   * import, subsequent strategies are not attempted. In the order that they are attempted,
   * these strategies are:
   *
   * Add a localName if one is not present, set equal to the shortName with '.' replaced by '_':
   *   "import foo" ====> "import foo as foo"
   *   "from foo import bar" ====> "from foo import bar as bar"
   *   "import foo.bar" ====> "import foo.bar as foo_bar"
   *
   * Move the highest-level single package name not already present in the alias into the alias:
   *   "from foo import bar as baz" ====> "from foo import bar as foo_baz"
   *   "from foo.bar import baz as bar_baz" ====> "from foo.bar import baz as foo_bar_baz"
   *
   * Mangle with a single underscore:
   *   "import foo as bar" ====> "import foo as bar_"
   *   "from foo import bar as foo_bar" ====> "from foo import bar as foo_bar_"
   *   "import foo as bar_" ====> "import foo as bar__"
   */
  public PythonImport disambiguate() {
    String oldShortName = shortName();
    PythonImport disambiguation = this;

    // Add local name
    if (Strings.isNullOrEmpty(localName())) {
      disambiguation =
          PythonImport.create(
              disambiguation.type(), disambiguation.moduleName(),
              disambiguation.attributeName(), disambiguation.shortName().replace('.', '_'));

      if (!disambiguation.shortName().equals(oldShortName)) {
        return disambiguation;
      }
    }

    // Move a package into local name
    String[] moduleNamePackages = disambiguation.moduleName().split("\\.");
    String localNamePackagePrefix = moduleNamePackages[moduleNamePackages.length - 1];
    int i = moduleNamePackages.length - 2;
    boolean found = disambiguation.localName().startsWith(localNamePackagePrefix);

    if (!found) {
      found = true;
      do {
        if (i < 0) {
          found = false;
          break;
        }
        localNamePackagePrefix = moduleNamePackages[i] + "_" + localNamePackagePrefix;
        i--;
      } while (!disambiguation.localName().startsWith(localNamePackagePrefix));
    }

    // Move a first package
    if (!found) {
      return PythonImport.create(
          disambiguation.type(),
          disambiguation.moduleName(),
          disambiguation.attributeName(),
          moduleNamePackages[moduleNamePackages.length - 1] + "_" + disambiguation.shortName());

      // Move another package
    } else if (found && i >= 0) {
      return PythonImport.create(
          disambiguation.type(),
          disambiguation.moduleName(),
          disambiguation.attributeName(),
          moduleNamePackages[i] + "_" + disambiguation.shortName());

      // Mangle
    } else {
      return PythonImport.create(
          disambiguation.type(),
          disambiguation.moduleName(),
          disambiguation.attributeName(),
          disambiguation.shortName() + "_");
    }
  }
}
