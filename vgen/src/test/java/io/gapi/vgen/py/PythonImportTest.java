package io.gapi.vgen.py;

import com.google.common.truth.Truth;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link PythonImportHandler} and {@link PythonImport}
 */
@RunWith(JUnit4.class)
public class PythonImportTest {

  @Test
  public void testImport_simple() {
    PythonImport imp = PythonImport.create("foo", PythonImport.ImportType.STDLIB);
    Truth.assertThat(imp.importString()).isEqualTo("import foo");
    Truth.assertThat(imp.shortName()).isEqualTo("foo");
  }

  @Test
  public void testImport_moduleName() {
    PythonImport imp = PythonImport.create("foo", "bar", PythonImport.ImportType.STDLIB);
    Truth.assertThat(imp.importString()).isEqualTo("from foo import bar");
    Truth.assertThat(imp.shortName()).isEqualTo("bar");
  }

  @Test
  public void testImport_localName() {
    PythonImport imp = PythonImport.create("foo", "bar", "baz", PythonImport.ImportType.STDLIB);
    Truth.assertThat(imp.importString()).isEqualTo("from foo import bar as baz");
    Truth.assertThat(imp.shortName()).isEqualTo("baz");
  }

  @Test
  public void testImport_disambiguate_noChange() {
    PythonImport imp =
         PythonImport.create("foo.bar", PythonImport.ImportType.STDLIB).disambiguate();
    Truth.assertThat(imp.importString()).isEqualTo("import foo.bar");
    Truth.assertThat(imp.shortName()).isEqualTo("foo.bar");
  }

  @Test
  public void testImport_disambiguate_mangle() {
    PythonImport imp =
        PythonImport.create("foo", "bar", "baz", PythonImport.ImportType.STDLIB).disambiguate();
   Truth.assertThat(imp.importString()).isEqualTo("from foo import bar as baz_");
   Truth.assertThat(imp.shortName()).isEqualTo("baz_");
  }

  @Test
  public void testImport_disambiguate_noModuleName() {
    PythonImport imp =
        PythonImport.create("foo", "bar", PythonImport.ImportType.STDLIB).disambiguate();
   Truth.assertThat(imp.importString()).isEqualTo("import foo.bar");
   Truth.assertThat(imp.shortName()).isEqualTo("foo.bar");
  }

  @Test
  public void testImport_disambiguate_moduleName() {
    PythonImport imp =
        PythonImport.create("foo.bar", "baz", PythonImport.ImportType.STDLIB).disambiguate();
   Truth.assertThat(imp.importString()).isEqualTo("from foo import bar.baz");
   Truth.assertThat(imp.shortName()).isEqualTo("bar.baz");
  }
}

