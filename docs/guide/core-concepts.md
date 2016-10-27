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



