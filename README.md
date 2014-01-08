# GeoTrellis

*GeoTrellis* is a Scala library and framework for creating processes to work with raster data.

#### IO: 
GeoTrellis reads, writes, and operates on raster data as fast as possible. It also has functionality to warp (change the resolution and bounding box of) rasters on loading and throughout the operation sequence.
      
#### Operations: 
GeoTrellis implements many [Map Algebra](http://en.wikipedia.org/wiki/Map_algebra) operations as well as vector to raster or raster to vector operations. This is the heart of GeoTrellis: preforming geospatial operations against raster data in the fastest way possible, no matter the scale.
      
##### Web Service Framework:
GeoTrellis provides tools to render rasters into PNGs for web mapping applications, or to convert information about the rasters into JSON format. One of the main goals of GeoTrellis is to provide raster processing at web speeds (sub-10 ms) for RESTful endpoints that can be consumed by web applications. Another goal is to provide fast batch processing of very large raster data.

Please visit our **[documentation page](http://geotrellis.github.com)** for more information.

You can also find more information at:

  - IRC:  #geotrellis on freenode. Come say HI, we would love to hear about what you're working on. 
  - The mailing list at [https://groups.google.com/group/geotrellis-user](https://groups.google.com/group/geotrellis-user).

GeoTrellis is available under the Apache 2 license.  

More information is also available on the [GeoTrellis website](http://www.azavea.com/products/geotrellis/).
 
[![Build Status](https://api.travis-ci.org/geotrellis/geotrellis.png)](http://travis-ci.org/geotrellis/geotrellis)

## SBT

    scalaVersion := "2.10.3"

    libraryDependencies += "com.azavea.geotrellis" %% "geotrellis" % "0.9.0-RC1"

## Some sample GeoTrellis code

```scala
  // Import some libraries and operations we'll use
  import geotrellis._
  import geotrellis.source._

  // Set up the rasters and weights we'll use:
  val r1 = RasterSource("rasterOne") // raster defined in catalog
  val r2 = RasterSource("rasterTwo")

  // This will define a RasterSource that is dthe addition of the 
  // first raster with each cell multiplied
  // by 5, and the second raster with each cell multiplied by 2. 
  // To understand what it means for a raster to be multiplied by an integer or
  // for two rasters to be added, see Map Algebra documentation,
  // GeoTrellis documentation or ask the mailing list/IRC.
  val added  = (r1*5) + (r2*2)
  

  // This divides the raster by the total weight,
  // which will give us the the final Weighted Overlay
  // (also called a Suitability Map).
  val weightedOverlay = added / (5+2)

  // Now we want to render this Weighted Overlay
  // as part of a WMS service. We could run the
  // renderPng operation with the default color ramp
  // (which is a Blue to Red color ramp descriped
  // at http://geotrellis.github.io/overviews/rendering.html).
  val rendered = added.renderPng  
 
  // At this point we've only describe the work that
  // we wish to be done. Once we call 'run' on the souce,
  // we will invoke the chain of commands that we have been
  // building up, from loading the rasters from disk to 
  // rendering the PNG of the wieghted overlay.

  rendered.run match {
    process.Complete(png, history) =>
      // return the PNG as an Array of Bytes
      // for spray:
      respondWithMediaType(MediaTypes.`image/png`) {
        complete { png }
      }
    process.Failure(message, history) =>
      // handle the failure.
      // for spray:
      failWith { new RuntimeException(message) }
  }
 ```

## API Reference

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
 - Ameet Kini
 - Mark Landry
 - Walt Chen

## Contributing

Feedback and contributions to the project, no matter what kind, are always very welcome. A CLA is required for contribution, see the [CLA FAQ](https://github.com/geotrellis/geotrellis/wiki/Contributor-license-agreement-FAQ) on the wiki for more information. Please refer to the [Scala style guide](http://docs.scala-lang.org/style/) for formatting patches to the codebase.
