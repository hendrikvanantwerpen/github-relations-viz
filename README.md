GitHub Project Relations Visualization
======================================

A visualization of the relation between GitHub projects, based on common
collaborators.

Ideas
-----

Integrate http://bl.ocks.org/2514276 in the front-end. Dragging the
canvas is needed for larger graphs (unles we can make them fit), the
fish-eye is great for exploring. Also the pop-up is handier then the
labels I think.

Performance
-----------

Using the monoid approach resulted in creating a lot of extra objects,
that severely harmed performance. Instead we reverted to a more traditional
format where we provide a reduce function ourselves, allowing element
insertion into collections.
The monoid thing would be better if individual items created big results to be
merged. Here the results were same size, so overhead was a lot. Alternative was
maps/folds in mapReduce functions, but that would violate the abstraction.

 - Monoids
   : reduce boilerplate, but a lot of cration
 - Map & Reduce
   : more boilerplate, nested structures get nasty
   : still no strong key dependency
 - Pluroids - can we make this?
   : Similar to Monoids, but allow append of elements, not just collections.
   
Performance difference Map/Set?
Good way to use collections?
Can we make it run on one machine?
What does the data look like? And after different steps?
What if the data was bigger? Impact on data processing, visualisation?

Build & Run
-----------

```sh
$ cd github-relations-viz
$ ./sbt
> container:start
> ~ ;copy-resources;aux-compile
```

Now open the site's [root page](http://localhost:8080/) in your browser.
