# GeoTrellis

[![CI](https://github.com/locationtech/geotrellis/workflows/CI/badge.svg)](https://github.com/locationtech/geotrellis/actions) [![Join the chat at https://gitter.im/geotrellis/geotrellis](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/geotrellis/geotrellis?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![ReadTheDocs](https://readthedocs.org/projects/geotrellis/badge/?version=latest)](http://geotrellis.readthedocs.io/en/latest/)
[![Changelog](https://img.shields.io/badge/changelog-v1.2.0-brightgreen.svg)](https://github.com/locationtech/geotrellis/blob/master/CHANGELOG.md)
[![Contributing](https://img.shields.io/badge/contributing-see%20conditions-brightgreen.svg)](https://github.com/locationtech/geotrellis/blob/master/docs/CONTRIBUTING.rst)

[![Maven Central](https://img.shields.io/maven-central/v/org.locationtech.geotrellis/geotrellis-spark_2.12)](http://search.maven.org/#search%7Cga%7C1%7Corg.locationtech.geotrellis) [![Snapshots](https://img.shields.io/badge/snapshots-available-orange)](https://central.sonatype.com/service/rest/repository/browse/maven-snapshots/org/locationtech/geotrellis/geotrellis-spark_2.13/)

_GeoTrellis_ is a Scala library and framework that provides
APIs for reading, writing and operating on geospatial
raster and vector data. GeoTrellis also provides helpers
for these same operations in Spark and for performing
[MapAlgebra](https://en.wikipedia.org/wiki/Map_algebra)
operations on rasters. It is released under the Apache 2 License.

Please visit the **[project site](http://geotrellis.io)**
for more information as well as some interactive demos.

You're also welcome to ask questions and talk to developers
(let us know what you're working on!) via [Gitter](https://gitter.im/geotrellis/geotrellis).

## Getting Started

GeoTrellis is currently available for Scala 2.12 and 2.13, using Spark 3.3.x.

To get started with SBT, simply add the following to your build.sbt file:

```scala
libraryDependencies += "org.locationtech.geotrellis" %% "geotrellis-raster" % "<latest version>"
```

To grab the latest `SNAPSHOT`, `RC` or milestone build, add these resolvers:

```scala
// maven central snapshots
resolvers ++= Seq(
  "central-snapshots" at "https://central.sonatype.com/repository/maven-snapshots/"
)

// or eclipse snapshots
resolvers ++= Seq(
  "eclipse-releases" at "https://repo.eclipse.org/content/groups/releases",
  "eclipse-snapshots" at "https://repo.eclipse.org/content/groups/snapshots"
)
```

If you are just getting started with GeoTrellis, we recommend familiarizing yourself with the
`geotrellis-raster` package, but it is just one of the many available. The complete list
of published GeoTrellis packages includes:

- `geotrellis-accumulo`: Accumulo store integration for GeoTrellis
- `geotrellis-accumulo-spark`: Accumulo store integration for GeoTrellis + Spark
- `geotrellis-cassandra`: Cassandra store integration for GeoTrellis
- `geotrellis-cassandra-spark`: Cassandra store integration for GeoTrellis + Spark
- `geotrellis-gdal`: GDAL bindings for GeoTrellis
- `geotrellis-geotools`: Conversions to and from GeoTools Vector and Raster data
- `geotrellis-hbase`: HBase store integration for GeoTrellis
- `geotrellis-hbase-spark`: HBase store integration for GeoTrellis + Spark
- `geotrellis-layer`: Datatypes to describe sets of rasters
- `geotrellis-macros`: Performance optimizations for GeoTrellis operations
- `geotrellis-proj4`: Coordinate Reference systems and reproject (Scala wrapper around Proj4j)
- `geotrellis-raster`: Raster data types and operations, including MapAlgebra
- `geotrellis-raster-testkit`: Testkit for testing `geotrellis-raster` types
- `geotrellis-s3`: Amazon S3 store integration for GeoTrellis
- `geotrellis-s3-spark`: Amazon S3 store integration for GeoTrellis + Spark
- `geotrellis-shapefile`: Read ESRI Shapefiles into GeoTrellis data types via GeoTools
- `geotrellis-spark`: Geospatially enables Spark and provides primitives for external data stores
- `geotrellis-spark-pipeline`: DSL for geospatial ingest jobs using GeoTrellis + Spark
- `geotrellis-spark-testkit`: Testkit for testing `geotrellis-spark` code
- `geotrellis-store`: Abstract interfaces for storage services, with concrete implementations for local and Hadoop filesystems
- `geotrellis-util`: Miscellaneous GeoTrellis helpers
- `geotrellis-vector`: Vector data types and operations extending JTS
- `geotrellis-vector-testkit`: Testkit for testing `geotrellis-vector` types
- `geotrellis-vectortile`: Experimental vector tile support, including reading and writing

A more complete feature list can be found on the [Module Hierarchy page](https://geotrellis.readthedocs.io/en/latest/guide/module-hierarchy.html) of the GeoTrellis documentation. If you're looking for a specific feature or
operation, we suggest searching there or reaching out on Gitter.

For older releases, check the complete list of packages and versions
available at [locationtech-releases](https://repo.locationtech.org/#view-repositories;geotrellis-releases~browsestorage).

## Hello Raster

```scala
scala> import geotrellis.raster._
import geotrellis.raster._

scala> import geotrellis.raster.render.ascii._
import geotrellis.raster.render.ascii._

scala> import geotrellis.raster.mapalgebra.focal._
import geotrellis.raster.mapalgebra.focal._

scala> val nd = NODATA
nd: Int = -2147483648

scala> val input = Array[Int](
     nd, 7, 1, 1,  3, 5, 9, 8, 2,
      9, 1, 1, 2,  2, 2, 4, 3, 5,
      3, 8, 1, 3,  3, 3, 1, 2, 2,
      2, 4, 7, 1, nd, 1, 8, 4, 3)
input: Array[Int] = Array(-2147483648, 7, 1, 1, 3, 5, 9, 8, 2, 9, 1, 1, 2,
2, 2, 4, 3, 5, 3, 8, 1, 3, 3, 3, 1, 2, 2, 2, 4, 7, 1, -2147483648, 1, 8, 4, 3)

scala> val iat = IntArrayTile(input, 9, 4)  // 9 and 4 here specify columns and rows
iat: geotrellis.raster.IntArrayTile = IntArrayTile([I@278434d0,9,4)

// The renderAscii method is mostly useful when you're working with small tiles
// which can be taken in at a glance.
scala> iat.renderAscii(AsciiArtEncoder.Palette.STIPLED)
res0: String =
∘█  ▚▜██▖
█  ▖▖▖▜▚▜
▚█ ▚▚▚ ▖▖
▖▜█ ∘ █▜▚

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

## Documentation

Documentation is available at [geotrellis.io/documentation](https://geotrellis.io/documentation).

_Scaladocs_ for the the `master` branch are
[available here](https://geotrellis.github.io/scaladocs/latest/geotrellis/index.html?search=geotrellis).

Further examples and documentation of GeoTrellis use-cases can be found in the [docs/](./docs) folder.

## Contributing

Feedback and contributions to the project, no matter what kind, are always
very welcome. A CLA is required for contribution, see
[Contributing](http://geotrellis.readthedocs.io/en/latest/CONTRIBUTING.html)
for more information. Please refer to the [Scala style
guide](http://docs.scala-lang.org/style/) for formatting patches to the
codebase.

### Where is our commit history and contributor list prior to Nov 2016?

**The entire old history is available in the `_old/master` branch.**

#### Why?

In November 2016, GeoTrellis moved it's repository from the
[GeoTrellis GitHub Organization](https://github.com/geotrellis) to it's current
home in the LocationTech GitHub organization.
In the process of moving our repository, we went through an IP review process.
Because the Eclipse foundation only reviews a snapshot of the repository, and
not all of history, we had to start from a clean `master` branch.

Unfortunately, we lost our commit and contributor count in the move.
These are significant statistics for a repository,
and our current counts make us look younger than we are.
GeoTrellis has been an open source project since 2011.
This is what our contributor and commit count looked like
before the move to LocationTech:

![Commit and contributor count before LocationTech move](docs/img/contributor-and-commit-count-pre-locationtech.png)

Along with counts, we want to make sure that all the awesome people
who contributed to GeoTrellis before the LocationTech move can
still be credited on a contributors page. For posterity, I will
leave the following contributors page to what it was before the move:

https://github.com/lossyrob/geotrellis-before-locationtech/graphs/contributors

#### Tie Local History to Old History

You can also tie your local clone's master history to the old history by running

```console
> git fetch origin refs/replace/*:refs/replace/*
```

if `origin` points to https://github.com/locationtech/geotrellis.
This will allow you to see the old history for commands like `git log`.
