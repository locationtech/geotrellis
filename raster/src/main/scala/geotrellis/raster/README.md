#geotrellis.raster

>“Yes raster is faster, but raster is vaster and vector just SEEMS more corrector.”
— [C. Dana Tomlin](http://uregina.ca/piwowarj/NotableQuotables.html)

##Tiles, tiles, tiles

The entire purpose of `geotrellis.raster` is to provide primitive datatypes which implement, modify, and utilize rasters. But what are rasters? In GeoTrellis, a raster is just a tile with an associated extent (read about extents [here](../../../../../vector/src/main/scala/geotrellis/vector)). The as-yet awake reader will have noticed that we've traded one obscurity for another. What, then, is meant 'tile'? A tile is just a two-dimensional collection of data.
Tiles are a lot like this sequence of sequences (this one's like a 3x3 'tile'):
```scala
val myFirstTile = [[1,1,1],[1,2,2],[1,2,3]]
/** It probably looks more like your mental model if we stack them up:
  * [[1,1,1],
  *  [1,2,2],
  *  [1,2,3]]
  */
```
In the raster module of GeoTrellis, the base type which corresponds to our abstract notion of a tile is - wait for it - `Tile`. The only tiles that hang out in these parts will have inherited from that base class, so if you find yourself wondering what their powers are, that's a decent place to look. Here's an incomplete list of the types of things on offer (Seriously, check out [this source code](./Tile.scala)! It *will* clarify the semantics of tiles in GeoTrellis.):
- Mapping transformations over the constituent cells
- Carrying out operations (side-effects) for each cell
- Querying a tile value
- Rescaling, resampling, cropping

We'll come back to what tiles of various types can do in a bit. First, let's get a grip on what tiles are all about: values and spatial relationships between those values.

##Axiology in Rasterville

Axiology is the study of value, so this section is about the types of things that can represent value in a raster. This is important because choices here are largely informed by the domain which is putatively represented by a tile.
A note on terminology: As we've already discussed, tiles are made up of squares which contain values. We'll sometimes refer to these value-boxes as 'cells'. And, just like cells in the body, though they are discrete units, they're most interesting when looked at from a more holistic perspective - rasters encode relations between values in a uniform space and it is usually these relations which most interest us. Check out the submodules in `geotrellis.raster.op`, they are full of operations which leverage these relationships to do cool stuff.

Back to the question of values in cells. Essentially, there are two types of value with differing levels of precision.

Within the integer types, we have:
-  1 bit representations (`TypeBit`)
-  8 bit representations (`TypeByte`)
- 16 bit representations (`TypeShort`)
- 32 bit representations (`TypeInt`)

Within the floating point types, we have:
- 16 bit representations (`TypeFloat`)
- 32 bit representations (`TypeDouble`)

In general, you'll want to choose the smallest representation which is fully capable of representing your domain so that your rasters require as little storage and memory as possible. If even one in one million cells requires Double precision floating point representation, every cell in the entire tile must be of type `Double`.

There's one final type a cell which I've failed to mention: `NODATA`. `NODATA` is provided to represent the difference between, for example, some location's thermostat reading at 0 degrees and said location's thermostat being broken such that no data is available. Conceptually, there's no reason that this arrangement wouldn't work for `TypeBit`. In practice, however, bits just aren't expressive enough. `0` and `1` exhaust `TypeBit`'s possible values.
>Macros in `geotrellis.raster`: You may have noticed that 'package.scala' makes a number of calls to `macro`. These calls tell the compiler to insert the appropriate macros during the compilation of the file they're in. Macros in GeoTrellis serve a twofold purpose:
>1. To simplify development and usage of GeoTrellis by obviating the need to call the correct type of operation to check for `NODATA` cell types.
>2. To improve performance by having the compiler sort out what is and isn't `NODATA` so that our code at runtime can avoid costly (from shear amount) calls to, e.g., Int.minValue.

##Bringing it together

We've got all the building blocks necessary to construct our own rasters. Now, since a raster is a combination of a datatype which its cells are encoded with and their spatial arrangement, we will have to somehow combine `Tile` (which encodes our expectations about how cells sit with respect to one another) and the datatype of our choosing. Luckily, GeoTrellis has already solved this problem for us. To keep the Great Chain of Being intact, the wise maintainers of GeoTrellis have organized `geotrellis.raster` such that fully reified tiles sit at the bottom of an inheritance graph. Let's explore how that inheritance shakes out so that you will know where to look when your intuitions lead you astray:
From IntArrayTile.scala:
```scala
final case class IntArrayTile(array: Array[Int], cols: Int, rows: Int) 
    extends MutableArrayTile with IntBasedArrayTile
```
From DoubleArrayTile.scala:
```scala
final case class DoubleArrayTile(array: Array[Double], cols: Int, rows: Int)
  extends MutableArrayTile with DoubleBasedArray
```

####Ecce tile
Whoa! Looks like there are two different chains of inheritance here. Let's first look at what they share:
1. `MutableArrayTile` adds some nifty methods for in-place manipulation of cells (GeoTrellis is about performance, so this minor affront to the gods of immutability is forgivable).
From MutableArrayTile.scala:
```scala
trait MutableArrayTile extends ArrayTile
```
2. One level up is `ArrayTile`. It's handy because it implements the behavior which largely allows us to treat our tiles like big, long arrays of data. They also have the trait `Serializable`, which is neat any time you can't completely conduct your business within the neatly defined space-time of the JVM process which is running your code.
From ArrayTile.scala:
```scala
trait ArrayTile extends Tile with Serializable
```
3. At the top rung in our abstraction ladder we have `Tile`. You might be surprised how much we can say about a tile at the base of its inheritance tree, so (as always) the source is worth spending some time on.
From Tile.scala
```scala
trait Tile
```

Cool. That wraps up one half of the inheritance. But how about that the features they don't share? As it turns out, each reified tile's second inheritance merely implements methods for dealing with their constitutent `CellType`s.
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
Mostly we've been looking to tiny snippets of source, but the two right above are it. All they do is:
1. Tell the things that inherit from them that they'd better define methods for application and updating of values that look like their cells if they want the compiler to be happy.
2. Tell the things that inherit from them exactly how to take values which don't look like their cells (int-like things for `DoubleBasedArray` and double-like things for `IntBasedArray`) and turn them into types they find more palatable.

That was lucky. It looks like `CellType` is another one of those things that we can *mostly* ignore once we've settled on which one is proper for our domain. After all, it appears as though there's very little difference between tiles that prefer int-like things and tiles that prefer double-like things.
- `geotrellis.raster.BitArrayTile`
- `geotrellis.raster.ByteArrayTile`
- `geotrellis.raster.ShortArrayTile`
- `geotrellis.raster.IntArrayTile`
- `geotrellis.raster.FloatArrayTile`
- `geotrellis.raster.DoubleArrayTile`
- `geotrellis.raster.NoDataTile`

>CAUTION: While it is true, in general, that operations are `CellType` agnostic, both `get` and `getDouble` are methods implemented on `Tile`. In effect, this means that you'll really want to be careful when querying values: Ints are generally quicker while Doubles offer greater precision. If you're working with int-like `CellType`s, always use `get`. If you're working with double-like `CellType`s, usually you'll want `getDouble`.

####Taking our tiles out for a spin
In the repl, you can try this out:
```scala
scala> IntArrayTile(Array(1,2,3),1,3)
res0: geotrellis.raster.IntArrayTile = IntArrayTile([S@338514ad,1,3)

scala> IntArrayTile(Array(1,2,3),3,1)
res1: geotrellis.raster.IntArrayTile = IntArrayTile([S@736a81de,3,1)

scala> IntArrayTile(Array(1,2,3,4,5,6,7,8,9),3,3)
res2: geotrellis.raster.IntArrayTile = IntArrayTile([I@5466441b,3,3)
```
Here's a fun method for exploring your tiles:
```scala
scala> res0.asciiDraw()  // This is kind of an old method
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
- `geotrellis.raster.compression` defines functions for squeezing extra bits out of serialized formats
- `geotrellis.raster.histogram` defines the classes used for analyzing the cell-value distributions of tiles (of special interest is `geotrellis.raster.histogram.FastMapHistogram`, though actual usage is typically done through the methods exposed in `geotrellis.raster.op.stats`
- `geotrellis.raster.interpolation` - TODO
- `geotrellis.raster.io` follows the package naming conventions elsewhere in GeoTrellis - `io` handles serialization for transmission and storage of rasters
- `geotrellis.raster.mosaic` defines `MosaicBuilder`, which facilitates the merging of rasters
- `geotrellis.raster.op` provides a slew of options, categorized by the type of relationship computed over (is it an operation which looks only at local relationships of cells or one which looks at all the cells of a raster together to compute its result?)
- `geotrellis.raster.rasterize` allows for conversion from raster to vector data
- `geotrellis.raster.render` allows for conversion from raster to the web-friendly png format
- `geotrellis.raster.reproject` defines methods for translating between projections 
