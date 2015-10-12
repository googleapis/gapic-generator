
# GAPI Tools Framework

The GAPI Tools Framework (GAPI Fx) is a set of Java libraries for
processing API definitions.

GAPI Fx takes as input a service configuration (usually provided in Yaml)
and a proto library. It parses the inputs into an object model,
processes it, and produces multiple outputs, for example:

- Code generation
- Validation errors and style violations.
- A normalized and validated service configuration for input into downstream tools.
- Documentation.
- Discovery documents in various formats.

This document gives an overview of architecture and usage of the framework.

## General Architecture

GAPI Fx is based on a traditional compiler architecture, with the
following major components:

- The **model** is an AST (Abstract Syntax Tree) representation of the
  API definition. The model consists of elements such as messages,
  fields, methods, interfaces (proto service definitions), files, and
  the root object (the model itself). To each model element attributes
  can be attached (Attributed AST). The model is stateful, and besides
  the attributes computed by various phases, also carries information
  like collected error messages and plugins registered in the
  framework. Tools like visitors are supported on the model and its
  elements.

- A **processor** is a pluggable abstraction which represents a
  transformation of the model, usually in terms of attaching
  attributes to it. Examples of processors include name resolver,
  config merger, linter, etc. Processors are implicitly chained by
  declaring dependencies on **stages** which the model has reached,
  and each processor establishes itself a stage.

- A **tool** is an abstraction which takes as input the model, in a
  given stage, and produces some output, for example, configuration
  files, documentation, or code. Most tools use a template engine
  called **snippets** which is tailored for producing structured text
  and part of GAPI Fx (see link below).

- A **configuration aspect** is a plugabble abstraction which
  comprehends all processing functionality around a certain area of
  configuration in one place. There are configuration aspects for
  auth, quota, http, visibility, discovery, etc. Each aspect
  implements processing for **merging** configuration with proto, for
  **linting** the configuration and protos, and for **normalizing**
  it. Generic processors for those three stages take care of composing
  the registered aspect's logics.

References:

- The [Model](src/main/java/io/gapi/fx/model/Model.java)
  class, the root object of the AST.
- Selected model elements:
  [ProtoFile](src/main/java/io/gapi/fx/model/ProtoFile.java),
  [Interface](src/main/java/io/gapi/fx/model/Interface.java),
  [Method](src/main/java/io/gapi/fx/model/Method.java),
  [MessageType](src/main/java/io/gapi/fx/model/MessageType.java), and
  [Field](src/main/java/io/gapi/fx/model/Model.java).
- The
  [Processor](src/main/java/io/gapi/fx/model/Processor.java)
  interface, and an example of a processor, the
  [Resolver](src/main/java/io/gapi/fx/processors/resolver/Resolver.java).
- The [ToolBase](src/main/java/io/gapi/fx/tools/ToolBase.java) class
  from which tools can be derived, and an example of a tool, the
  [SchemaGeneratorTool](src/main/java/io/gapi/fx/tools/restschemagen/SchemaGeneratorTool.java).
- The [SnippetSet](src/main/java/io/gapi/fx/snippet/SnippetSet.java) class, which contains
  documentation of the snippets templating engine.
- The
  [ConfigAspect](src/main/java/io/gapi/fx/model/ConfigAspect.java)
  interface, the
  [ConfigAspectBase](src/main/java/io/gapi/fx/aspects/ConfigAspectBase.java)
  class from which specific aspects derive, and an example of an
  aspect, the
  [VersionConfigAspect](src/main/java/io/gapi/fx/aspects/versioning/VersionConfigAspect.java).
  Furthermore, the
  [RuleBasedConfigAspect](src/main/java/io/gapi/fx/aspects/RuleBasedConfigAspect.java)
  which is used for config aspects which use the config rule
  mechanism. See also more information about configuration aspects
  below.


## Configuration Aspects: A Walkthrough

Adding a new configuration aspect to GAPI Fx is relative easy and can be
contained to changes in a few places. Here we walk through an example.

Suppose we want to add a new service configuration option which allows
a user to specify a *release number* in the API configuration, so he can
write in his Yaml something like:

```
name: myapi.googleapis.com
release_number: 2727282
...
```

### Step 1: Extend the Service Configuration Proto

The [service config schema][service_config] is defined as a protocol buffer, and we need to add a field to it, as such:


```proto
message Service {
  ...
  string release_number = 43; // actual number may vary
  ...
}
```

[service_config]: http://cs/codesearch#google3/google/api/service.proto

The Yaml parser part of GAPI Fx will automatically support parsing the new field.

### Step 2: Add a New Config Aspect

Next we add a new config aspect package and class to the `framework.aspects` package:

