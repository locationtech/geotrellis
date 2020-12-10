---
id: getting_started
title: Getting Started 
sidebar_label: Getting Started 
slug: /
---

_GeoTrellis_ is a Scala library and framework that provides APIs for reading, writing and operating on geospatial raster and vector data. GeoTrellis also provides helpers for these same operations in Spark and for performing [MapAlgebra](https://en.wikipedia.org/wiki/Map_algebra) operations on rasters. It is released under the Apache 2 License.

## Installation

To get started with SBT, add the following to your `build.sbt`:
```scala
libraryDependencies += "org.locationtech.geotrellis" %% "geotrellis-raster" % "@VERSION@"
```

## An Example

Loading rasters via the `RasterSource` API is easy with the `geotrellis-raster` package:

```scala mdoc
import geotrellis.raster._

val rs = RasterSource("gtiff+file://raster/data/aspect.tif")
rs.extent
```
