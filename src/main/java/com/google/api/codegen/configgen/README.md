## Gapic Config Generation

Generates the gapic config.

The purpose of the `ConfigNode`s is to preserve comments when refreshing the
gapic config. Linked nodes allow easy insertion and replacement of any node.

Different node types indicate what the `NodeVisitor` should do for a visited
node. Nodes can have metadata, such as `Comment`s, that annotate and change the
behavior of a node depending on how the `NodeVisitor` interprets it.

The mergers combine the `ApiModel` with a `ConfigNode` to output a `ConfigNode`
that represents the IDL. For initial gapic config generation, the input node is
empty. When refreshing, the node will be read from an existing gapic yaml.

To generate the yaml, a `ConfigGenerator` visits each node and builds the
output.
