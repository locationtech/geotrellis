## Reading GeoTiffs


### Introduction
This tutorial will go over how to read GeoTiff files using GeoTrellis. It assumes that you already have the environment needed to run these examples. If not, please follow this [link] to get GeoTrellis working on your system.
- - -

### Reading GeoTiffs with GeoTrellis
One of the most common methods of storing geo-spatial information is through GeoTiffs. This is reflected throughout the GeoTrellis library where many of its features can work with GeoTiffs. Which would mean that there would have to be many different ways to read in GeoTiff, and indeed there are! In the following document, we will go over these methods as well as some more advanced features when loading in GeoTiff into GeoTrellis.

Before we start, open a Scal REPL in the Geotrellis dirctory.

#### Reading Locally
The first way we will read in a GeoTiff is also the easiest, just a simple local read. You can see how to do this below.

```scala
  import geotrellis.raster.io.geotiff.reader.GeoTiffReader

  val path: String = "raster-test/data/geotiff-test-files/lzw_int32.tif"
  val geoTiff: SinglebandGeoTiff = GeoTiffReader.readSingleband(path)
```

And that's it! Not too bad at all really, just four lines of code. Even still, though, let's break this down line-by-line so we can see what exactly is going on.

```scala
  import geotrellis.raster.io.geotiff.reader.GeoTiffReader
```
This import statement brings in `GeoTiffReader` from `geotrellis.raster.io.geotiff.reader` so that we can use it in the REPL. As the name implys, `GeoTiffReader` is the object that actually reads the GeoTiff. If you ever wonder about how we analyze and process GeoTiffs, then `geotrellis.raster.io.geotiff` would be the place to work.

Okay, so we got the thing that will give us our GeoTiff, now we just need to give it something. This is where the next line of code comes into play.

```scala
  val path: String = "raster-test/data/geotiff-test-files/lzw_int32.tif"
```
Our `path` variable is a string that contains the file path to a GeoTiff in `geotrellis.raster-test`. This is what our `GeoTiffReader` will read from. Do not think that the `GeoTiffReader` can only take `String`s, though. There are two other data types it can take: these are `Array[Byte]`s and `ByteReader`s. We will stick with `String`s for this lesson, but `Array[Byte]` is not that much different. It's just all of the bytes within your file held in an Array. To learn more about `ByteReader`, follow this [link].

The last part of four line coding escapade is:

```scala
  val geoTiff: SinglebandGeoTiff = GeoTiffReader.readSingleband(path)
```
This line assigns the variable, `geoTiff` to our read in GeoTiff file. Notice the `geoTiff`'s, though. It is `SinglebandGeoTiff`. Why does `geoTiff` have this type? It's because in GeoTrellis, `SinglebandGeoTiff`s and `MutlibandGeoTiff`s are two seperate subtypes of `GeoTiff`. Do not get too hung up on these two types, as the actual methods you will use on them can take either. However, where this does matter is when actaully loading in the files.

Let's say that we have a `MultibandGeoTiff`, but we use the code from above to read it in.

```scala
  import geotrellis.raster.io.geotiff.reader.GeoTiffReader

  // now a MultibandGeoTiff!!!
  val path: String = "raster-test/data/geotiff-test-files/3bands/3bands-striped-band.tif"
  val geoTiff: SinglebandGeoTiff = GeoTiffReader.readSingleband(path)
```
If we run this code, what do you think will happen? The result may surprise you, we get back a `SinglebandGeoTiff`! When told to read a `SinglebandGeoTiff` from a `MultibandGeoTiff`, the `GeoTiffReader` will just read in the first band of the file and return that. Thus, it is important to keep in mind what kind of GeoTiff you are working with, or else you could get some incorrect results.

To remedy this issue, we just have to change the method call and return type so that `GeoTiffReader` will read in all of the bands of our GeoTiff.

```scala

  val geoTiff: MultibandGeoTiff = GeoTiffReader.readMultiband(path)
```

And that's it! We now have our `MutlibandGeoTiff`.
