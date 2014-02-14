Pappus Graph-Object Projection Framework
----------------------------------------

![Simple Model](https://github.com/jkschneider/pappus-graph-projection/wiki/img/projection.jpg)

Pappus projects any Java object model onto a Tinkerpop compatible graph!

Features
--------

* Automatically projects Java object models to graphs without requiring any annotations.
* Provides a `model` Gremlin step that locates model objects matching the specified type whose subgraphs contain vertices matched by the preceding step.
* Preserves the order of `Collection`-based fields.
* Automatically deduplicates isomorphic subgraphs.
* Version or time slices model objects to maintain history.
* Cascading delete of subgraphs related to a specific model element.

Getting Started
---------------

Read the [documentation](https://github.com/jkschneider/pappus-graph-projection/wiki).

To use, add the following Maven dependency:

```xml
<dependency>
  <groupId>com.github.jkschneider</groupId>
  <artifactId>pappus-graph-projection</artifactId>
  <version>0.1.3</version>
</dependency>
```

Building from source
--------------------

Run `mvn package` in the root directory.
