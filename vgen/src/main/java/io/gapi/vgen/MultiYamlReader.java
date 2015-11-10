package io.gapi.vgen;

import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.SimpleLocation;
import com.google.api.tools.framework.yaml.YamlReader;
import com.google.common.base.Preconditions;
import com.google.common.io.Files;
import com.google.protobuf.Message;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

public class MultiYamlReader {

  @Nullable public static Message read(DiagCollector collector, List<String> inputNames,
      List<String> inputs, Map<String, Message> supportedConfigTypes) {
    Preconditions.checkArgument(inputNames.size() == inputs.size(),
        "size() of inputNames and inputs not equal: %d != %d", inputNames.size(), inputs.size());
    Message.Builder messageBuilder = null;
    for (int i = 0; i < inputs.size(); i++) {
      String inputName = inputNames.get(i);
      String input = inputs.get(i);
      Message message = YamlReader.read(collector, inputName, input, supportedConfigTypes);
      if (message != null) {
        if (messageBuilder == null) {
          messageBuilder = message.toBuilder();
        } else {
          messageBuilder.mergeFrom(message);
        }
      }
    }

    if (messageBuilder == null) {
      return null;
    } else {
      return messageBuilder.build();
    }
  }

  @Nullable public static Message read(DiagCollector collector, List<File> files,
      Map<String, Message> supportedConfigTypes) {
    List<String> inputNames = new ArrayList<>();
    List<String> inputs = new ArrayList<>();
    for (File file : files) {
      inputNames.add(file.getName());
      try {
        String fileContent = Files.toString(file, Charset.forName("UTF8"));
        inputs.add(fileContent);
      } catch (IOException e) {
        collector.addDiag(Diag.error(SimpleLocation.TOPLEVEL,
            "Cannot read configuration file '%s': %s", file.getName(), e.getMessage()));
      }
    }
    if (collector.getErrorCount() > 0) {
      return null;
    } else {
      return read(collector, inputNames, inputs, supportedConfigTypes);
    }
  }

}
