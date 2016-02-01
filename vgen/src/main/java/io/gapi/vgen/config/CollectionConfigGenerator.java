package io.gapi.vgen.config;

import com.google.api.tools.framework.aspects.http.model.HttpAttribute.FieldSegment;
import com.google.api.tools.framework.model.Interface;
import com.google.common.collect.Lists;

import io.gapi.vgen.Resources;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Class for collection config generator.
 */
public class CollectionConfigGenerator {

  private static final String CONFIG_KEY_NAME_PATTERN = "name_pattern";
  private static final String CONFIG_KEY_METHOD_BASE_NAME = "method_base_name";

  public List<Object> generate(Interface service) {
    List<Object> output = new LinkedList<Object>();

    Iterable<FieldSegment> segments =
        Resources.getFieldSegmentsFromHttpPaths(service.getMethods());
    for (FieldSegment segment : segments) {
      Map<String, Object> collectionMap = new LinkedHashMap<String, Object>();
      collectionMap.put(CONFIG_KEY_NAME_PATTERN, Resources.templatize(segment));
      collectionMap.put(CONFIG_KEY_METHOD_BASE_NAME, getMethodBaseName(segment));
      output.add(collectionMap);
    }
    return output;
  }

  private String getMethodBaseName(FieldSegment segment) {
    // TODO(shinfan): Consider finding a better way to determine the name if possible.
    List<String> params =
        Lists.newArrayList(Resources.getParamsForResourceNameWildcards(segment));
    return params.get(params.size() - 1);
  }
}
