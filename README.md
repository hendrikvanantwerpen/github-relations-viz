GitHub Project Relations Visualization
======================================

A visualization of the relation between GitHub projects, based on common
collaborators.

Actor setup
-----------

Layer 1: per file: Txt -> [(Committer,[Project])] filter on dates
Layer 2: grouped per committer: [(Committer,[Project])] -> [(Project,Project)]
Layer 3: grouped per projectpair: [(Project,Project)] -> [(Project,Project)]
Layer 4: [(Project,Project)] -> GraphJson

Request: (Date,Date) -> [(Project,Project)]

Missing: counts, dates

Build & Run
-----------

```sh
$ cd github-relations-viz
$ ./sbt
> container:start
> ~ ;copy-resources;aux-compile
```

Now open the site's [root page](http://localhost:8080/) in your browser.
