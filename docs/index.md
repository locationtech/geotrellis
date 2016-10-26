What is GeoTrellis?
-------------------

*GeoTrellis* is a Scala library and framework that uses
Spark to work with raster data.  It is released under
the Apache 2 License.

GeoTrellis reads, writes, and operates on raster data
as fast as possible. It implements many
[Map Algebra](http://en.wikipedia.org/wiki/Map_algebra)
operations as well as vector to raster or raster to
vector operations.

GeoTrellis also provides tools to render rasters into
PNGs or to store metadata about raster files as JSON.
It aims to provide raster processing at web speeds (sub-second
or less) with RESTful endpoints as well as provide
fast batch processing of large raster data sets.

Please visit the **[project site](http://geotrellis.io)**
for more information as well as some interactive demos.


Why GeoTrellis?
---------------

Make rasters great again! And vectors too, I guess.

Contact and Support
-------------------

You can find more information and talk to developers
(let us know what you're working on!) at:

- [Gitter](https://gitter.im/geotrellis/geotrellis)
- [GeoTrellis mailing list](https://groups.google.com/group/geotrellis-user)

Hello Raster!
-------------

Here's a small example showing a routine focal operation over a single
`Tile`:

```scala
scala> import geotrellis.raster._
import geotrellis.raster._

scala> import geotrellis.raster.op.focal._
import geotrellis.raster.op.focal._

scala> val nd = NODATA
nd: Int = -2147483648

scala> val input = Array[Int](
     |         nd, 7, 1, 1, 3, 5, 9, 8, 2,
     |         9, 1, 1, 2, 2, 2, 4, 3, 5,
     |
     |         3, 8, 1, 3, 3, 3, 1, 2, 2,
     |         2, 4, 7, 1, nd, 1, 8, 4, 3)
input: Array[Int] = Array(-2147483648, 7, 1, 1, 3, 5, 9, 8, 2, 9, 1, 1, 2,
2, 2, 4, 3, 5, 3, 8, 1, 3, 3, 3, 1, 2, 2, 2, 4, 7, 1, -2147483648, 1, 8, 4, 3)

scala> val iat = IntArrayTile(input, 9, 4)  // 9 and 4 here specify columns and rows
iat: geotrellis.raster.IntArrayTile = IntArrayTile([I@278434d0,9,4)

// The asciiDraw method is mostly useful when you're working with small tiles
// which can be taken in at a glance
scala> iat.asciiDraw()
res0: String =
"    ND     7     1     1     3     5     9     8     2
     9     1     1     2     2     2     4     3     5
     3     8     1     3     3     3     1     2     2
     2     4     7     1    ND     1     8     4     3
"

scala> val focalNeighborhood = Square(1)  // a 3x3 square neighborhood
focalNeighborhood: geotrellis.raster.op.focal.Square =
 O  O  O
 O  O  O
 O  O  O

scala> val meanTile = iat.focalMean(focalNeighborhood)
meanTile: geotrellis.raster.Tile = DoubleArrayTile([D@7e31c125,9,4)

scala> meanTile.getDouble(0, 0)  // Should equal (1 + 7 + 9) / 3
res1: Double = 5.666666666666667
```

Ready? [Setup a GeoTrellis development environment.](tutorials/setup.md)
