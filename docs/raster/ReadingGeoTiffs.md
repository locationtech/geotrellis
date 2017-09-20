## Reading GeoTiffs

- [Introduction](#introduction)
- [Reading GeoTiffs With GeoTrellis](#reading-geotiffs-with-geotrellis)
  - [Reading Locally Part 1: Reading For the First Time](#reading-locally-part-1-reading-for-the-first-time)
  - [Reading Locally Part 2: Expanding Our Vocab](reading-locally-part-2-expanding-our-vocab)

### Introduction
This tutorial will go over how to read GeoTiff files using GeoTrellis. It assumes that you already have the environment needed to run these examples. If not, please follow this [link] to get GeoTrellis working on your system.
- - -

### Reading GeoTiffs With GeoTrellis
One of the most common methods of storing geo-spatial information is through GeoTiffs. This is reflected throughout the GeoTrellis library where many of its features can work with GeoTiffs. Which would mean that there would have to be many different ways to read in GeoTiff, and indeed there are! In the following document, we will go over these methods as well as some more advanced features when loading in GeoTiff into GeoTrellis.

Before we start, open a Scal REPL in the Geotrellis dirctory.

#### Reading Locally Part 1: Reading For the First Time
The first way we will read in a GeoTiff is also the easiest, just a simple local read. You can see how to do this below.
```scala
  import geotrellis.raster.io.geotiff.reader.GeoTiffReader

  val path: String = "raster/data/geotiff-test-files/lzw_int32.tif"
  val geoTiff: SinglebandGeoTiff = GeoTiffReader.readSingleband(path)
```
And that's it! Not too bad at all really, just three lines of code. Even still, though, let's break this down line-by-line so we can see what exactly is going on.
```scala
  import geotrellis.raster.io.geotiff.reader.GeoTiffReader
```
This import statement brings in `GeoTiffReader` from `geotrellis.raster.io.geotiff.reader` so that we can use it in the REPL. As the name implys, `GeoTiffReader` is the object that actually reads the GeoTiff. If you ever wonder about how we analyze and process GeoTiffs, then `geotrellis.raster.io.geotiff` would be the place to look.

Okay, so we brought in the object that will give us our GeoTiff, now we just need to supply it what to read. This is where the next line of code comes into play.
```scala
  val path: String = "raster/data/geotiff-test-files/lzw_int32.tif"
```
Our `path` variable is a `String` that contains the file path to a GeoTiff in `geotrellis.raster`. `GeoTiffReader` will use this value then to read in our GeoTiff. There are more types `GeoTiffReader` can accept, however. There are two other data types it can take: these are `Array[Byte]`s and `ByteReader`s. We will stick with `String`s for this lesson, but `Array[Byte]` is not that much different. It's just all of the bytes within your file held in an Array. To learn more about `ByteReader`, follow this [link].

The last part of three line, coding escapade is:
```scala
  val geoTiff: SinglebandGeoTiff = GeoTiffReader.readSingleband(path)
```
This line assigns the variable, `geoTiff` to our read in GeoTiff file. Notice the `geoTiff`'s type, though. It is `SinglebandGeoTiff`. Why does `geoTiff` have this type? It's because in GeoTrellis, `SinglebandGeoTiff`s and `MutlibandGeoTiff`s are two seperate subtypes of `GeoTiff`.

Great! We have a `SinglebandGeoTiff`. Let's say that we have a `MultibandGeoTiff`, though; and we use the code from above to read it in with just one slight difference.
```scala
  import geotrellis.raster.io.geotiff.reader.GeoTiffReader

  // now a MultibandGeoTiff!!!
  val path: String = "raster/data/geotiff-test-files/3bands/3bands-striped-band.tif"
  val geoTiff = GeoTiffReader.readSingleband(path)
```
If we run this code, what do you think will happen? The result may surprise you, we get back a `SinglebandGeoTiff`! **When told to read a `SinglebandGeoTiff` from a `MultibandGeoTiff` without a return type, the `GeoTiffReader` will just read in the first band of the file and return that**. Thus, it is important to keep in mind what kind of GeoTiff you are working with, or else you could get back an incorrect result.

To remedy this issue, we just have to change the method call and return type so that `GeoTiffReader` will read in all of the bands of our GeoTiff.
```scala
  val geoTiff: MultibandGeoTiff = GeoTiffReader.readMultiband(path)
```
And that's it! We now have our `MutlibandGeoTiff`.

> ##### Beginner Tip
> A good way to ensure that your codes works properly is to give the return data type for each of your `val`s and `def`s. If by chance your return type and is different from what is actually returned, the compiler will through an error. In addition, this will also make your code easier to read and understand for both you and others as well.
>
> Example:
>
> ```scala
>   val multiPaht = "path/to/a/multiband/geotiff.tif
>   
>   // This will give you the wrong result!!!
>   val geoTiff = GeoTiffReader.readsingleband(multiPath)
>
>   // This will cause your compiler to throw an error
>   val geoTiff: MultibandGeoTiff = GeoTiffReader.readsingleband(multiPath)
>```

Before we move on to the next section, I'd like to take moment and talk about an alternative way in which you can read in GeoTiffs. Both `SinglebandGeoTiff`s and `MultibandGeoTiff`s have their own `apply` methods, this means that you can give your paramter(s) directly to their companion objects and you'll get back a new instance of the class.

For `SinglebandGeoTiff`s:
```scala
  import geotrellis.raster.io.geotiff.SinglebandGeoTiff

  val path: String = "raster/data/geotiff-test-files/lzw_int32.tif"
  val geoTiff: SinglebandGeoTiff = SignlebandGeoTiff(path)
```
There are two differences found within this code from the previous example. The first is this:
```scala
  import geotrellis.raster.io.geotiff.SinglebandGeoTiff
```
`SinglebandGeoTiff` and `MultibandGeoTiff` are found within a different folder of `geotrellis.raster.io.geotiff`. This is important ot keep in mind when importing, as it can cause your code not to compile if you refer to the wrong subfolder.

The second line that was changed is:
```scala
  val geoTiff: SinglebandGeoTiff = SinglebandGeoTiff(path)
```
Here, we see `SinglebandGeoTiff`'s `apply` method being used on `path`. Which returns the same thing as `GeoTiffReader.readSingleband(path)`, but with less verbosity.

`MultibandGeoTiff`s are the exact same as their singleband counterparts.
```Scala
  import geotrellis.raster.io.geotiff.MultibandGeoTiff

  val path: String = "raster/data/geotiff-test-files/3bands/3bands-striped-band.tif"
  val geoTiff: MultibandGeoTiff = MultibandGeoTiff(path)
```

> ##### Beginner's Tip
> Notice, how we're using an `apply` method but not actually calling the method itself? That's because `apply` is special in Scala in that you're *applying* these paramters to an object. You'll find these methods found in companion object s most of the time, and they provide ways of creating new instances of classes and traits.

Our overview of basic GeoTiff reading is now done! But keep reading! For you have greater say over how your GeoTiff will be read than what has been shown.
