#geotrellis.raster

>“Yes raster is faster, but raster is vaster and vector just SEEMs more 
>corrector.”
— [C. Dana Tomlin](http://uregina.ca/piwowarj/NotableQuotables.html)

##Tiles, tiles, tiles

The entire purpose of `geotrellis.raster` is to provide primitive datatypes which implement, modify, and utilize rasters. But what are rasters? A raster is just a universe of at least one tile. A tile is just a two-dimensional collection of data (and some metadata which is not super important to us right now).
Tiles are a lot like this sequence of sequences (this one's a 3x3 'tile'):
```scala
val myFirstTile = [[1,1,1],[1,2,2],[1,2,3]]
/** It probably looks more like your mental model if we stack them up:
  * [[1,1,1],
  *  [1,2,2],
  *  [1,2,3]]
  */
```
In the raster module of GeoTrellis, the base type which corresponds to our abstract notion of a tile is - wait for it - `Tile`. All of the tiles which we'll be looking into inherit from `geotrellis.raster.Tile` - glancing through its source will give you a sense of what we expect of our tiles. Here's an incomplete list of the types of things on offer (Seriously, check the source out! It *will* clarify the semantics of tiles in GeoTrellis.):
- Mapping transformations over the constituent cells
- Carrying out operations (side-effects) for each cell
- Querying a tile value
- Rescaling, resampling, cropping

We'll come back to what tiles can do in a bit. First, let's get a grip on what tiles are all about: values and spatial relationships between those values.

##Axiology in Rasterville

Axiology is the study of value, so this section is all about the types of things that can represent value in a raster. This is important because choices here are largely informed by the domain which is putatively represented by a tile.
A note on terminology: As we've already discussed, tiles are made up of squares which contain values. We'll sometimes refer to these value-boxes as 'cells'. And, just like cells in the body, though they are discrete units, they're often most interesting when looked at from a more holistic perspective - rasters encode relations between values in a uniform space and it is usually these relations which most interest us. Check out the submodules in `geotrellis.raster.op`, they are full of operations which leverage these relationships to do some cool stuff.
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

##Bringing it together

We've got all the building blocks necessary to construct our own rasters. Now, since a raster is a combination of a datatype which its cells are encoded with and their spatial arrangement, we will have to somehow combine `Tile` (which encodes our expectations about how cells sit with respect to one another) and the datatype of our choosing. Luckily, GeoTrellis has already solved this problem for us. To find fully reified tiles, check these out:
- `geotrellis.raster.BitArrayTile`
- `geotrellis.raster.ByteArrayTile`
- `geotrellis.raster.ShortArrayTile`
- `geotrellis.raster.IntArrayTile`
- `geotrellis.raster.FloatArrayTile`
- `geotrellis.raster.DoubleArrayTile`
- `geotrellis.raster.NoDataTile`

In the repl, you can try this out:
```scala
scala> IntArrayTile(Array(1,2,3),1,3)
res1: geotrellis.raster.IntArrayTile = IntArrayTile([S@338514ad,1,3)

scala> IntArrayTile(Array(1,2,3),3,1)
res2: geotrellis.raster.IntArrayTile = IntArrayTile([S@736a81de,3,1)
```













Consider [Conway's Game of Life](http://en.wikipedia.org/wiki/Conway%27s_Game_of_Life). Using the terminology we've developed thus far, Conway's Game of Life is a program which evaluates the spatial relations of cells which are capable of representing only an 'on' and an 'off' state.






