GeoTrellis relies on `geotrellis.proj4` for projecting coordinates - that
is, finding 2D arrangements of positions from the real, 3D world.  It
provides implementations of [many projection formulae](https://xkcd.com/977/),
and supports further customization such as selecting different units of
measure or models of the earth's curvature.  A database of well-known
coordinate systems provides commonly used settings, so typically users do
not need to be familiar with all of the possible parameters.

Ultimately, there are two types that are important to users of the
`geotrellis.proj4` module: `CRS` represents a Coordinate Reference System,
and ``Transform`` represents a function for transforming coordinates from
one CRS to another.

To create a CRS instance, there are a few options:

```scala
import geotrellis.proj4._
LatLng // latitude/longitude coordinate system
WebMercator // "Web mercator" coordinate system, the one used by many
// commercial tile providers including Google Maps, Bing Maps, MapQuest, etc.

CRS.fromEpsgCode(2272) // Data is generally distributed with an advertised
// "EPSG Code" corresponding to an entry in a well-known database of coordinate
// system parameters. Use fromEpsgCode to load coordinate system information
// from this database.

CRS.fromString("+proj=utm +zone=18 +datum=WGS84 +units=m +no_defs")
// Alternatively, coordinate system parameters may be specified in a string
// conforming to the proj.4 parameter format.  Data sets may document these
// parameters instead of identifying an EPSG code, and these strings can also
// be used to operate against data using coordinate systems not documented in
// the EPSG database.
```

Individual CRS values do not offer many operations, but two CRSs can be used
together to create a Transform capable of transforming coordinate pairs from
the source CRS to the target CRS.  For example, to transform coordinates
from the latitude/longitude coordinate system to the web mercator coordinate
system in a Scala REPL the session might look like this:

```scala
scala> import geotrellis.proj4._
import geotrellis.proj4._

scala> val latLongToWebMercator = Transform(LatLng, WebMercator)
latLongToWebMercator: (Double, Double) => (Double, Double) = <function2>

scala> latLongToWebMercator(39.961286, -75.154233)
res0: (Double, Double) = (4448470.008964373,-1.2998915115941694E7)

```

This is all you really need to know to use `geotrellis.proj4`, and generally
you won't even need to use `Transform` objects directly - for the most part,
GeoTrellis APIs expect source and target `CRS`'s and manage the `Transform`
instances internally.  If you do find yourself working with `Transform`s,
you should keep in mind that creating them takes some computation, so it is
a good idea to reuse them where possible.
