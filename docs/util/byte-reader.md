## S3 GeoTiff Streaming

- [Introduction](#introduction)
- [Why Even Use ByteReader?](#why-even-use-bytereader)
  - [No Size Restrictions](#no-size-restrictions)
  - [Open Source](#open-source)
- [The Inner Workings of ByteReader](#the-inner-workings-of-bytereader)
  - [Going Right to the Source](#going-right-to-the-source)
  - [Reading the Chunks](#reading-the-chunks)

- - -
### Introduction

A common problem that faces those who perform geo-spatial analysis is dealing with importing and working with large GeoTiffs. The solution that has been developed and used in GeoTrellis is the `ByteReader` object. With this feature, one can work with files of any size. This document will explain the inner workings of this data structure, so that you can best utilize or even extend it for your own needs.

### Why Even Use ByteReader?
One of the first things that should be addressed is why should one use `ByteReader` over a `java.nio.ByteBuffer`. Which until now was the standard way of accessing data in GeoTrellis. There are two main advantages of using `ByteReader`, these are:

- No Size Restriction
- Open Source

Let's go through these both indiviually.

#### No Size Restriction
`ByteBuffer`s have a size restriction to them which makes it hard to use with large files. This puts a hamper on work flow as it requires the user to break down a file into smaller pieces and then load in each and reassemble them later on. Whereas `ByteReader` can deal with files of any size.

#### Open Source
If there was ever an instance where the user needs to extend `ByteBuffer`, they'd run into a confusing and vague error telling them that `ByteBuffer` is not expendable. Indeed, `ByteBuffer` is actually licensed by Sun and it's not only very difficult to extend `ByteBuffer`, but it can also cause legal trouble.

The benefit of `ByteReader` then is that it changed and/or expanded however the user desires.

### The Inner Workings of ByteReader
The concept of `ByteReader` is this:  given an input stream, read in sections of it at a time (these are called `chunk`s) and then from those `chunk`s get the desired values. If a `chunk` is used up or a position is requested that is outside of it, a new `chunk` is obtained.

#### Going Right to the Source
A `InputStream` of some kind is needed in order to get the `chunk`s. Thus, there exists an object called a `RangeReader`. What this object does is actually get a `chunk` from the stream for the `ByteReader` to read. There currently exists three different instances depending on where the file is being read from as can be seen in the image below.

![Alt text](https://geotrellis/geotrellis/docs/img/read-range-tree.jpg)

#### Reading the Chunks
Now that we have a `RangeReader`, it is time to create our `ByteReader`. Currently, there is only one instance of `ByteReader`, and that's `StreamingByteReader`. As the name suggests, this will perform stream readings of the `chunk`s. `ByteReader` requires a `RangeReader` and an optional `chunkSize` as its paramters. What is a `chunkSize`? A `chunkSize` is how many bytes should be read in at a time. Once created, the `ByteReader` will behave very much like a `ByteBuffer`, and it can be used throughout the GeoTrellis library.
