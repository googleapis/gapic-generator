package io.gapi.fx.processors.merger;

import com.google.api.Service;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Key;
import com.google.protobuf.Api;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;

import io.gapi.fx.aspects.visibility.model.ScoperImpl;
import io.gapi.fx.model.ConfigAspect;
import io.gapi.fx.model.Diag;
import io.gapi.fx.model.Interface;
import io.gapi.fx.model.Model;
import io.gapi.fx.model.Processor;
import io.gapi.fx.model.ProtoElement;
import io.gapi.fx.model.SimpleLocation;
import io.gapi.fx.model.TypeRef;
import io.gapi.fx.model.Visitor;
import io.gapi.fx.model.stages.Merged;
import io.gapi.fx.model.stages.Resolved;
import io.gapi.fx.util.VisitsBefore;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A processor which establishes the {@link Merged} stage, in which service config
 * and IDL are combined and validated.
 *
 * <p>The merger also derives interpreted values from the configuration, for example, the
 * {@link HttpMethodConfig}, and reports consistency errors encountered during interpretation.
 */
public class Merger implements Processor {

  @Override
  public ImmutableList<Key<?>> requires() {
    return ImmutableList.<Key<?>>of(Resolved.KEY);
  }

  @Override
  public Key<?> establishes() {
    return Merged.KEY;
  }

  @Override
  public boolean run(Model model) {
    int oldErrorCount = model.getErrorCount();

    if (model.getServiceConfig() == null) {
      // No service config defined; create a dummy one.
      // TODO(wgg): need to figure what to do with this case.
      model.setServiceConfig(Service.getDefaultInstance());
    }

    // TODO: MIGRATION

    //if (model.getLegacyConfig() == null) {
    //  // No legacy config defined; create a dummy one.
    //  model.setLegacyConfig(Legacy.getDefaultInstance());
    //}

    // Resolve apis, computing which parts of the model are included. Attach apis to interfaces.
    Service config = model.getServiceConfig();
    for (Api api : model.getServiceConfig().getApisList()) {
      Interface iface = model.getSymbolTable().lookupInterface(api.getName());
      if (iface != null) {
        // Add interface to the roots.
        model.addRoot(iface);
        // Attach api proto to interface.
        iface.setConfig(api);
      } else {
        model.addDiag(Diag.error(SimpleLocation.TOPLEVEL,
            "Cannot resolve api '%s'.", api.getName()));
      }
    }

    List<Set<ConfigAspect>> orderedAspectGroup = sortForMerge(model.getConfigAspects());
    // Merge-in config aspects.
    for (Set<ConfigAspect> aspects : orderedAspectGroup) {
      for (ConfigAspect aspect : aspects) {
        aspect.startMerging();
      }
    }

    for (Set<ConfigAspect> aspects : orderedAspectGroup) {
      new ConfigAspectMerger(aspects).visit(model);
    }

    for (Set<ConfigAspect> aspects : orderedAspectGroup) {
      for (ConfigAspect aspect : aspects) {
        aspect.endMerging();
      }
    }

    // Resolve types and enums specified in the service config as additional inclusions to
    // the tool chain, but not reachable from the service IDL, such as types associated with
    // Any type.
    for (com.google.protobuf.Type type : config.getTypesList()) {
      resolveAdditionalType(model, type.getName(), Type.TYPE_MESSAGE);
    }
    for (com.google.protobuf.Type type : config.getSystemTypesList()) {
      resolveAdditionalType(model, type.getName(), Type.TYPE_MESSAGE);
    }
    for (com.google.protobuf.Enum enumType : config.getEnumsList()) {
      resolveAdditionalType(model, enumType.getName(), Type.TYPE_ENUM);
    }
    for (com.google.api.CustomErrorRule customErrorRule : config.getCustomError().getRulesList()) {
      resolveAdditionalType(model, customErrorRule.getSelector(), Type.TYPE_MESSAGE);
    }
    for (String customErrorType : config.getCustomError().getTypesList()) {
      resolveAdditionalType(model, customErrorType, Type.TYPE_MESSAGE);
    }

    // Set the initial scoper based on the roots. This will scope down further operation on the
    // model to those elements reachable via the roots.
    model.setScoper(ScoperImpl.create(model.getRoots()));

    if (oldErrorCount == model.getErrorCount()) {
      // No new errors produced -- success.
      model.putAttribute(Merged.KEY, new Merged());
      return true;
    }
    return false;
  }

