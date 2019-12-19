package com.google.api.codegen.bazel;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

class ArgsParser {
  private final Map<String, String> parsedArgs = new HashMap<>();

  ArgsParser(String[] args) {
    for (String arg : args) {
      String[] argNameVal = arg.split("=");
      if (argNameVal.length != 2) {
        System.out.println("WARNING: Ignoring unrecognized argument: " + arg);
        continue;
      }
      parsedArgs.put(argNameVal[0], argNameVal[1].trim());
    }

    List<String> required = Arrays.asList("--src", "--dest");
    if (!parsedArgs.keySet().containsAll(required)) {
      String msg =
          "ERROR: Not all of the required arguments are spcified. "
              + "The required arguments are: "
              + required;
      System.out.println(msg);
      throw new IllegalArgumentException(msg);
    }
  }

  ApisVisitor createApisVisitor(ApisVisitor.FileWriter fileWriter) throws IOException {
    String gapicApiTemplPath = parsedArgs.get("--gapic_api_templ");
    String rootApiTemplPath = parsedArgs.get("--root_api_templ");
    String rawApiTempl = parsedArgs.get("--raw_api_templ");

    return new ApisVisitor(
        Paths.get(parsedArgs.get("--src")).normalize(),
        Paths.get(parsedArgs.get("--dest")).normalize(),
        gapicApiTemplPath == null
            ? readResource("BUILD.bazel.gapic_api.mustache")
            : ApisVisitor.readFile(gapicApiTemplPath),
        rootApiTemplPath == null
            ? readResource("BUILD.bazel.root_api.mustache")
            : ApisVisitor.readFile(rootApiTemplPath),
        rawApiTempl == null
            ? readResource("BUILD.bazel.raw_api.mustache")
            : ApisVisitor.readFile(rawApiTempl),
        fileWriter);
  }

  private String readResource(String resourcename) {
    return new Scanner(getClass().getResourceAsStream(resourcename), "UTF-8")
        .useDelimiter("\\A")
        .next();
  }
}
