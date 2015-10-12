package io.gapi.fx.processors.linter;

import io.gapi.fx.model.ConfigAspect;
import io.gapi.fx.model.Model;
import io.gapi.fx.model.Processor;
import io.gapi.fx.model.ProtoElement;
import io.gapi.fx.model.Visitor;
import io.gapi.fx.model.stages.Linted;
import io.gapi.fx.model.stages.Merged;
import io.gapi.fx.util.VisitsBefore;

import com.google.common.collect.ImmutableList;
import com.google.inject.Key;

/**
 * Linter for IDL and service config. Delegates work to config aspects.
 */
public class Linter implements Processor {

  @Override
  public ImmutableList<Key<?>> requires() {
    return ImmutableList.<Key<?>>of(Merged.KEY);
  }

  @Override
  public Key<?> establishes() {
    return Linted.KEY;
  }

  @Override
  public boolean run(final Model model) {
    int oldErrorCount = model.getErrorCount();

    for (ConfigAspect aspect : model.getConfigAspects()) {
      aspect.startLinting();
    }
    new Visitor(model.getScoper()) {
      @VisitsBefore void validate(ProtoElement element) {
        for (ConfigAspect aspect : model.getConfigAspects()) {
          aspect.lint(element);
        }
      }
    }.visit(model);

    for (ConfigAspect aspect : model.getConfigAspects()) {
      aspect.endLinting();
    }

    if (oldErrorCount == model.getErrorCount()) {
      // No new errors produced -- success.
      model.putAttribute(Linted.KEY, new Linted());
      return true;
    }
    return false;
  }
}
