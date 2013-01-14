# GitHub Project Relations Visualization

A visualization of the relation between GitHub projects, based on common
collaborators. Interactive web interface.

## Visualization

 * Search for projects
 * Search for languages
 * Time-run
 * Fix on project / possible filter for it
 * Window sizing
 * Link value needs label

 * Monthly periods iso weekly?
 * Use case classes iso tuples?

## Build & Run

```sh
$ cd github-relations-viz
$ ./sbt
> container:start
> ~ ;copy-resources;aux-compile
```

Now open the site's [root page](http://localhost:8080/) in your browser.
