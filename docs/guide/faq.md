## How do I install geotrellis?

Sadly, you can't.  
GeoTrellis is a developer toolkit/library/framework used to develop applications 
in scala against geospatial data large and small. So to use it, you need to 
rely on it in a scala application that you develop. Read through our tutorials 
to learn about the dependencies required to use GeoTrellis. 


## I need to convert a `Tile`'s `CellType`, which methods should I use?

There are two distinct flavors of 'conversion' which GeoTrellis supports
for moving between `CellType`s: `convert` and `interpretAs`. In what
follows we will try to limit any confusion about just what differentiates
these two methods and describe which should be used under what circumstances.  

In previous sections, we've said that the `CellType` is just a piece of
metadata carried around alongside a `Tile` which helps GeoTrellis to
keep track of how that `Tile`'s array should be interacted with. The
distinction between `interpretAs` and `convert` relates to how smart
GeoTrellis should be while swapping out one `CellType` for another.  

Broadly, `convert` assumes that your `Tile`'s `CellType` is accurate and
that you'd like the semantics of your `Tile` to remain invariant under
the conversion in question. For example, imagine that we've got
categorical data whose cardinality is equal to the cardinality of `Byte`
(254 assuming we reserved a spot for `NoData`). Let's fiat, too, that
the `CellType` we're using is `ByteConstantNoData`. What happens if we want
to add a 255th category? Unless we abandon `NoData` (usually not the
right move), it would seem we're out of options so long as we use
`ByteCells`. Instead, we should call `convert` on that tile and tell it
that we'd like to transpose all `Byte` values to `Short` values. All of
the numbers will remain the same with the exception of any
`Byte.MinValue` cells, which will be turned into `Short.MinValue` in
accordance with the new `CellType`'s chosen `NoData` value. This frees
up quite a bit of extra room for categories and allows us to continue
working with our data in nearly the same manner as before conversion.  

`interpretAs` is a method that was written to resolve a different
problem. If your `Tile` is associated with an incorrect `CellType` (as
can often happen when reading GeoTIFFs that lack proper, accurate headers),
`interpretAs` provides a means for attaching the correct metadata to
your `Tile` *without trusting the pre-interpretation metadata*. The
"conversion" carried out through `interpretAs` does *not* try to do
anything intelligent. There can be no guarantee that meaning
is preserved through reinterpretation - in fact, the primary use case
for `interpretAs` is to attach the correct metadata to a `Tile` which is
improperly labelled for whatever reason.  

An interesting consequence is that you can certainly move between
data types (not just policies for handling `NoData`) by way of
`interpretAs` but that, because the original metadata is not accurate,
the default, naive conversion (`_.toInt`, `_.toFloat`, etc.) must be
depended upon.  

```scala
/** getRaw is a method that allows us to see the values regardless of
if, semantically, they are properly treated as non-data. We use it here
simply to expose the mechanics of the transformation 'under the hood' */

val myData = Array(42, 2, 3, 4)
val tileBefore = IntArrayTile(myData, 2, 2, IntUserDefinedNoDataValue(42))

/** While the value in (0, 0) is NoData, it is now 1 instead of 42
  *  (which matches our new CellType's expectations)
  */
val converted = tileBefore.convert(IntUserDefinedNoData(1))
assert(converted.getRaw.get(0, 0) != converted.get(0, 0))

/** Here, the first value is still 42. But because the NoData value is
  *  now 1, the first value is no longer treated as NoData
  *  (which matches our new CellType's expectations) */
val interpreted = tileBefore.interpretAs(IntUserDefinedNoData(1))
assert(interpreted.getRaw.get(0, 0) == interpreted.get(0, 0))
```

TL;DR: If your `CellType` is just wrong, reinterpret the meaning of your
underlying cells with a call to `interpretAs`. If you trust your
`CellType` and wish for its semantics to be preserved through
transformation, use `convert`.  

