## WindowedGeoTiffReader

- [Introduction](#introduction)
- [Tips for Using This Feature](#tips-for-using-this-feature)
   - [Reading in Small Files](#reading-in-small-files)
   - [Reading in Large Files](#reading-in-large-files)
- [How to Use This Feature](#how-to-use-this-feature)
   - [Method 1](#method-1)
   - [Method 2](#method-2)
- [Demo](#demo)

- - -
### Introduction
Geotrellis is a Scala library which allows for geospatial analysis on both local and distributed networks. It can read a
large number of different types of spatial data files. One such file type are GeoTiffs. While Geotrellis can perform
powerful analytical operations on these types of files, they are unable to read in GeoTiffs past a certain size (without some additional work). This poses a problem as many files of interest are large, and thus, require extra proscessing in order to analyze them. To solve this issue, the Winowed GeoTiff Reader was created.
- - -
### Tips For Using This Feature
It is important to go over the strengths and weaknesses of this feature before use. If implemented well, the WindowedGeoTiff Reader can save you a large amount of time. However, it can also lead to further problems if it is not used how it was intended.

It should first be stated that this reader was made to read in ***sections*** of a Geotiff. Therefore, reading in either the entire, or close to the whole file will either be comparable or slower than reading in the entire file at once and then
cropping it. In addtion, crashes may occur depending on the size of the file.

#### Reading in Small Files
Smaller files are GeoTiffs that can be read in full with the normal reader without crashing the process. The way to best
utilize the reader for these kinds of files differs from larger ones.

To gain optimum performance, the principle to follow is: **the smaller the area selected, the faster the reading will
be**. What the exact performance increase will be depends on the bandtype of the file. The general pattern is that the larger the datatype is, quicker it will be at reading. Thus, a Float64 GeoTiff will be loaded at a faster rate than a UByte
GeoTiff. There is one caveat to this rule, though. Bit bandtype is the smallest of all the bandtypes, yet it can be read in
at speed that is similar to Float32.

For these files, 90% is the cut off for all band and storage types. Anything more may cause performance declines.

#### Reading in Large Files
Whereas samll files could be read in full using the reader, larger files cannot as they will crash whtever process you're
running. The rules for these sorts of files are a bit more complicated than that of their smaller counterparts, but learning them will allow for much greater performance in your analysis.

One similarity that both large and small files share is that they have the same principle: **the smaller the area selected,
the faster the reading will be**. However, while smaller files may experience slowdown if the selected area is too large,
these bigger files will crash. Therefore, this principle must be applied more strictly than with the previous file sizes.

In large files, the pattern of performance increase is the reverse of the smaller files. Byte bandtype can not only read faster, but are able to read in larger areas than bigger bandtypes. Indeed, the area which you can select is limited to what the bandtype of the GeoTiff is. Hence, an additonal principle applies for these large files: **the smaller the bandtype, the larger of an area you can select**. The exact size for each bandtype is not known, estimates have been given in the table bellow that should provide some indiication as to what size to select.

| BandType | Area Threshold Range In Cells |
|:--------:|:----------------------------------------------------:|
|   Byte   |                [5.76 * 10<sup>9</sup>,  6.76 * 10<sup>9</sup>)                |
|   Int16  |                [3.24 * 10<sup>9</sup>,  2.56 * 10<sup>9</sup>)                |
|   Int32  |                [1.44 * 10<sup>9</sup>,  1.96 * 10<sup>9</sup>)                |
|  UInt16  |                [1.96 * 10<sup>9</sup>,  2.56 * 10<sup>9</sup>)                |
|  UInt32  |                [1.44 * 10<sup>9</sup>,  1.96 * 10<sup>9</sup>)                |
|  Float32 |                [1.44 * 10<sup>9</sup>,  1.96 * 10<sup>9</sup>)                |
|  Float64 |                 [3.6 * 10<sup>8</sup>,  6.4 * 10<sup>8</sup>)                 |
- - -
### How to Use This Feature
Using this feature is straight forward and easy. There are two ways to implement the WindowedReader: Supplying the desired extent with the path to the file, and cropping an already existing file that is read in through a stream.

#### Method 1
Supplyig an extent with the file's path and having it being read in windowed can be done in the following ways:

```scala
val path: String = "path/to/my/geotiff.tif"
val e: Extent = Extent(0, 1, 2, 3)

// supplying the extent as an Extent

// if the file is singleband
SinglebandGeoTiff(path, e)
// or
GeoTiffReader.readSingleband(path, e)

// if the file is multiband
MultibandGeoTiff(path, e)
// or
GeoTiffReader.readMultiband(path, e)

// supplyig the extent as an Option[Extent]

// if the file is singleband
SinglebandGeoTiff(path, Some(e))
// or
GeoTiffReader.readSingleband(path, Some(e))

// if the file is multiband
MultibandGeoTiff(path, Some(e))
// or
GeoTiffReader.readMultiband(path, Some(e))
```

#### Method 2
Cropping an already loaded GeoTiff that was read in through Streaming. By using this method, the actual file isn't loaded into memory, but its data can still be accessed. Here's how to do the cropping:
```scala
val path: String = "path/to/my/geotiff.tif"
val e: Extent = Extent(0, 1, 2, 3)

// doing the reading and cropping in one line

// if the file is singleband
SinglebandGeoTiff.streaming(path).crop(e)
// or
GeoTiffReader.readSingleband(path, false, true).crop(e)

// if the file is multiband
MultibandGeoTiff.streaming(path).crop(e)
// or
GeoTiffReader.readMultiband(path, false, true).crop(e)

// doing the reading and cropping in two lines

// if the file is singleband
val sgt: SinglebandGeoTiff =
  SinglebandGeoTiff.streaming(path)
  // or
  GeoTiffReader.readSingleband(path, false, true)
sgt.crop(e)

// if the file is multiband
val mgt: MultibandGeoTiff =
  MultibandGeoTiff.streaming(path)
  // or
  GeoTiffReader.readMultiband(path, false, true)
mgt.crop(e)
```
- - -
### Demo
There is a demo for this project as well as some examples that can be found [here](./WindowedGeoTiffReaderDemo.md).
