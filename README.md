# Introduction

Google API tools consist of two tools:

- The Google API Code Generator, which produces client code wrappers around GRPC
  APIs which can be enriched by manually edited code.
- The Google API Code Synchronizer, which can merge updates in the generated
  code wrapper to an existing code wrapper which might have manual edits.

More Google API tools will be introduced in the future.

## The Google API Code Generator

We currently support the generation of client code wrappers in seven languages:
Java, Python, Go, C#, Ruby, Node.js, and PHP. There are two ways to generate client
code wrappers:

1. Invoking directly through a gradle task (see the build.gradle:runCodeGen task).
2. Invoking a code generation pipeline through
   [artman]( https://github.com/googleapis/artman#usage ).

## The Google API Code Synchronizer

The process currently hard-wires `kdiff3` for merging and conflict resolution.
`kdiff3` runs in auto mode and will only pop up a UI if conflict resolution is
not possible (note: X display needed in this case). There are two ways to invoke
code synchronizer:

1. Invoking directly through a gradle task (see the build.gradle:runSynchronizer
   task).
2. Invoking a code generation pipeline which contains synchronization step
   through [artman](https://github.com/googleapis/artman/blob/master/USAGE.rst).
