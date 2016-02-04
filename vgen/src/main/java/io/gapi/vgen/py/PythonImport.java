package io.gapi.vgen.py;

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
   * Create a Python import with the given module, attribute and local names.
   */
  public static PythonImport create(String moduleName, String attributeName, String localName,
      ImportType type) {
    return new AutoValue_PythonImport(
        moduleName, attributeName, localName, type);
  }

  /*
   * Create a Python import with then given module and attribute names.
   */
  public static PythonImport create(String moduleName, String attributeName, ImportType type) {
    return create(moduleName, attributeName, "", type);
  }

  /*
   * Create a Python import with the given attribute name.
   */
  public static PythonImport create(String attributeName, ImportType type) {
    return create("", attributeName, "", type);
  }

  public String importString() {
    return (Strings.isNullOrEmpty(moduleName()) ? "" : "from " + moduleName() + " ") + "import "
        + attributeName() + (Strings.isNullOrEmpty(localName()) ? "" : " as " + localName());
  }

  /*
   * The Python symbol that can be used to reference the package/module being imported
   */
  public String shortName() {
    return Strings.isNullOrEmpty(localName()) ? attributeName() : localName();
  }

  /*
   * Attempts to disambiguate this import. If this import uses a local name, mangles the local
   * name; if this import uses the attribute name, moves a package into the attribute name.
   * Returns the import unchanged if the import cannot be disambiguated through either of these
   * strategies.
   * E.g.,
   *   "from foo import bar as baz" ====> "from foo import bar as baz_"
   *   "from foo.bar import baz" ====> "from foo import bar.baz"
   *   "import foo.bar" ====> "import foo.bar"
   */
  public PythonImport disambiguate() {

    // Package disambiguation
    if (Strings.isNullOrEmpty(localName())) {

      // Case: import foo.bar (cannot disambiguate)
      if (Strings.isNullOrEmpty(moduleName())) {
        return this;

      } else {
        int lastDot = moduleName().lastIndexOf('.');

        // Case: from foo import bar (only one package in moduleName())
        if (lastDot == -1) {
          return PythonImport.create(moduleName() + "." + attributeName(), type());

        // Case: from foo.bar import baz
        } else {
          return PythonImport.create(moduleName().substring(0, lastDot),
              moduleName().substring(lastDot + 1) + "." + attributeName(),
              type());
        }
      }

    // Mangling disambiguation
    } else {
      return PythonImport.create(moduleName(), attributeName(), localName() + "_", type());
    }

  }

}
