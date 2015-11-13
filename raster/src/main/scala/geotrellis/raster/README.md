#geotrellis.raster

>“Yes raster is faster, but raster is vaster and vector just SEEMS more corrector.”  
— [C. Dana Tomlin](http://uregina.ca/piwowarj/NotableQuotables.html)

##Tiles

The entire purpose of `geotrellis.raster` is to provide primitive datatypes which implement, modify, and utilize rasters. In GeoTrellis, a raster is just a tile with an associated extent (read about extents [here](../../../../../vector/src/main/scala/geotrellis/vector)). A tile is just a two-dimensional, regularly distributed collection of data.
Tiles are a lot like certain sequences of sequences (this one's quite like a 3x3 tile):
```scala
val myFirstTile = [[1,1,1],[1,2,2],[1,2,3]]
/** It probably looks more like your mental model if we stack them up:
  * [[1,1,1],
  *  [1,2,2],
  *  [1,2,3]]
  */
```
In the raster module of GeoTrellis, the base type which corresponds to our abstract notion of a tile is just `Tile`. All GeoTrellis compatible tiles will have inherited from that base class, so if you find yourself wondering what a given type of tile's powers are, that's a decent place to start your search. Here's an incomplete list of the types of things on offer (Seriously, check out [the source code](./Tile.scala)! It *will* clarify the semantics of tiles in GeoTrellis.):
- Mapping transformations of arbitrary complexity over the constituent cells
- Carrying out operations (side-effects) for each cell
- Querying a specific tile value
- Rescaling, resampling, cropping


##Working with Raster Values

One of the first questions you'll ask yourself when working with GeoTrellis is what kinds of representation best model the domain you're dealing with. What types of value do you need your raster to hold?
A note on terminology: As we've already discussed, tiles are made up of squares which contain values. We'll sometimes refer to these value-boxes as 'cells'. And, just like cells in the body, though they are discrete units, they're most interesting when looked at from a more holistic perspective - rasters encode relations between values in a uniform space and it is usually these relations which most interest us. Check out the submodules in [`geotrellis.raster.op`](./op), they are full of operations which leverage these relationships to do cool stuff (especially [`geotrellis.raster.op.focal`](./op/focal)).

Essentially, there are two types of value with differing levels of precision: integer and floating point values.

###Integer CellTypes
-  1 bit representations (`TypeBit`)
-  8 bit representations (`TypeByte`)
- 16 bit representations (`TypeShort`)
- 32 bit representations (`TypeInt`)

###Floating point CellTypes
- 16 bit representations (`TypeFloat`)
- 32 bit representations (`TypeDouble`)

In general, you'll want to choose the smallest representation which is fully capable of representing your domain so that your rasters require as little storage and memory as possible. If even one in one billion cells in a tile requires Double precision floating point representation, every cell in the entire tile must be of type `Double` and every cell will, consequently, require 64 bits of memory. GeoTrellis is a performance-oriented library, so if you're running into performance bottlenecks like this within GeoTrellis, it is quite likely that you'll need to either throw more computational resources at your problem or set your sights a bit lower in terms of precision (perhaps you can live with 32bit floating point values).

There's one final type a cell which I've failed to mention: `NODATA`. `NODATA` cells are provided to represent the difference between, e.g., some location's thermostat reading at 0 degrees and said location's thermostat being broken such that no data is available. Conceptually, there's no reason that this arrangement wouldn't work for `TypeBit`. In practice, however, bits just aren't expressive enough. `0` and `1` exhaust `TypeBit`'s possible values. This means that for `NODATA` to exist, we'd only be able to represent the difference between cells for which thermostat measurements exist and those cells for which our thermostat is broken. Effectively then, the `NODATA` vs `0` distinction in a `TypeBit` tile collapses and we're left with a tile semantically indistinct from a tile for which the 'data' is whether or not measurements exist at a given location.  

######Macros in `geotrellis.raster`:  
>You may have noticed that 'package.scala' makes a number of calls to `macro`. These calls tell the compiler to insert the appropriate macros during the compilation of the file they're in. Macros in GeoTrellis serve a twofold purpose:  
>1. To simplify development and usage of GeoTrellis by obviating the need to call the correct type of operation to check for `NODATA` cell types.  
>2. To improve performance by having the compiler sort out what is and isn't `NODATA` so that our code at runtime can avoid costly (from shear amount) calls to, e.g., Int.minValue.  

##Bringing it together

With a grasp of tiles and their values, we've got all the conceptual tools necessary to construct our own rasters. Now, since a raster is a combination of a datatype with which its cells are encoded and their spatial arrangement, we will have to somehow combine `Tile` (which encodes our expectations about how cells sit with respect to one another) and the datatype of our choosing. Luckily, GeoTrellis has already solved this problem for us. To keep us sane, the wise maintainers of GeoTrellis have organized `geotrellis.raster` such that fully reified tiles sit at the bottom of an inheritance chain. Let's explore how that inheritance shakes out so that you will know where to look when your intuitions lead you astray:  
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

####Ecce tile
It looks like there are two different chains of inheritance here (`IntBasedArrayTile` and `DoubleBasedArrayTile`). Let's first look at what they share:
1. `MutableArrayTile` adds some nifty methods for in-place manipulation of cells (GeoTrellis is about performance, so this minor affront to the gods of immutability can be forgiven).
From MutableArrayTile.scala:
```scala
trait MutableArrayTile extends ArrayTile
```  
2. One level up is `ArrayTile`. It's handy because it implements the behavior which largely allows us to treat our tiles like big, long arrays of (arrays of) data. They also have the trait `Serializable`, which is neat any time you can't completely conduct your business within the neatly defined space-time of the JVM processes which are running on a single machine (this is the point of GeoTrellis' Spark integration).
From ArrayTile.scala:
```scala
trait ArrayTile extends Tile with Serializable
```  
3. At the top rung in our abstraction ladder we have `Tile`. You might be surprised how much we can say about a tile at the base of its inheritance tree, so (at risk of sounding redundant) the source is worth spending some time on.
From Tile.scala
```scala
trait Tile
```  
Cool. That wraps up one half of the inheritance. But how about that the features they don't share? As it turns out, each reified tile's second piece of inheritance merely implements methods for dealing with their constitutent `CellType`s.
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
Mostly we've been looking to tiny snippets of source, but the two above are the entire files. All they do is:
1. Tell the things that inherit from them that they'd better define methods for application and updating of values that look like their cells if they want the compiler to be happy.  
2. Tell the things that inherit from them exactly how to take values which don't look like their cells (int-like things for `DoubleBasedArray` and double-like things for `IntBasedArray`) and turn them into types they find more palatable.

That was lucky. It looks like `CellType` is another one of those things that we can *mostly* ignore once we've settled on which one is proper for our domain. After all, it appears as though there's very little difference between tiles that prefer int-like things and tiles that prefer double-like things.

>CAUTION: While it is true, in general, that operations are `CellType` agnostic, both `get` and `getDouble` are methods implemented on `Tile`. In effect, this means that you'll want to be careful when querying values. If you're working with int-like `CellType`s, probably use `get`. If you're working with float-like `CellType`s, usually you'll want `getDouble`.

####Taking our tiles out for a spin
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
######Constructing a Raster
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
"     1
     2
     3

"

scala> res2.asciiDraw()
res4: String =
"     1     2     3
     4     5     6
     7     8     9

"
```
That's probably enough to get started. `geotrellis.raster` is a pretty big place, so you'll benefit from spending a few hours playing with the tools it provides.

##Submodules

Here's what you can expect to find in `geotrellis.raster`:
- [`geotrellis.raster.compression`](./compression) defines functions for squeezing extra bits out of serialized formats
- [`geotrellis.raster.histogram`](./histogram) defines the classes used for analyzing the cell-value distributions of tiles (of special interest is `geotrellis.raster.histogram.FastMapHistogram`, though actual usage is typically done through the methods exposed in [`geotrellis.raster.op.stats`](./op/stats)
- [`geotrellis.raster.interpolation`](./interpolation) - TODO
- [`geotrellis.raster.io`](./io) follows the package naming conventions elsewhere in GeoTrellis - `io` handles serialization for transmission and storage of rasters
- [`geotrellis.raster.mosaic`](./mosaic) defines `MosaicBuilder`, which facilitates the merging of rasters
- [`geotrellis.raster.op`](./op) provides a slew of options, categorized by the type of relationship computed over (is it an operation which looks only at local relationships of cells or one which looks at all the cells of a raster together to compute its result?)
- [`geotrellis.raster.rasterize`](./rasterize) allows for conversion from raster to vector data
- [`geotrellis.raster.render`](./render) allows for conversion from raster to the web-friendly png format
- [`geotrellis.raster.reproject`](./reproject) defines methods for translating between projections 