```java
package framework.aspects.releasing;

public class ReleaseConfigAspect extends ConfigAspectBase {

  public ReleaseConfigAspect(Model model) {
    super(model, "releasing"); // 'releasing' will be used in error msgs
    registerStyleRule(new ReleaseNumberRule());
  }

  // ... to be continued
}
```

We now add processing logic. There are generally three phases to
which an aspect can add logic: **merging**, **linting**, and
**normalization**. The aspect is called from the framework for
starting the phase, for processing each model element, and for ending
the phase. Methods from `ConfigAspectBase` can be overriden to get
triggered on those events. For example, we only override the beginning
of the merge phase, where we check whether the release number is a
valid digit sequence:

```java
class ReleaseConfigAspect extends ConfigAspectBase {

  @Override
  public void beginMerging(Model model) {
    String releaseNumber = model.getServiceConfig().getReleaseNumber();
    if (releaseNumber.length() == 0 || !CharMatcher.DIGIT.matchesAllOfi(releaseNumber)) {
      error(model, "The release number must be a non-empty sequence of digits, but is '%s'.",
          releaseNumber);
    }
  }
}
```

Merging is generally the phase where inconsistency in the service
config should be detected. In the above case, it is an *error* if the
release number is not a string of digits. Conditions which are not
producing errors but style violations should be reported differently
see below.

In more complex use cases, merging would also attach derived
information to the model.

We conclude this example by implementing a style rule. Those rules,
when registered with the base class, are used to automatically
implement the linting phase. For the example, we put some arbitrary
style rule up that release numbers must have at least four digits:

```java
class ReleaseConfigAspect extends ConfigAspectBase {

  private class ReleaseNumberRule extends StyleRule<Model> {

    ReleaseNumberRule() {
      // 'number' will be used in error reporting, together
      // with aspect name, so the user can turn this rule off.
      super(ReleaseConfigAspect.this, "number", Model.class);
    }

    @Override
    public void run(Model model) {
      int noOfDigits = model.getServiceConfig().getReleaseNumber().length();
      if (noOfDigits < 4) {
        violation(model, "Release numbers must have at least 4 digits, found: %s",
            noOfDigits);
      }
    }
  }

```

### Step 3: Register Config Aspect in Standard Setup

Every config aspect relevant for the default logic of the tools
framework should be added to the
[StandardSetup](src/main/java/io/gapi/fx/setup/StandardSetup.java)
class, so tools pick it up by default.

### Step 4: Test the Config Aspect

Currently, there is no independent testing abstraction available for
configuration aspects (one is forthcoming), and we test aspects using
baseline tests (see below), typically as part of the
[NormalizerTest](http://cs/codesearch#google3/javatests/com/google/api/tools/framework/processors/normalizer/NormalizerTest.java).
The normalizer test is well suited for this because it triggers all
phases in which aspects are involved.

## Baseline Tests

Most of the code in GAPI Fx is (or can be described) as a transformation
from input to output text. Many of our tests are written by comparing
expected output we have stored in piper with the current output. This
is facilitated by the [BaseLineTestCase][baselineTest] class.

Both the expected input and output for those tests is found in a
subdirectory of the test package named `testdata`, like
[this][testdataExample] one. Usually inputs are proto and yaml files
(the api service config) and outputs some stuff generated from it, in
a single file.

### Updating Baseline Tests

If a BaseLineTestCase fails, it prints the diff between expected and
actual output to the JUnit console. It also stores the actual output
in a file under `/tmp/package_testdata. It **only** does that when not
running under forge, e.g. when running from Eclipse.

The recommended workflow to update baseline files is to copy the
actual result from `/tmp` into the testdata directory after reviewing
whether the diff is ok:

```sh
$ cd javatests/com/google/api/tools/framework/tools/restschemagen
$ cp /tmp/com.google.api.tools.framework.tools.restschemagen_tesdata/* testdata
```

The recommendation for being reasonable fast with this workflow is (a)
run the test from Eclipse (b) have a shell open in the test's package
dir (c) use tab-completion on prefixes such as `/tmp/com.` (d) use
shell history via arrow-up or Ctrl-R to repeat updating.

**Note**: We are still looking for a volunteer to automate the
  updating of baseline files.

### Noisiness

Yes, baseline tests are noisy. Many tests may fail as cause of a small
change. But it's worth it:

- We can understand what the system actually does by looking at the
  baselines. This is much harder with UTs which try to focus on a few
  assertions at a given time.
- It makes reviewing CLs easier, as the impact of a change is clearly
  visible.
- It's way faster to write and maintain a baseline test than writing a
  unit test.

Baseline tests shouldn't be applied anywhere, indeed. It's a design choice to be made on a case-by-case basis.

[baselineTest]: http://cs/codesearch#piper///depot/google3/java/com/google/api/server/testing/baseline/BaselineTestCase.java
[testdataExample]: http://cs/codesearch#piper///depot/google3/javatests/com/google/api/tools/framework/tools/restschemagen/testdata/
