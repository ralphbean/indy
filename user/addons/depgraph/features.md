---
title: "Depgraph: Features"
---

### Initializing the Java Client Module

To use the Java client API to access the Depgraph add-on, you'll need to initialize it with the `DepgraphAproxClientModule`:

    DepgraphAproxClientModule module = new DepgraphAproxClientModule();
    Aprox aprox = new Aprox("http://localhost:8080", module).connect();

The Java examples in the feature documentation below will assume you already have the above snippet of code in place, with the variable `module` ready to use.

### Feature Documentation

Depgraph's features can be broken down into a few broad categories including administration and maintenance, graph querying and extraction, and graph rendering.

The following sections provide detail on each of these feature categories:

#### Administration and Maintenance
<a name="admin"></a>

* [List](features/list-workspaces.html) or [delete](features/delete-workspace.html) workspaces
* [Re-scan](features/rescan-metadata.html) previously discovered GAVs for metadata
  * *(scans for extra info like SCM available in POMs)*
* [Reindex](features/reindex-graph.html) previously discovered relationships
* [Store / update](features/store-update-metadata.html) metadata attached to GAVs

#### Graph Querying and Extraction
<a name="query"></a>

* List GAVs (with option for matching a GA pattern) in a RelationshipGraph that are:
  * [included](features/list-included-gavs.html)
  * [missing](features/list-missing-gavs.html) (not yet resolved)
  * [in error](features/list-gavs-with-errors.html) (attempted but failed resolve)
  * [variable](features/list-variable-gavs.html) (snapshots and the like)
* List [paths](features/list-paths.html) from a [set of] root GAV to some specific [set of] target GA
* List [ancestry](features/list-ancestry.html) or [build order](build-order.html) for a particular [set of] GAV
* List [relationship cycles](features/list-cycles.html) in the graph
* [Retrieve the full graph](features/graph-export.html) in JSON format
* [Retrieve the full list of artifacts](features/list-artifacts.html) in the graph, in JSON format

#### Graph Rendering
<a name="render"></a>

* [Create a BOM / POM](features/pom-generation.html) from a RelationshipGraph
* [Create a Maven console-style log](features/downlog.html) of "Downloading..." lines
  * *for artifacts referenced in the graph*
* [Create a zip archive](features/repo-zip.html) containing all artifacts in the graph
* Generate `maven-dependency-plugin` style output:
  * [dependency:tree](features/dep-tree.html)
  * [dependency:list](features/dep-list.html)