  /**
   * Resolve the additional type specified besides those that can be reached transitively from
   * service definition.
   */
  private void resolveAdditionalType(Model model, String typeName, Type kind) {
    TypeRef type = model.getSymbolTable().lookupType(typeName);
    if (type == null) {
      model.addDiag(Diag.error(SimpleLocation.TOPLEVEL,
          "Cannot resolve additional type '%s' specified in the config. Make sure its associated "
          + "build target was included in your protobuf build rule.", typeName));
      return;
    }
    if (type.getKind() != kind) {
      model.addDiag(Diag.error(SimpleLocation.TOPLEVEL,
          "Invalid reference. %s '%s' is referenced as a %s.", type.getKind(), typeName, kind));
      return;
    }
    if (type.isMessage()) {
      model.addRoot(type.getMessageType());
    } else if (type.isEnum()) {
      model.addRoot(type.getEnumType());
    }
  }

  private static class ConfigAspectMerger extends Visitor {

    private final Iterable<ConfigAspect> orderedAspects;

    private ConfigAspectMerger(Iterable<ConfigAspect> orderedAspects) {
      this.orderedAspects = orderedAspects;
    }

    @VisitsBefore void merge(ProtoElement element) {
      for (ConfigAspect aspect : orderedAspects) {
        aspect.merge(element);
      }
    }
  }

  /**
   * Returns the given config aspects as list of group of aspects in merge dependency order.
   * This performs a 'longest path layering' algorithm by placing aspects at different levels
   * (layers). First place all sink nodes at level-1 and then each node n is placed at level
   * level-p+1, where p is the longest path from n to sink. Aspects in each level are independent of
   * each other and can only depend on aspects in lower levels.
   * Detailed algorithm : 13.3.2 Layer Assignment Algorithms :
   * https://cs.brown.edu/~rt/gdhandbook/chapters/hierarchical.pdf
   */
  private static List<Set<ConfigAspect>> sortForMerge(Iterable<ConfigAspect> aspects) {
    Map<Class<? extends ConfigAspect>, ConfigAspect> aspectsByType =
        HashBiMap.create(Maps.toMap(
                             aspects, new Function<ConfigAspect, Class<? extends ConfigAspect>>() {
                               @Override
                               public Class<? extends ConfigAspect> apply(ConfigAspect aspect) {
                                 return aspect.getClass();
                               }
                             })).inverse();
    List<Class<? extends ConfigAspect>> visiting = Lists.newArrayList();
    Map<ConfigAspect, Integer> aspectsToLevel = Maps.newLinkedHashMap();
    for (ConfigAspect aspect : aspects) {
      assignLevelToAspect(aspect, aspectsByType, visiting, aspectsToLevel);
    }
    Map<Integer, Set<ConfigAspect>> aspectsByLevel = Maps.newLinkedHashMap();
    for (ConfigAspect aspect : aspectsToLevel.keySet()) {
      Integer aspectLevel = aspectsToLevel.get(aspect);
      if (!aspectsByLevel.containsKey(aspectLevel)) {
        aspectsByLevel.put(aspectLevel, Sets.<ConfigAspect>newLinkedHashSet());
      }
      aspectsByLevel.get(aspectLevel).add(aspect);
    }
    List<Set<ConfigAspect>> aspectListByLevels = Lists.newArrayList();
    for (int level = 1; level <= aspectsByLevel.size(); ++level) {
      aspectListByLevels.add(aspectsByLevel.get(level));
    }
    return aspectListByLevels;
  }

  /**
   * Does a DFS traversal and computes the maximum height (level) of each node from the sink node.
   */
  private static int assignLevelToAspect(ConfigAspect aspect,
      Map<Class<? extends ConfigAspect>, ConfigAspect> aspectsByType,
      List<Class<? extends ConfigAspect>> visiting, Map<ConfigAspect, Integer> aspectToLevel) {
    Class<? extends ConfigAspect> aspectType = aspect.getClass();
    if (aspectToLevel.containsKey(aspect)) {
      return aspectToLevel.get(aspect);
    }
    if (visiting.contains(aspectType)) {
      throw new IllegalStateException(
          String.format("Cyclic dependency between config aspect attributes. Cycle is: %s <- %s",
              aspectType, Joiner.on(" <- ").join(visiting)));
    }
    visiting.add(aspectType);
    Integer childMaxHeight = 0;
    for (Class<? extends ConfigAspect> dep : aspect.mergeDependencies()) {
      Integer childHeight =
          assignLevelToAspect(aspectsByType.get(dep), aspectsByType, visiting, aspectToLevel);
      childMaxHeight = childHeight > childMaxHeight ? childHeight : childMaxHeight;
    }
    visiting.remove(aspectType);
    aspectToLevel.put(aspect, childMaxHeight + 1);
    return childMaxHeight + 1;
  }
}
