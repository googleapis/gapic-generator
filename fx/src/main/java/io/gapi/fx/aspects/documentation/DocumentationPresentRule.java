package io.gapi.fx.aspects.documentation;

import io.gapi.fx.aspects.ConfigAspectBase;
import io.gapi.fx.aspects.LintRule;
import io.gapi.fx.aspects.documentation.model.DocumentationUtil;
import io.gapi.fx.model.ProtoElement;
import io.gapi.fx.model.ProtoFile;
import com.google.common.base.Strings;

/**
 * Style rule checking whether documentation is present.
 */
class DocumentationPresentRule extends LintRule<ProtoElement> {

  DocumentationPresentRule(ConfigAspectBase aspect) {
    super(aspect, "presence", ProtoElement.class);
  }

  @Override public void run(ProtoElement elem) {
    if (elem instanceof ProtoFile) {
      // Nothing enforced for files.
      return;
    }
    String doc = DocumentationUtil.getDescription(elem);
    if (Strings.isNullOrEmpty(doc)) {
      warning(elem, "'%s' has no documentation, neither in IDL or config.", elem);
    }
  }
}
