package io.gapi.fx.aspects.documentation;

import io.gapi.fx.model.Element;
import io.gapi.fx.model.Location;
import io.gapi.fx.model.Model;
import io.gapi.fx.model.ProtoElement;
import io.gapi.fx.model.SymbolTable;
import io.gapi.fx.model.Visitor;
import io.gapi.fx.util.VisitsBefore;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is responsible for resolving the relative name references in comments.
 *
 * <p>Markdown comments in proto files could contain references to other proto elements. These
 * references could be relative meaning that user doesn't have to specify the full name to address
 * an element. An example of such reference in markdown syntax is:
 * <pre>  {@code
 *   package protiary. test . comment_refs;
 *
 *   message Foo {
 *     optional string field;
 *   }
 *
 *   //  display text  relative name
 *   // [field of Foo][Foo.field]
 *   message Bar {
 *   }
 * }</pre>
 *
 * <p>As can be seen in the above example the reference to field of Foo is relative without having
 * to specify the full name of the package containing Foo.
 *
 * <p>This class will act as one of the processors in documentation aspect and will be run before
 * other processors. It will replace all the relative names with their respective resolved full name
 * in the comment. In case where it cannot make a resolution, it will not change or replace the
 * text.
 *
 * <p>For more information on how we resolve these names see: https://go/1p-comment-relative-names
 */
public class CommentReferenceResolver implements DocumentationProcessor {
  public static final Pattern MARKDOWN_LINK_REGEX =
      Pattern.compile("\\[(?<text>.*?)\\][ ]?(?:\\n[ ]*)?\\[(?<id>.*?)\\]");
  private static final int TEXT_GROUP_NUMBER = 1;
  private static final int ID_GROUP_NUMBER = 2;

  private Set<String> protoElemFullNames;

  public CommentReferenceResolver(Model model) {
    protoElemFullNames = new HashSet<>();
    // Build the set of fully qualified names in this model.
    new Visitor(model.getScoper()) {
      @VisitsBefore
      public void accept(ProtoElement element) {
        protoElemFullNames.add(element.getFullName());
      }
    }.accept(model);
  }


  @Override
  public String process(String comment, Location sourceLocation, Element element) {
    // Resolver only works based on given element full name. And we will return original comment
    // if given element is null.
    if (element == null) {
      return comment;
    }
    // Try to match the comment with markdown syntax for links:
    Matcher linkMatcher = MARKDOWN_LINK_REGEX.matcher(comment);
    StringBuilder sb = new StringBuilder();
    // This var will keep track of the end of last match and will be used to construct the final
    // output.
    int lastMatchedEnd = 0;
    while (linkMatcher.find()) {
      String id = linkMatcher.group(ID_GROUP_NUMBER).trim();
      String text = linkMatcher.group(TEXT_GROUP_NUMBER);
      if (id.isEmpty()) {
        id = text;
      }
      Iterable<String> candidateNames;
      String resolution = null;
      candidateNames = SymbolTable.nameCandidates(element.getFullName(), id);
      for (String candidateName : candidateNames) {
        if (protoElemFullNames.contains(candidateName)) {
          resolution = candidateName;
          break;
        }
      }

      sb.append(comment.substring(lastMatchedEnd, linkMatcher.start(TEXT_GROUP_NUMBER)));
      sb.append(text);
      sb.append(comment.substring(linkMatcher.end(TEXT_GROUP_NUMBER),
          linkMatcher.start(ID_GROUP_NUMBER)));
      if (resolution != null) {
        sb.append(resolution);
      } else {
        // When there is no resolution, just output the input name.
        sb.append(linkMatcher.group(ID_GROUP_NUMBER));
      }
      lastMatchedEnd = linkMatcher.end(ID_GROUP_NUMBER);
    }
    sb.append(comment.substring(lastMatchedEnd));
    return sb.toString();
  }
}
