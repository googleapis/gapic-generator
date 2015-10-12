package io.gapi.fx.processors.resolver;

import io.gapi.fx.model.Model;
import io.gapi.fx.model.Processor;
import io.gapi.fx.model.SymbolTable;
import io.gapi.fx.model.stages.Resolved;
import com.google.common.collect.ImmutableList;
import com.google.inject.Key;

/**
 * A processor which establishes the {@link Resolved} stage.
 *
 * <p>
 * The resolver does a proper context check on the model constructed from a protocol descriptor.
 * There is not assumption that the protocol compiler would have run before us, allowing tools
 * building the model from descriptors which are inconsistent, and getting proper error messages
 * instead of crashes.
 */
public class Resolver implements Processor {

  @Override
  public ImmutableList<Key<?>> requires() {
    return ImmutableList.of();
  }

  @Override
  public Key<?> establishes() {
    return Resolved.KEY;
  }

  @Override
  public boolean run(Model model) {
    int oldErrorCount = model.getErrorCount();
    SymbolTable symbolTable = new SymbolTableBuilder(model).run();
    model.setSymbolTable(symbolTable);
    new ReferenceResolver(model, symbolTable).run();
    if (oldErrorCount == model.getErrorCount()) {
      // No new errors produced -- success.
      model.putAttribute(Resolved.KEY, new Resolved());
      return true;
    }
    return false;
  }
}
