/* Copyright 2017 Google Inc
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
package com.google.api.codegen.configgen.mergers;

import com.google.api.codegen.configgen.ListTransformer;
import com.google.api.codegen.configgen.StringPairTransformer;
import com.google.api.codegen.configgen.nodes.ConfigNode;
import com.google.api.codegen.configgen.nodes.FieldConfigNode;
import com.google.api.codegen.configgen.nodes.ListItemConfigNode;
import com.google.api.codegen.configgen.nodes.metadata.DefaultComment;
import java.util.Map;

public class CollectionMerger {
  private static final String COLLECTIONS_COMMENT =
      "A list of resource collection configurations.\n"
          + "Consists of a name_pattern and an entity_name.\n"
          + "The name_pattern is a pattern to describe the names of the resources of this "
          + "collection, using the platform's conventions for URI patterns. A generator may use "
          + "this to generate methods to compose and decompose such names. The pattern should use "
          + "named placeholders as in `shelves/{shelf}/books/{book}`; those will be taken as hints "
          + "for the parameter names of the generated methods. If empty, no name methods are "
          + "generated.\n"
          + "The entity_name is the name to be used as a basis for generated methods and classes.";

  public ConfigNode generateCollectionsNode(ConfigNode prevNode, Map<String, String> nameMap) {
    FieldConfigNode collectionsNode =
        new FieldConfigNode("collections").setComment(new DefaultComment(COLLECTIONS_COMMENT));
    prevNode.insertNext(collectionsNode);
    ListTransformer.generateList(
        nameMap.entrySet(),
        collectionsNode,
        new ListTransformer.ElementTransformer<Map.Entry<String, String>>() {
          @Override
          public ConfigNode generateElement(Map.Entry<String, String> entry) {
            return generateCollectionNode(entry.getKey(), entry.getValue());
          }
        });
    return collectionsNode;
  }

  private ConfigNode generateCollectionNode(String namePattern, String entityName) {
    ConfigNode collectionNode = new ListItemConfigNode();
    ConfigNode namePatternNode =
        StringPairTransformer.generateStringPair("name_pattern", namePattern);
    collectionNode.setChild(namePatternNode);
    ConfigNode entityNameNode = StringPairTransformer.generateStringPair("entity_name", entityName);
    namePatternNode.insertNext(entityNameNode);
    return collectionNode;
  }
}
