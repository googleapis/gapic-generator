package io.gapi.fx.aspects.documentation;

import com.google.api.Documentation;
import com.google.api.DocumentationRule;
import com.google.api.Page;
import com.google.api.Service;
import com.google.api.Service.Builder;
import io.gapi.fx.aspects.RuleBasedConfigAspect;
import io.gapi.fx.aspects.documentation.model.DocumentationPagesAttribute;
import io.gapi.fx.aspects.documentation.model.ElementDocumentationAttribute;
import io.gapi.fx.aspects.documentation.model.PageAttribute;
import io.gapi.fx.model.Model;
import io.gapi.fx.model.ProtoElement;
import io.gapi.fx.model.SimpleLocation;
import io.gapi.fx.model.SymbolTable;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * Configuration aspect for documentation.
 */
public class DocumentationConfigAspect
    extends RuleBasedConfigAspect<DocumentationRule, ElementDocumentationAttribute> {

  public static DocumentationConfigAspect create(Model model) {
    return new DocumentationConfigAspect(model);
  }

  private static final List<Page> EMPTY_PAGES = ImmutableList.of();

  private final DocumentationProcessorSet processorSet;

  private DocumentationConfigAspect(Model model) {
    super(model, ElementDocumentationAttribute.KEY, "documentation",
          DocumentationRule.getDescriptor(),
          model.getServiceConfig().getDocumentation().getRulesList());
    processorSet = DocumentationProcessorSet.standardSetup(model);
    registerLintRule(new DocumentationPresentRule(this));
  }

  @Override
  protected boolean isApplicable(ProtoElement element) {
    return true; // applicable to all elements.
  }

  @Override
  public void startMerging() {
    super.startMerging();
    Documentation doc = getModel().getServiceConfig().getDocumentation();
    getModel().putAttribute(DocumentationPagesAttribute.KEY,
        DocumentationPagesAttribute.create(processDocumentPages(doc)));
  }

  @Override @Nullable
  protected DocumentationRule fromIdlLayer(ProtoElement element) {
    // For the case there is no documentation rule, synthesize one from the comment
    // in the proto.
    String description = element.getFile().getDocumentation(element);
    if (Strings.isNullOrEmpty(description)) {
      return null;
    }
    return DocumentationRule.newBuilder()
        .setSelector(element.getFullName())
        .setDescription(trimCommentIndentation(element.getFile().getDocumentation(element)))
        .build();
  }

  @Override
  protected ElementDocumentationAttribute evaluate(ProtoElement element, DocumentationRule rule,
      boolean isFromIdl) {

    // Process documentation by chained processors, such as CommentFilter and SourceNormalizer.
    String description = processorSet.process(rule.getDescription(), element.getLocation(),
        element);

    // Processing may have attached page attribute to element. Propagate to file of element if the
    // file does not yet have one, so the first occurrence of a (== page ==) instruction
    // becomes the default for the enclosing file.
    if (element.hasAttribute(PageAttribute.KEY)
        && !element.getFile().hasAttribute(PageAttribute.KEY)) {
      element.getFile().putAttribute(PageAttribute.KEY, element.getAttribute(PageAttribute.KEY));
    }

    return ElementDocumentationAttribute.create(description);
  }

  /**
   * Trim the one space indentation in proto comments which is the convention for writing
   * documentation.
   */
  private static String trimCommentIndentation(String description) {
    if (description.startsWith(" ")) {
      description = description.substring(1);
    }
    return description.replace("\n ", "\n").replace("\r ", "\r");
  }

  @Override
  public void startNormalization(Service.Builder builder) {
    super.startNormalization(builder);
    if (getModel().hasAttribute(DocumentationPagesAttribute.KEY)) {
      // Since overview has been merged into the page structure, we should clear it from normalized
      // service config. Otherwise the normalized config will have both 'overview' and 'pages' set
      // which violate the rule enforced at merging stage.
      Documentation.Builder docBuilder = builder.getDocumentationBuilder();
      docBuilder.clearOverview();
      docBuilder.clearPages();
      docBuilder.addAllPages(getModel().getAttribute(DocumentationPagesAttribute.KEY)
          .toplevelPages());
    }
  }

  @Override
  protected void clearRuleBuilder(Builder builder) {
    builder.getDocumentationBuilder().clearRules();
  }

  @Override
  protected void addToRuleBuilder(Builder builder, String selector,
      ElementDocumentationAttribute attr) {
    builder.getDocumentationBuilder().addRules(
        DocumentationRule.newBuilder()
          .setSelector(selector)
          .setDescription(attr.documentation())
          .build());
  }

  private List<Page> processDocumentPages(Documentation doc) {
    String overview = doc.getOverview();
    if (!Strings.isNullOrEmpty(overview) && !doc.getPagesList().isEmpty()) {
      error(SimpleLocation.TOPLEVEL, "Cannot configure both `overview` and `toplevel_pages` in "
          + "documentation confguration.");
      return EMPTY_PAGES;
    }

    List<Page> toBeProcessedPages = null;
    ImmutableList.Builder<Page> processedPages = ImmutableList.builder();
    // If the overview is set, we will normalize it into page structure.
    if (!Strings.isNullOrEmpty(doc.getOverview())) {
      toBeProcessedPages = Lists.newArrayList(
          Page.newBuilder()
            .setName("Overview")
            .setContent(doc.getOverview())
            .build());
    } else {
      toBeProcessedPages = doc.getPagesList();
    }
    ensureUniquePageName(""/* Parent page name*/, toBeProcessedPages);
    for (Page page : toBeProcessedPages) {
      processedPages.add(processPage("", page));
    }
    return processedPages.build();
  }

  private Page processPage(String parentPageFullName, Page page) {
    String currentPageFullName = parentPageFullName.isEmpty()
        ? page.getName() : String.format("%s.%s", parentPageFullName, page.getName());
    ensureUniquePageName(currentPageFullName, page.getSubpagesList());

    Page.Builder normalizedPage = page.toBuilder().clearSubpages();
    normalizedPage.setContent(
        processorSet.process(page.getContent(), SimpleLocation.TOPLEVEL, getModel()).trim());

    for (Page subpage : page.getSubpagesList()) {
      normalizedPage.addSubpages(processPage(currentPageFullName, subpage));
    }
    return normalizedPage.build();
  }

  /**
   * Ensure the sub page full names are unique regard of other page names and proto element
   * full name so that we will not have naming clash for references of the pages.
   */
  private void ensureUniquePageName(String parentPageName, List<Page> pages) {
    String parentPageNameInError = parentPageName.isEmpty() ? "Top Level" : parentPageName;
    SymbolTable symbolTable = getModel().getSymbolTable();
    Set<String> names = Sets.newHashSet();
    for (Page page : pages) {
      if (Strings.isNullOrEmpty(page.getName())) {
        error(getModel(), "Found empty subpage name of '%s' page.", parentPageNameInError);
      } else if (names.contains(page.getName())) {
        error(getModel(), "Found duplicate subpage name '%s' of '%s' page.",
            page.getName(), parentPageNameInError);
      } else if (symbolTable.resolve(
          String.format("%s.%s", parentPageName, page.getName())) != null) {
        error(getModel(), "Found conflict subpage name '%s' of '%s' page with ProtoElement.",
            page.getName(), parentPageNameInError);
      } else {
        names.add(page.getName());
      }
    }
  }
}
