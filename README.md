# GitHub Project Relations Visualization

A visualization of the relation between GitHub projects, based on common
collaborators.

## Visualization

Integrate http://bl.ocks.org/2514276 in the front-end. Dragging the
canvas is needed for larger graphs (unles we can make them fit), the
fish-eye is great for exploring. Also the pop-up is handier then the
labels I think.

## MapReduce

### Reduction using Monoids

Use Monoids from Scalaz

Only append complete elements, since we do a lot of element mapping on
small elements, this results in creating a lot of collections for one
element. Ruins performance completely. A handwritten version using folds
and maps over Scala's collections worked orders faster, but the fact
that the mapping and composition logic was mixed was not nice. Also
combining maps several levels deep was a pain to program.

### Reduce elements to collections

Can we have the simplicity of a monoid, but append elements to
collections: multoids? First attempt was for maps with monoid values. It
works but has limitations. Different functions for monoid or multoid
values in the map. Inference only one level deep, then we need to build
them explicitly. Also the monoids are for concrete types only.

### Generic collection multoids

We want the multoids to work with any concrete map type without having
to implement them over and over again. Leverage the design of the Scala
collection library. We use generic builders that can be implicitly looked
up to create a collection of the correct specific type. We are able to
infer the multoids over maps and collections, but still only one level
deep. Also the difference between multoid and monoid values in maps is
still there.

Performance was bad when we used to builder, because our function is
called for every element, bu then rebuild the collection using the
builder. Vast improvement when we used the builder only for the empty
collection and then appended elements using the normal + operators. These
messed up the types a bit, giving only a base type, so we cast to the
correct type. (this works for immutable types, but will it also for
mutable?)

### Fully inferred generic multoids

Unified the cases for monoids and multoids by implicitly wrapping monoids
in multoids who have the same collection as element type.

Reformulated the types of the implicit functions so we can access the
type parameters of the collections we get (e.g. the key and value type of
map). This allowed us to completely infer multoids that go several levels
deep. We created general multoids for maps, sets, sequences and tuples.

Performance of the final solution is very close to using handwritten folds.

### Comparison

* Monoids
  - reduce boilerplate
  - works well if individual items reduce to rather big data structures
* Map & Reduce
  - more boilerplate, nested reduction gets nasty
  - still no strong key dependency
* Multoids
  - Similar to Monoids in boiler plate
  - Include all available Monoids
  - Allow append of elements, not just collections which makes it much
    better fit for listitem to listitem operations where the dataset is
    transformed but the size stays the same.
   
What does the data look like? And after different steps?
What if the data was bigger? Impact on data processing, visualisation?

It seems a known issue: http://stackoverflow.com/a/2189532

### Chaining mapReduce

Another nice feature would be chaining, where we create a chain of
mapReducers and call the whole chain on a dataset. The composition problem
of before (which types can automatically be appended to which collections)
is no reversed. We want to go from a collection (the previous out)
to individual elements we can map over. And the previous result might
not even be a collection. To make this possible we would have to write
co-multoids corresponding to the multoids that can extract the individual
values from the collection. This is probably slightly easier than the
multoids because extraction is only one level deep.

## Build & Run

```sh
$ cd github-relations-viz
$ ./sbt
> container:start
> ~ ;copy-resources;aux-compile
```

Now open the site's [root page](http://localhost:8080/) in your browser.
