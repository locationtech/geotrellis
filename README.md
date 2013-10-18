# GeoTrellis

*GeoTrellis* is a high performance geoprocessing engine and programming toolkit.  The goal of the project is to transform
user interaction with geospatial data by bringing the power of geospatial analysis to real time, interactive web applications.

Please visit our **[documentation page](http://geotrellis.github.com)** for more information. 

GeoTrellis was designed to solve three core problems, with a focus on raster processing:

- Creating scalable, high performance geoprocessing web services
- Creating distributed geoprocessing services that can act on large data sets
- Parallelizing geoprocessing operations to take full advantage of multi-core architecture 

Please contact us if you have any questions, find us on irc at #geotrellis on freenode, or join 
the user mailing list at [https://groups.google.com/group/geotrellis-user](https://groups.google.com/group/geotrellis-user).

GeoTrellis is available under the Apache 2 license.  More information is also available on the [GeoTrellis website](http://www.azavea.com/products/geotrellis/).

GeoTrellis is available under the Apache 2 License.

The current release version of GeoTrellis is 0.8.2.

The current development release of GeoTrellis is 0.9.0-SNAPSHOT.
 
[![Build Status](https://api.travis-ci.org/geotrellis/geotrellis.png)](http://travis-ci.org/geotrellis/geotrellis)

## Features

- GeoTrellis is designed to help a developer create simple, standard REST services that return the results of geoprocessing models.
- Like an RDBS that can optimize queries, GeoTrellis will automatically parallelize and optimize your geoprocessing models where possible.  
- In the spirit of the object-functional style of Scala, it is easy to both create new operations and compose new 
operations with existing operations.

## SBT

    scalaVersion := "2.10.0"

    libraryDependencies += "com.azavea.geotrellis" %% "geotrellis" % "0.8.1"

## Some sample GeoTrellis code

```scala
  // Import some libraries and operations we'll use
  import geotrellis._
  import geotrellis.raster.op._

  // Set up the rasters and weights we'll use
  val raster1 = io.LoadRaster("foo")
  val raster2 = io.LoadRaster("bar")
  val weight1 = 5
  val weight2 = 2

  val total = weight1 + weight2

  // Create a new operation that multiplies each cell of
  // each raster by a weight, and then add those two new
  // rasters together.
  val op = local.Add(local.MultiplyConstant(raster1, weight1),
                     local.MultiplyConstant(raster2, weight2))

  // Create a new operation that takes the result of the
  // previous operation and divides each cell by the total
  // weight, creating a weighted overlay of our two rasters.
  val wo1 = local.DivideConstant(op, total)

  // We can use a simpler syntax if we want.  Note that this
  // is still just creating an operation.
  import geotrellis.Implicits._
  val wo2 = (raster1 * weight1 + raster2 * weight2) / total

  // To this point, we've only been composing new operations.
  // Now, we will run them.
  import geotrellis.process.Server
  val server = Server("example")
  val result1 = server.run(wo1)
  val result2 = server.run(wo2)
```

## API Reference

### Scaladocs

You can find *Scaladocs* for the latest version of the project here:

[http://geotrellis.github.com/scaladocs/latest/#geotrellis.package](http://geotrellis.github.com/scaladocs/latest/#geotrellis.package)

## Contributors

 - Josh Marcus
 - Erik Osheim
 - Rob Emanuele 
 - Adam Hinz
 - Michael Tedeschi
 - Robert Cheetham
 - Justin Walgran
 - Eric J. Christeson

