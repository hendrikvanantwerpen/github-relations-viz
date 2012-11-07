# GitHub Project Relations Visualization

A visualization of the relation between GitHub projects, based on common
collaborators.

## Visualization

Integrate http://bl.ocks.org/2514276 in the front-end. Dragging the
canvas is needed for larger graphs (unles we can make them fit), the
fish-eye is great for exploring. Also the pop-up is handier then the
labels I think.

What does the data look like? And after different steps?
What if the data was bigger? Impact on data processing, visualisation?

## MapReduce

### Reduction using Monoids

Use Monoids from Scalaz. In the map create in instance of the monoid
and concat them in the reduce.

The problem is that they only combine monoids. If our monoids are
collections, like sets or maps, a new set is created for every map
operation. In our case often every mapreduced element corresponded to one
new element. This caused the creation of a lot of element, resulting in
very bad performance. Not only was performance bad, it got worse with
every next mapReduce, suggesting the the JVM was going down. It seems
other people have observed the same behaviour with lots of short-lived
objects are involved: http://stackoverflow.com/a/2189532

To compare: a handwritten version using folds and maps over Scala's
collections worked orders faster. The fact that mapping and composition
logic was mixed was obviously not nice. Also the combination logic got
more complicated with every level that was added and the combination
logic was reproduced in every fold for the same kind of collections.

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

Note that using the CanBuildFrom approach, the Multoids only work for
immutable collections. (is this true??)

Performance was bad when we used to builder, because our function is
called for every element, but then rebuild the collection using the
builder. Vast improvement when we used the builder only for the empty
collection and then appended elements using the normal + operators. These
messed up the types a bit, giving only a base type, so we cast to the
correct type.

### Fully inferred generic multoids

Unified the cases for monoids and multoids by implicitly wrapping monoids
in multoids who have the same collection as element type.

Reformulated the types of the implicit functions so we can access the
type parameters of the collections we get (e.g. the key and value type of
map). This allowed us to completely infer multoids that go several levels
deep. We created general multoids for maps, sets, sequences and tuples.

There are situations where conflicts can occur. E.g. the Tuple2Multoid and
the Tuple2Monoid both match when the tuple values are Monoids. Haven't found
a way to resolve this automatically yet. You'll have to pick one by hand. 

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
   
### Integrating MapReduce with collections

By using a simple implicit conversion we can add mapReduce and flatMapReduce
functions to all Traversable types. This was we can chain mapReduce calls but
also mix them with the regular collection operations. By doing this we can skip
one type parameter on mapReduce because it can be inferred from the collection
it is invoked on.

## Performance

 * Reduce binned commits to time-indexed Map
   ```
   takes 160928ms
   max mem 1.65G
   Reduced 14287887 commits to 3320057 (23.24%) in 891 bins
   ```
 * Reduce binned commits to Set
   ```
   takes 148077ms
   max mem 1.4G
   Reduced 14287887 commits to 3320057 (23.24%)
   ```
 * Reduce binned commits to timeindexed SortedMap
   ```
   final mem 1.85
   done in 169659ms
   Reduced 14287887 commits to 3320057 (23.24%) in 891 bins
   ```

## Build & Run

```sh
$ cd github-relations-viz
$ ./sbt
> container:start
> ~ ;copy-resources;aux-compile
```

Now open the site's [root page](http://localhost:8080/) in your browser.
