package io.gapi.fx.aspects.naming;

import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.net.InternetDomainName;

import io.gapi.fx.aspects.ConfigAspectBase;
import io.gapi.fx.aspects.LintRule;
import io.gapi.fx.model.Element;
import io.gapi.fx.model.EnumType;
import io.gapi.fx.model.EnumValue;
import io.gapi.fx.model.Field;
import io.gapi.fx.model.Interface;
import io.gapi.fx.model.MessageType;
import io.gapi.fx.model.Method;
import io.gapi.fx.model.Model;
import io.gapi.fx.model.ProtoFile;

import java.util.regex.Pattern;

/**
 * Config aspect for validating naming rules.
 */
public class NamingConfigAspect extends ConfigAspectBase {

  public static NamingConfigAspect create(Model model) {
    return new NamingConfigAspect(model);
  }

  // TODO(wgg): refine those rules as naming conventions for OP are finalized.

  private static final String UPPER_CAMEL_REGEX = "[A-Z][A-Za-z0-9]*";
  private static final String UPPER_UNDERSCORE_REGEX = "[A-Z][A-Z0-9_]*";
  private static final String LOWER_UNDERSCORE_REGEX = "[a-z][a-z0-9_]*";
  private static final String PACKAGE_REGEX = String.format("%s([.]%s)*",
      LOWER_UNDERSCORE_REGEX, LOWER_UNDERSCORE_REGEX);
  private static final Pattern INVALID_CHARACTER_PATTERN = Pattern.compile("[_]");

  private NamingConfigAspect(Model model) {
    super(model, "naming");
    registerLintRule(new ServiceNameRule());

    // Package name.
    registerLintRule(new RegexRule<>(ProtoFile.class, "lower-camel-qualified", PACKAGE_REGEX,
        new Function<ProtoFile, String>() {
          @Override public String apply(ProtoFile elem) {
            return elem.getFullName();
          }
    }));

    // Interface name.
    registerLintRule(new RegexRule<>(Interface.class, "upper-camel", UPPER_CAMEL_REGEX,
        new Function<Interface, String>() {
          @Override public String apply(Interface elem) {
            return elem.getSimpleName();
          }
    }));

    // Method name.
    registerLintRule(new RegexRule<>(Method.class, "upper-camel", UPPER_CAMEL_REGEX,
        new Function<Method, String>() {
          @Override public String apply(Method elem) {
            return elem.getSimpleName();
          }
    }));

    // Message name.
    registerLintRule(new RegexRule<>(MessageType.class, "upper-camel", UPPER_CAMEL_REGEX,
        new Function<MessageType, String>() {
          @Override public String apply(MessageType elem) {
            return elem.getSimpleName();
          }
    }));

    // Field Name
    registerLintRule(new RegexRule<>(Field.class, "lower-underscore", LOWER_UNDERSCORE_REGEX,
        new Function<Field, String>() {
          @Override public String apply(Field elem) {
            return elem.getSimpleName();
            // TODO: MIGRATION
            //return ExtensionSimpleNameAttribute.getSimpleName(elem);
          }
    }));

    // Enum type Name
    registerLintRule(new RegexRule<>(EnumType.class, "upper-camel", UPPER_CAMEL_REGEX,
        new Function<EnumType, String>() {
          @Override public String apply(EnumType elem) {
            return elem.getSimpleName();
          }
    }));

    // Enum value Name
    registerLintRule(new RegexRule<>(EnumValue.class, "upper-underscore",
        UPPER_UNDERSCORE_REGEX,
        new Function<EnumValue, String>() {
          @Override public String apply(EnumValue elem) {
            return elem.getSimpleName();
          }
    }));
  }

  private class ServiceNameRule extends LintRule<Model> {

    private ServiceNameRule() {
      super(NamingConfigAspect.this, "service-dns-name", Model.class);
    }

    @Override public void run(Model model) {
      String serviceName = model.getServiceConfig().getName();
      if (!Strings.isNullOrEmpty(serviceName) && (!InternetDomainName.isValid(serviceName)
      // InternetDomainName.isValid does a lenient validation and allows underscores (which we do
      // not want to permit as DNS names). Therefore explicitly checking for underscores.
          || INVALID_CHARACTER_PATTERN.matcher(serviceName).find())) {
        warning(model, "Invalid DNS name '%s'.", serviceName);
      }
    }
  }

  private class RegexRule<E extends Element> extends LintRule<E> {

    private final String ruleName;
    private final Pattern pattern;
    private final Function<E, String> nameExtractor;

    private RegexRule(Class<E> elemClass, String ruleName,
        String regex, Function<E, String> nameExtractor) {
      super(NamingConfigAspect.this, ruleName, elemClass);
      this.ruleName = ruleName;
      pattern = Pattern.compile(regex);
      this.nameExtractor = nameExtractor;
    }

    @Override public void run(E elem) {
      String name = nameExtractor.apply(elem);
      if (!pattern.matcher(name).matches()) {
        warning(elem, "Name '%s' is not matching %s conventions (pattern '%s')",
            name, ruleName, pattern);
      }
    }
  }
}
