## S3 GeoTiff Streaming

- [Introduction](#introduction)
- [How to Use thise Feature](#how-to-use-this-feature)
  - [Creating BytesStreamer](#creating-streambytes)
  - [Creating a ByteReader](#creating-a-bytereader)
  - [Streaming a GeoTiff From S3](#streaming-a-geotiff-from-s3)
    - [Streaming the Entire GeoTiff](#streaming-the-entire-geotiff)
    - [Streaming a Section of a GeoTiff](#streaming-a-section-of-a-geotiff)
- [Conclusion](#conclusion)

- - -
### Introduction

A common problem that faces those who perform geo-spatial analysis using Amazon's S3 is dealing with importing and working with large GeoTiffs on the S3 server. A solution to this issue has been in development for the past few weeks that will allow for stream reading of GeoTiffs directly off S3 regardless of the file's size. This short guide goes over how to use this feature as of 10/12/2016.

**Note** This feature is still be worked on and is subject to change at any time. I will update this document when any changes have been made to this feature as soon as I can. To check the latest version, see the link to the pull request at the bottom of this document.
- - -
### How to Use this Feature

In order for streaming from S3 to work, two new types have been created. These are: `BytesStreamer` and `ByteReader`, respectively. the first deals with getting bytes in the form of `Map[Long, Array[Byte]` called a, "chunk". The `Long` of this chunk is the position in the file where the `Array[Byte]` starts. For example, if a new chunk needs to be created at the 100th byte of the file then the chunk that is created will have a value of 100. These chunks can vary in size and a new one is produced each time a call is made to another part of the file which is not contained within the current chunk.

`ByteReader` is what reads the bytes from the chunks. As of right now, only `java.nio.ByteBuffer`s and the `StreamByteReader` type can access the chunk.

#### Creating `BytesStreamer`
There is only one way to construct an instance of `BytesStreamer` as of this writing, and that is through the `S3ByteStreamer` class. Found in `geotrellis.spark.io.s3.util` package, this class will be able to get chunks from a desired GeoTifff on S3. Below is a basic guide on creating this class:

```scala
 import geotrellis.spark.io.s3._
 import geotrellis.spark.io.s3.util.S3BytesStreamer
 
 val client: S3Client = S3Client.default
 val bucket: String = "my-bucket"
 val key: String = "path/to/my/geotiff.tif"
 
 // how many bytes that should be streamed at a time
 val chunkSize: Int = 256000
 
 val s3Bytes = S3BytesStreamer(bucket, key, client, chunkSize)
```

#### Creating a `ByteReader`
A `java.nio.ByteBuffer` can be used as a `ByteReader`, but because this focus is on streaming GeoTiffs off S3, only `StreamByteReader` will be looked at in this guide. Below is a basic guide on creating this class:

```scala
 import geotrellis.util.StreamByteReader
 
 val reader = StreamByteReader(s3Bytes)
```

#### Streaming a GeoTiff From S3
Now that we have a `ByteReader` all that needs to be done now is to supply it to the `GeoTiffReader` with any additional reading parameters that might be needed.

#### Streaming the Entire GeoTiff
Because GeoTiffs can be read in lazily now, files of any size should be able to be read in fully either from S3 or locally. However, precautions should be taken when doing so. It appears that when a file's size passes a certain point, the time it takes to perform operations on it degrades rapidly. This aspect of whole file streaming has not yet been looked into closely, so it is uncertain at what point performance begins to fall and to what extent. Therefore, it is best to use caution when streaming the entire file.

```scala
 import geotrellis.raster.io.geotiff._
 import geotrellis.raster.io.geotiff.reader._
 
 // streaming the entire GeoTiff
 
 // for Singlebands
 GeoTiffReader.readSingleband(reader, false, true)
 // or
 SinglebandGeoTiff(reader, false, true)
 // or
 SinglebandGeoTiff.streaming(reader)
 
 // for Multibands
 GeoTiffReader.readMultiband(reader, false, true)
 // or
 MultibandGeoTiff(reader, false, true)
 // or
 MultibandGeoTiff.streaming(reader)
```

#### Streaming a Section of a GeoTiff
A better alternative to streaming the whole GeoTiff from S3 is to do a windowed streaming of it. This will only take a section of the GeoTiff and not the entire thing. This requires knowing the `Extent` of the GeoTiff already, and giving the `GeoTiffReader` a suitable, new `Extent`.

```scala
 import geotrellis.vector.Extent
 import geotrellis.raster.io.geotiff._
 import geotrellis.raster.io.geotiff.reader._
 
 // windowed streaming of GeoTiffs
 
 // extent of the windowed region
 val e: Extent = Extent(0, 1, 2, 3)
 
 // for Singlebands
 GeoTiffReader.readSingleband(reader, e)
 // or
 SinglebandGeoTiff.streaming(reader, e)

 // for Multibands
 GeoTiffReader.readMultiband(reader, e)
 // or
 MultibandGeoTiff.streaming(reader, e)
 
 // the extent can also be supplied as an Option[Extent]
 
 // for Singlebands
 GeoTiffReader.readSingleband(reader, Some(e))
 // or
 SinglebandGeoTiff.streaming(reader, Some(e))

 // for Multibands
 GeoTiffReader.readMultiband(reader, Some(e))
 // or
 MultibandGeoTiff.streaming(reader, Some(e))
```
- - -

### Conclusion
The core functionality of this feature is now believed to work, but there still needs to be improvements done to both the API and the streaming process itself. Any feedback at all about this new feature would be greatly appreciated. Please feel free to contact the author, [@jbouffard](https://github.com/jbouffard), on either Geotrellis' [Gitter room](https://gitter.im/geotrellis/geotrellis#) or [mailing list](https://groups.google.com/forum/#!forum/geotrellis-user). In addition, you can feel free to leave a comment on this feature's [pull request](https://github.com/geotrellis/geotrellis/pull/1617).
