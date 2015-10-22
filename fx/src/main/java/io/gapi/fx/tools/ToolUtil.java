package io.gapi.fx.tools;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;
import com.google.protobuf.Message;

import io.gapi.fx.model.Diag;
import io.gapi.fx.model.Model;
import io.gapi.fx.model.SimpleLocation;
import io.gapi.fx.snippet.Doc;
import io.gapi.fx.snippet.Doc.AnsiColor;
import io.gapi.fx.yaml.YamlReader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/**
 * Utilities for tools.
 */
public class ToolUtil {

  // Constants used in sanitizeSourceFiles() method.
  private static final String GENFILES_TOKEN = "/genfiles/";
  private static final String BLAZEOUT_PREFIX = "blaze-out/";


  /**
   * Writes a set of files with directory structure to a .jar. The content is a map from
   * file path to one of {@link Doc}, {@link String}, or {@code byte[]}.
   */
  public static void writeJar(Map<String, ?> content, String outputName)
      throws IOException {
    OutputStream outputStream = new FileOutputStream(outputName);
    JarOutputStream jarFile = new JarOutputStream(outputStream);
    OutputStreamWriter writer = new OutputStreamWriter(jarFile);
    try {
      for (Map.Entry<String, ?> entry : content.entrySet()) {
        jarFile.putNextEntry(new JarEntry(entry.getKey()));
        Object value = entry.getValue();
        if (value instanceof Doc) {
          writer.write(((Doc) value).prettyPrint());
          writer.flush();
        } else if (value instanceof String) {
          writer.write((String) value);
          writer.flush();
        } else if (value instanceof byte[]) {
          jarFile.write((byte[]) value);
        } else {
          throw new IllegalArgumentException("Expected one of Doc, String, or byte[]");
        }
        jarFile.closeEntry();
      }
    } finally {
      writer.close();
      jarFile.close();
    }
  }

  /**
   * Writes a content object into a set of output files. The content is one of {@link Doc},
   * {@link String} or {@code byte[]}.
   */
  public static void writeFiles(Map<String, ?> content, String baseName)
      throws IOException {

    for (Map.Entry<String, ?> entry : content.entrySet()) {
      File outputFile = Strings.isNullOrEmpty(baseName) ? new File(entry.getKey())
          : new File(baseName, entry.getKey());
      outputFile.getParentFile().mkdirs();
      OutputStream outputStream = new FileOutputStream(outputFile);
      OutputStreamWriter writer = new OutputStreamWriter(outputStream);
      try {
        Object value = entry.getValue();
        if (value instanceof Doc) {
          writer.write(((Doc) value).prettyPrint());
          writer.flush();
        } else if (value instanceof String) {
          writer.write((String) value);
          writer.flush();
        } else if (value instanceof byte[]) {
          outputStream.write((byte[]) value);
          outputStream.flush();
        } else {
          throw new IllegalArgumentException("Expected one of Doc, String, or byte[]");
        }
      } finally {
        writer.close();
      }
    }
  }

  /**
   * Report errors and warnings.
   */
  public static void reportDiag(Model model, boolean colored) {
    for (Diag diag : model.getDiags()) {
      System.err.println(diagToString(diag, colored));
    }
  }

  /**
   * Produce a string for the diagnosis, with optional coloring.
   */
  public static String diagToString(Diag diag, boolean colored) {
    Doc text;
    switch (diag.getKind()) {
      case ERROR:
        text = Doc.text("ERROR: ");
        if (colored) {
          text = Doc.color(AnsiColor.RED, text);
        }
        break;
      case WARNING:
        text = Doc.text("WARNING: ");
        if (colored) {
          text = Doc.color(AnsiColor.YELLOW, text);
        }
        break;
      default:
        text = Doc.text("HINT:");
        break;
    }
    text = text.add(Doc.text(diag.getLocation().getDisplayString())).add(Doc.text(": "))
        .add(Doc.text(diag.getMessage()));
    return text.toString();
  }

  /**
   * Sanitize the sources list which might include blaze-generated proto file.
   */
  public static ImmutableList<String> sanitizeSourceFiles(Iterable<String> sources) {
    ImmutableList.Builder<String> sourcesBuilder = new ImmutableList.Builder<>();
    for (String source : sources) {
      // TODO(wgg): MIGRATION
      if (source.startsWith(BLAZEOUT_PREFIX) && source.contains(GENFILES_TOKEN)) {
        // For blaze-generated proto source, keep the string after "/genfiles/" as the proto source
        // file name.
        sourcesBuilder.add(
          source.substring(source.indexOf(GENFILES_TOKEN) + GENFILES_TOKEN.length()));
      } else {
        sourcesBuilder.add(source);
      }
    }
    return sourcesBuilder.build();
  }

  /**
   * Sets up the model configs, reading them from Yaml files and attaching to the model.
   */
  public static void setupModelConfigs(Model model, List<String> configs) {
    ImmutableList.Builder<Message> builder = ImmutableList.builder();

    for (String fileName : configs) {
      File file = model.findDataFile(fileName);
      if (file == null) {
        model.addDiag(Diag.error(SimpleLocation.TOPLEVEL,
            "Cannot find configuration file '%s'.", fileName));
        continue;
      }
      try {
        Message message = YamlReader.read(model, fileName, Files.toString(new File(fileName),
            Charset.forName("UTF8")));
        if (message != null) {
          builder.add(message);
        }
      } catch (IOException e) {
        model.addDiag(Diag.error(SimpleLocation.TOPLEVEL,
            "Cannot read configuration file '%s': %s",
            fileName, e.getMessage()));
      }
    }
    if (model.getErrorCount() > 0) {
      return;
    }
    model.setConfigs(builder.build());
  }
}
