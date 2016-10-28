Geographical Information Systems (GIS), like any specialized field, has a wealth
of jargon and unique concepts. When represented in software, these concepts
can sometimes be skewed or expanded from their original forms. We give a thorough definition of many of the core concepts
here, while referencing the Geotrellis objects and source files backing them.

This document aims to be informative to new and experienced GIS users alike.
If GIS is brand, brand new to you, [this document](https://www.gislounge.com/what-is-gis/) is
 a useful high level overview.

Raster Data
===========

Vector Data
===========

Vector Tiles
===========

Tile Layers
===========

`RDD[(K, V)] with Metadata[M]`

Keys and Key Indexes
====================

Tiles
=====

Cell Types
==========

**What is a Cell Type?**

- A `CellType` is a data type plus a policy for handling cell values that
  may contain no data.
- By 'data type' we shall mean the underlying numerical representation
  of a `Tile`'s cells.
- `NoData`, for performance reasons, is not represented as a value outside
  the range of the underlying data type (as, e.g., `None`) - if each cell in some
  tile is a `Byte`, the `NoData` value of that tile will exist within the range
  [`Byte.MinValue` (-128), `Byte.MaxValue` (127)].
- If attempting to convert between `CellTypes`, see
  [this note](../celltype-conversion.md) on `CellType` conversions.

|             |     No NoData    |         Constant NoData        |        User Defined NoData        |
|-------------|:----------------:|:------------------------------:|:---------------------------------:|
| BitCells    | `BitCellType`    | N/A                            | N/A                               |
| ByteCells   | `ByteCellType`   | `ByteConstantNoDataCellType`   | `ByteUserDefinedNoDataCellType`   |
| UbyteCells  | `UByteCellType`  | `UByteConstantNoDataCellType`  | `UByteUserDefinedNoDataCellType`  |
| ShortCells  | `ShortCellType`  | `ShortConstantNoDataCellType`  | `ShortUserDefinedNoDataCellType`  |
| UShortCells | `UShortCellType` | `UShortConstantNoDataCellType` | `UShortUserDefinedNoDataCellType` |
| IntCells    | `IntCellType`    | `IntConstantNoDataCellType`    | `IntUserDefinedNoDataCellType`    |
| FloatCells  | `FloatCellType`  | `FloatConstantNoDataCellType`  | `FloatUserDefinedNoDataCellType`  |
| DoubleCells | `DoubleCellType` | `DoubleConstantNoDataCellType` | `DoubleUserDefinedNoDataCellType` |

The above table lists `CellType` `DataType`s in the leftmost column
and `NoData` policies along the top row.
A couple of points are worth
making here:

1. Bits are incapable of representing on, off, *and* some `NoData`
   value. As a consequence, there is no such thing as a Bit-backed tile
   which recognizes `NoData`.
2. While the types in the 'No NoData' and 'Constant NoData' are simply
   singleton objects that are passed around alongside tiles, the greater
   configurability of 'User Defined NoData' `CellType`s means that they
   require a constructor specifying the value which will count as
   `NoData`.

Let's look to how this information can be used:
```scala
/** Here's an array we'll use to construct tiles */
val myData = Array(42, 1, 2, 3)

/** The GeoTrellis-default integer CellType */
val defaultCT = IntConstantNoDataCellType
val normalTile = IntArrayTile(myData, 2, 2, defaultCT)

/** A custom, 'user defined' NoData CellType for comparison; we will
treat 42 as `NoData` for this one */
val customCellType = IntUserDefinedNoDataValue(42)
val customTile = IntArrayTile(myData, 2, 2, customCellType)

/** We should expect that the first tile has the value 42 at (0, 0)
because Int.MinValue is the GeoTrellis-default `NoData` value for
integers */
assert(normalTile.get(0, 0) == 42)
assert(normalTile.getDouble(0, 0) == 42.0)

/** Here, the result is less obvious. Under the hood, GeoTrellis is
inspecting the value to be returned at (0, 0) to see if it matches our
custom `NoData` policy and, if it matches (it does), return Int.MinValue
(no matter your underlying type, `get` on a tile will return an `Int`
and `getDouble` will return a `Double`.

The use of Int.MinValue and Double.NaN is a result of those being the
GeoTrellis-blessed values for NoData - below, you'll find a chart that
lists all such values in the rightmost column */
assert(customTile.get(0, 0) == Int.MinValue)
assert(customTile.getDouble(0, 0) == Double.NaN)
```

**Why you should care**

In most programming contexts, it isn't all that useful to think carefully
about the number of bits necessary to represent the data passed around
by a program. A program tasked with keeping track of all the birthdays
in an office or all the accidents on the New Jersey turnpike simply
doesn't benefit from carefully considering whether the allocation of
those extra few bits is *really* worth it. The costs for any lack of
efficiency are more than offset by the savings in development time and
effort. This insight - that computers have become fast enough for us to
be forgiven for many of our programming sins - is, by now, truism.  

An exception to this freedom from thinking too hard about
implementation details is any software that tries, in earnest, to
provide the tools for reading, writing, and working with large arrays of
data. Rasters certainly fit the bill. Even relatively modest rasters
can be made up of millions of underlying cells. Additionally,
the semantics of a raster imply that each of these cells shares
an underlying data type. These points - that rasters are made up
of a great many cells and that they all share a backing
data type - jointly suggest that a decision regarding the underlying
data type could have profound consequences. More on these consequences
[below](#cell-type-performance).  

Compliance with the GeoTIFF standard is another reason that management
of cell types is important for GeoTrellis. The most common format for
persisting a raster is the [GeoTIFF](https://trac.osgeo.org/geotiff/).
A GeoTIFF  is simply an array of data along with some useful tags
(hence the 'tagged' of 'tagged image file format'). One of these
tags specifies the size of each cell and how those bytes should be
interpreted (i.e. whether the data for a byte includes its
sign - positive or negative - or whether it counts up from 0 - and
is therefore said to be 'unsigned').  

In addition to keeping track of the memory used by each cell in a `Tile`,
the cell type is where decisions about which values count as data (and
which, if any, are treated as `NoData`). A value recognized as `NoData`
will be ignored while mapping over tiles, carrying out focal operations
on them, interpolating for values in their region, and just about all of
the operations provided by GeoTrellis for working with `Tile`s.  


**Cell Type Performance**

There are at least two major reasons for giving some thought to the
types of data you'll be working with in a raster: persistence and
performance.  

Persistence is simple enough: smaller datatypes end up taking less space
on disk. If you're going to represent a region with only `true`/`false`
values on a raster whose values are `Double`s, 63/64 bits will be wasted.
Naively, this means somewhere around 63 times less data than if the most
compact form possible had been chosen (the use of `BitCells` would
be maximally efficient for representing the bivalent nature of boolean
values). See the chart below for a sense of the relative sizes of these
cell types.  

The performance impacts of cell type selection matter in both a local
and a distributed (spark) context. Locally, the memory footprint will mean
that as larger cell types are used, smaller amounts of data can be held in
memory and worked on at a given time and that more CPU cache misses are to be
expected. This latter point - that CPU cache misses will increase - means that
more time spent shuffling data from the memory to the processor (which
is often a performance bottleneck). When running programs that
leverage spark for compute distribution, larger data types mean more
data to serialize and more data send over the (very slow, relatively
speaking) network.  

In the chart below, `DataType`s are listed in the leftmost column and
important characteristics for deciding between them can be found to the
right. As you can see, the difference in size can be quite stark depending on
the cell type that a tile is backed by. That extra space is the price
paid for representing a larger range of values. Note that bit cells
lack the sufficient representational resources to have a `NoData` value.  

|             | Bits / Cell | 512x512 Raster (mb) |     Range (inclusive)     | GeoTrellis NoData Value |
|-------------|:-----------:|---------------------|:-------------------------:|-------------------------|
| BitCells    | 1           | 0.032768            | [0, 1]                    |                     N/A |
| ByteCells   | 8           | 0.262144            | [-128, 128]               |                    -128 |
| UbyteCells  | 8           | 0.262144            | [0, 255]                  |                       0 |
| ShortCells  | 16          | 0.524288            | [-32768, 32767]           |                  -32768 |
| UShortCells | 16          | 0.524288            | [0, 65535]                |                       0 |
| IntCells    | 32          | 1.048576            | [-2147483648, 2147483647] |             -2147483648 |
| FloatCells  | 32          | 1.048576            | [-3.40E38, 3.40E38]       |               Float.NaN |
| DoubleCells | 64          | 2.097152            | [-1.79E308, 1.79E308]     |              Double.NaN |

One final point is worth making in the context of `CellType`
performance: the `Constant` types are able to depend upon macros which
inline comparisons and conversions. This minor difference can certainly
be felt while iterating through millions and millions of cells. If possible, Constant
`NoData` values are to be preferred. For convenience' sake, we've
attempted to make the GeoTrellis-blessed `NoData` values as unobtrusive
as possible a priori.  

Projections
===========

**What is a projection?**

In GIS, a *projection* is a mathematical transformation of
Latitude/Longitude coordinates on a sphere onto some other flat plane. Such
a plane is naturally useful for representing a map of the earth in 2D. A
projection is defined by a *Coordinate Reference System* (CRS), which holds
some extra information useful for reprojection. CRSs themselves have static
definitions, have agreed-upon string representations, and are usually made
public by standards bodies or companies. They can be looked up at
[SpatialReference.org](http://spatialreference.org/).

A *reprojection* is the transformation of coorindates in one CRS to another.
To do so, coordinates are first converted to those of a sphere. Every CRS
knows how to convert between its coordinates and a sphere's, so a
transformation `CRS.A -> CRS.B -> CRS.A` is actually `CRS.A -> Sphere ->
CRS.B -> Sphere -> CRS.A`. Naturally some floating point error does
accumulate during this process.


**Data structures:** `CRS`, `LatLng`, `WebMercator`, `ConusAlbers`

**Sources:** `geotrellis.proj4.{CRS, LatLng, WebMercator, ConusAlbers}`

Within the context of Geotrellis, the main projection-related object is the
`CRS` trait. It stores related `CRS` objects from underlying libraries, and
also provides the means for defining custom reprojection methods, should the
need arise. It's companion object provides convenience functions for
creating `CRS`s. Geotrellis currently has three `object`s that implement the
`CRS` trait: `LatLng`, `WebMercator`, and `ConusAlbers`.

**What can CRSs do?**

They can be transformed back into their String representations:

self => toWKT, toProj4String

**How are CRSs used throughout Geotrellis?**

`CRS`s are stored in the `*ProjecedExtent` classes and are used chiefly
to define how reprojections should operate. Example:

```scala
val wm = Line(...)  // A `LineString` vector object in WebMercator.
val ll: Line = wm.reproject(WebMercator, LatLng)  // The Line reprojected into LatLng.

```


Extents
=======


**Data structures:** `Extent`, `ProjectedExtent`, `TemporalProjectedExtent`,
`GridExtent`, `RasterExtent`

**Sources:** `geotrellis.vector.Extent`,
`geotrellis.vector.reproject.Reproject`,
`geotrellis.spark.TemporalProjectExtent`,
`geotrellis.raster.{ GridExtent, RasterExtent }`,
`geotrellis.raster.reproject.ReprojectRasterExtent`

**What is an extent?**

An `Extent` is a rectangular section of a 2D projection of the Earth. It is
represented by two coordinate pairs that are its "min" and "max" corners in
some Coorindate Reference System. "min" and "max" here are CRS
specific, as the location of the point `(0,0)` varies between different CRS.
An Extent can also be referred to as a *Bounding Box*.

Within the context of Geotrellis, the points within an `Extent` always
implicitely belong to some `CRS`, while a `ProjectedExtent` holds both the
original `Extent` and its current `CRS`. If you ever wish to reproject an
extent, you'd need the original `CRS` and hence a `ProjectedExtent`.

**What can Extents do?**

Extents can perform operations on themselves and other objects.

self => expansion, translation, reprojection

other: Extent => distance, intersection

other: Point => contains

**What are the other `*Extent` types?**

A `GridExtent` is any `Extent` which contains an extra internal grid. Grid
coordinates follow Graphics / Matrix conventions, where `(0,0)` is at the
top-left. The cells of this grid are usually larger than the individual
points of the underlying map.

A `GridExtent` specific to rasters, where the underlying map is some image
(possibly held in an `Array[Byte]`) is called a `RasterExtent`.
RasterExtents are used heavily in the `raster` subproject. Both `GridExtent`
and `RasterExtent` can be reprojected.

**How are Extents used throughout Geotrellis?**

Extents are held by `LayoutDefinition`s, which in turn are used heavily in
Raster reading, writing, and reprojection.

**How does reprojection work?**

Below is the rough call stack when projecting an `Extent`. It assumes you're
starting with a `ProjectExtent` so that the original `CRS` is available.

```
ProjectedExtent.reproject(CRS)
ReprojectExtent(Extent)  // implicit class wrapping
ReprojectExtent.reproject(CRS, CRS)
Reproject.apply(Extent, CRS, CRS)
Reproject.apply(Polygon, CRS, CRS)
Reproject.apply(Polygon, Transform(CRS, CRS))  // A transform is a function that translates a Point
                                               // via some inner `Transform` object, by default
                                               // a `BasicCoordinateTransform` from Proj4.
Polygon.apply(Reproject.apply(Line, Transform), Array[Line])  // Line is reprojected.
Polygon.envelope  // from `Geometry` trait
Geometry.jtsGeom.getEnvelopeInternal
Extent.jts2Extent(jts.geom.Envelope)  // implicitly. This is the final `Extent`.
```

*So basically*

`Extent => ReprojectExtent => Polygon => Line => (projected) Line => Polygon => jts.geom.Envelope => Extent`


Layout Definitions
====================

**Data structures:** `LayoutDefinition`, `TileLayout`, `CellSize`

**Sources:** `geotrellis.spark.tiling.LayoutDefinition`

**What is a Layout Definition?**

A Layout Definition describes the location, dimensions of, and organization
of a tiled area of a map. Conceptually, the tiled area forms a grid, and the
Layout Definitions describes that grid's area and cell width/height.

Within the context of Geotrellis, the `LayoutDefinition` class extends
`GridExtent`, and exposes methods for querying the sizes of the grid and
grid cells. Those values are stored in the `TileLayout` (the grid
description) and `CellSize` classes respectively. `LayoutDefinition`s are
used heavily during the raster reprojection process.

In essence, a `LayoutExtent` is the minimum information required to
describe some tiled map area in Geotrellis.

**How are Layout Definitions used throughout Geotrellis?**

They are used heavily when reading, writing, and reprojecting Rasters.



