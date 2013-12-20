Graph Object Mapper
===================

Graph Object Mapper projects any Java object model onto a Tinkerpop compatible graph!

Features
--------

* Automatically projects Java object models to graphs without requiring any annotations.
* Provides a `model` Gremlin step that locates model objects matching the specified type whose subgraphs contain vertices matching by the preceding step.
* Preserves the order of `Collection`-based fields.
* Automatically deduplicates isomorphic subgraphs.

Getting Started
---------------

Read the [documentation](https://github.com/jkschneider/graph-object-mapper/wiki).

Building from source
--------------------

Run `mvn package` in the root directory.