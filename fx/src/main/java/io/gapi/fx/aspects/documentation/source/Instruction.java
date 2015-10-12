package io.gapi.fx.aspects.documentation.source;

import io.gapi.fx.aspects.documentation.model.PageAttribute;
import io.gapi.fx.aspects.documentation.model.ResourceAttribute;
import io.gapi.fx.model.Diag;
import io.gapi.fx.model.DiagCollector;
import io.gapi.fx.model.Element;
import io.gapi.fx.model.Location;
import io.gapi.fx.model.MessageType;

/**
 * Represents Docgen instructions other than file inclusion:
 * (== code arg ==)
 */
public class Instruction extends ContentElement {

  private static final String PAGE_INSTRUCTION = "page";
  private static final String SUPPRESS_WARNING_INSTRUCTION = "suppress_warning";
  private static final String RESOURCE_INSTRUCTION = "resource_for";

  private final String code;
  private final String arg;

  public Instruction(String code, String arg, int startIndex, int endIndex,
      DiagCollector diagCollector, Location sourceLocation) {
    super(startIndex, endIndex, diagCollector, sourceLocation);
    this.code = code.trim();
    this.arg = arg.trim();
  }

  /**
   * Returns the instruction code
   */
  public String getCode() {
    return code;
  }

  /**
   * Returns the instruction argument.
   */
  public String getArg() {
    return arg;
  }

  /**
   * Return the content (empty for instruction).
   */
  @Override public String getContent() {
    return "";
  }

  /**
   * Evaluate the instruction in context of given element.
   */
  public void evalute(Element element) {
    switch (code) {
      case PAGE_INSTRUCTION:
        element.putAttribute(PageAttribute.KEY, PageAttribute.create(arg));
        break;
      case SUPPRESS_WARNING_INSTRUCTION:
        element.getModel().addSupressionDirective(element, arg);
        break;
      case RESOURCE_INSTRUCTION:
        if (!(element instanceof MessageType)) {
          element.getModel().addDiag(Diag.error(element.getLocation(),
              "resource instruction must be associated with a message declaration, but '%s' "
              + "is not a message.",
              element.getFullName()));
        } else {
          element.addAttribute(ResourceAttribute.KEY, ResourceAttribute.create(arg));
        }
        break;
      default:
        element.getModel().addDiag(Diag.error(element.getLocation(),
            "documentation instruction '%s' unknown.", code));
    }
  }
}
