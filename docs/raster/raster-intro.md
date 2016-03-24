# geotrellis.raster

> “Yes raster is faster, but raster is vaster and vector just SEEMS more corrector.”
— [C. Dana Tomlin](http://uregina.ca/piwowarj/NotableQuotables.html)

## Raster vs Tile

The entire purpose of `geotrellis.raster` is to provide primitive datatypes which implement,
modify, and utilize rasters. In GeoTrellis, a raster is just a tile with an associated extent
(read about extents [here](../vector/vector-intro.md)). A tile is just a two-dimensional,
collection of evenly spaced data. Tiles are a lot like certain
sequences of sequences (this array of arrays is like a 3x3 tile):

```scala
val myFirstTile = [[1,1,1],[1,2,2],[1,2,3]]
/** It probably looks more like your mental model if we stack them up:
  * [[1,1,1],
  *  [1,2,2],
  *  [1,2,3]]
  */
```

In the raster module of GeoTrellis, the base type of tile is just `Tile`. All GeoTrellis
compatible tiles will have inherited from that base class, so if you find yourself wondering
what a given type of tile's powers are, that's a decent place to start your search. Here's an
incomplete list of the types of things on offer (Seriously, check out
[the source code](../../raster/src/main/scala/geotrellis/raster/Tile.scala)! It *will* clarify the
semantics of tiles in GeoTrellis.):
- Mapping transformations of arbitrary complexity over the constituent cells
- Carrying out operations (side-effects) for each cell
- Querying a specific tile value
- Rescaling, resampling, cropping

As we've already discussed, tiles are made up of squares which
contain values. We'll sometimes refer to these
value-boxes as 'cells'. And, just like cells in the body, though
they are discrete units, they're most interesting when looked at
from a more holistic perspective - rasters encode relations between
values in a uniform space and it is usually these relations which
most interest us. The code found in the `mapalgebra` submodule —
discussed [here](./map-algebra.md) — is all about exploiting these
spatial relations


## Working with cell values

One of the first questions you'll ask yourself when working with GeoTrellis is what kinds
of representation best model the domain you're dealing with. What types of value do you need
your raster to hold? This question is the province of GeoTrellis
`CellType`s.
Check out the [documentation on `CellType`s](./celltype.md) for more.

## Building Your Own Tiles

With a grasp of tiles and `CellType`s, we've got all the
conceptual tools necessary to construct our own tiles. Now, since
a tile is a combination of a `CellType` with which its cells are
encoded and their spatial arrangement, we will have to somehow combine
`Tile` (which encodes our expectations about how cells sit with
respect to one another) and the datatype of our choosing. Luckily,
GeoTrellis has done this for us. To keep its users sane,
the wise maintainers of GeoTrellis have organized `geotrellis.raster`
such that fully reified tiles sit at the bottom of an pretty simple
inheritance chain. Let's explore that inheritance so that you will
know where to look when your intuitions lead you astray:  

From IntArrayTile.scala:
```scala
final case class IntArrayTile(array: Array[Int], cols: Int, rows: Int)
    extends MutableArrayTile with IntBasedArrayTile
```

From DoubleArrayTile.scala:
```scala
final case class DoubleArrayTile(array: Array[Double], cols: Int, rows: Int)
  extends MutableArrayTile with DoubleBasedArrayTile
```  

#### Tile inheritance structure
It looks like there are two different chains of inheritance here (`IntBasedArrayTile` and
`DoubleBasedArrayTile`). Let's first look at what they share:
1. `MutableArrayTile` adds some nifty methods for in-place manipulation of cells (GeoTrellis is
about performance, so this minor affront to the gods of immutability can be forgiven).
From MutableArrayTile.scala:

```scala
trait MutableArrayTile extends ArrayTile
```  

2. One level up is `ArrayTile`. It's handy because it implements the behavior which largely allows
us to treat our tiles like big, long arrays of (arrays of) data. They also have the trait
`Serializable`, which is neat any time you can't completely conduct your business within the
neatly defined space-time of the JVM processes which are running on a single machine (this is the
point of GeoTrellis' Spark integration).
From ArrayTile.scala:

```scala
trait ArrayTile extends Tile with Serializable
```  

3. At the top rung in our abstraction ladder we have `Tile`. You might be surprised how much we
can say about tile behavior from the base of its inheritance tree, so (at risk of sounding
redundant) the source is worth spending some time on.
From Tile.scala

```scala
trait Tile
```  

Cool. That wraps up one half of the inheritance. But how about that the features they don't share?
As it turns out, each reified tile's second piece of inheritance merely implements methods for
dealing with their constitutent `CellType`s.
From IntBasedArrayTile.scala:

```scala
trait IntBasedArrayTile {
  def apply(i:Int):Int
  def update(i:Int, z:Int):Unit

  def applyDouble(i:Int):Double = i2d(apply(i))
  def updateDouble(i:Int, z:Double):Unit = update(i, d2i(z))
}
```

From DoubleBasedArrayTile.scala:

```scala
trait DoubleBasedArray {
  def apply(i:Int):Int = d2i(applyDouble(i))
  def update(i:Int, z:Int):Unit = updateDouble(i, i2d(z))

  def applyDouble(i:Int):Double
  def updateDouble(i:Int, z:Double):Unit
}
```

Mostly we've been looking to tiny snippets of source, but the two above are the entire files.
All they do is:
1. Tell the things that inherit from them that they'd better define methods for application
and updating of values that look like their cells if they want the compiler to be happy.
2. Tell the things that inherit from them exactly how to take values which don't look like
their cells (int-like things for `DoubleBasedArray` and double-like things for
`IntBasedArray`) and turn them into types they find more palatable.

As it turns out, `CellType` is one of those things that we can *mostly* ignore
once we've settled on which one is proper for our domain. After all, it appears
as though there's very little difference between tiles that prefer int-like
things and tiles that prefer double-like things.

>**CAUTION**: While it is true, in general, that operations are `CellType` agnostic,
both `get` and `getDouble` are methods implemented on `Tile`. In effect, this
means that you'll want to be careful when querying values. If you're working with
int-like `CellType`s, probably use `get`. If you're working with float-like
`CellType`s, usually you'll want `getDouble`.

#### Taking our tiles out for a spin
In the repl, you can try this out:

```scala
import geotrellis.raster._
import geotrellis.vector._

scala> IntArrayTile(Array(1,2,3),1,3)
res0: geotrellis.raster.IntArrayTile = IntArrayTile([S@338514ad,1,3)

scala> IntArrayTile(Array(1,2,3),3,1)
res1: geotrellis.raster.IntArrayTile = IntArrayTile([S@736a81de,3,1)

scala> IntArrayTile(Array(1,2,3,4,5,6,7,8,9),3,3)
res2: geotrellis.raster.IntArrayTile = IntArrayTile([I@5466441b,3,3)
```

#### Constructing a Raster

```scala
scala> Extent(0, 0, 1, 1)
res4: geotrellis.vector.Extent = Extent(0.0,0.0,1.0,1.0)

scala> Raster(res2, res4)
res5: geotrellis.raster.Raster = Raster(IntArrayTile([I@7b47ab7,1,3),Extent(0.0,0.0,1.0,1.0))
```

Here's a fun method for exploring your tiles:

```scala
scala> res0.asciiDraw()
res3: String =
"    1
     2
     3
"

scala> res2.asciiDraw()
res4: String =
"    1     2     3
     4     5     6
     7     8     9
"
```

That's probably enough to get started. `geotrellis.raster` is a pretty big place, so you'll
benefit from spending a few hours playing with the tools it provides.

