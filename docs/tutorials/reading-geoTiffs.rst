Reading GeoTiffs
================

This tutorial will go over how to read GeoTiff files using GeoTrellis on
your local filesystem. It assumes that you already have the environment
needed to run these examples. If not, please see our `Setup
Guide <setup.html>`__ to get GeoTrellis working on your system. Also, this
tutorial uses GeoTiffs from the ``raster`` project from GeoTrellis.
If you have not already done so, please clone GeoTrellis
`here <https://github.com/locationtech/geotrellis>`__ so that you can
access the needed files.

One of the most common methods of storing geospatial information is
through GeoTiffs. This is reflected throughout the GeoTrellis library
where many of its features can work with GeoTiffs. Which would mean that
there would have to be many different ways to read in GeoTiff, and
indeed there are! In the following document, we will go over the methods
needed to load in a GeoTiff from your local filesystem.

Before we start, open a Scala REPL in the Geotrellis directory.

Reading For the First Time
--------------------------

Reading a local GeoTiff is actually pretty easy. You can see how to do
it below.

.. code:: scala

    import geotrellis.raster.io.geotiff.reader.GeoTiffReader
    import geotrellis.raster.io.geotiff._

    val path: String = "path/to/geotrellis/raster/data/geotiff-test-files/lzw_int32.tif"
    val geoTiff: SinglebandGeoTiff = GeoTiffReader.readSingleband(path)

And that's it! Not too bad at all really, just four lines of code. Even
still, though, let's break this down line-by-line so we can see what
exactly is going on.

.. code:: scala

    import geotrellis.raster.io.geotiff.reader.GeoTiffReader

This import statement brings in ``GeoTiffReader`` from
``geotrellis.raster.io.geotiff.reader`` so that we can use it in the
REPL. As the name implys, ``GeoTiffReader`` is the object that actually
reads the GeoTiff. If you ever wonder about how we analyze and process
GeoTiffs, then ``geotrellis.raster.io.geotiff`` would be the place to
look. Here's a
`link <https://github.com/locationtech/geotrellis/tree/master/raster/src/main/scala/geotrellis/raster/io/geotiff>`__.

.. code:: scala

    import geotrellis.raster.io.geotiff._

The next import statement loads in various data types that we need so
that we can assign them to our ``val``\ s.

Okay, so we brought in the object that will give us our GeoTiff, now we
just need to supply it what to read. This is where the next line of code
comes into play.

.. code:: scala

    val path: String = "path/to/geotrellis/raster/data/geotiff-test-files/lzw_int32.tif"

Our ``path`` variable is a ``String`` that contains the file path to a
GeoTiff in ``geotrellis.raster``. ``GeoTiffReader`` will use this
value then to read in our GeoTiff. There are more types of paramters
``GeoTiffReader`` can accept, however. These are ``Array[Byte]``\ s and
``ByteReader``\ s. We will stick with ``String``\ s for this lesson, but
``Array[Byte]`` is not that much different. It's just all of the bytes
within your file held in an Array.

The last part of our four line coding escapade is:

.. code:: scala

    val geoTiff: SinglebandGeoTiff = GeoTiffReader.readSingleband(path)

This line assigns the variable, ``geoTiff``, to the file that is being
read in. Notice the ``geoTiff``'s type, though. It is
``SinglebandGeoTiff``. Why does ``geoTiff`` have this type? It's because
in GeoTrellis, ``SinglebandGeoTiff``\ s and ``MutlibandGeoTiff``\ s are
two seperate subtypes of ``GeoTiff``. In case you were wondering about
the second ``import`` statement earlier, this is where is comes into
play; as these two types are defined within
``geotrellis.raster.io.geotiff``.

Great! We have a ``SinglebandGeoTiff``. Let's say that we have a
``MultibandGeoTiff``, though; let's use the code from above to read it.

.. code:: scala

    import geotrellis.raster.io.geotiff.reader.GeoTiffReader
    import geotrellis.raster.io.geotiff._

    // now a MultibandGeoTiff!
    val path: String = "path/to/raster/data/geotiff-test-files/3bands/3bands-striped-band.tif"
    val geoTiff = GeoTiffReader.readSingleband(path)

If we run this code, what do you think will happen? The result may surprise
you, we get back a ``SinglebandGeoTiff``! When told to read a
``SinglebandGeoTiff`` from a ``MultibandGeoTiff`` without a return type, the
``GeoTiffReader`` will just read in the first band of the file and return
that. Thus, it is important to keep in mind what kind of GeoTiff you are
working with, or else you could get back an incorrect result.

To remedy this issue, we just have to change the method call and return
type so that ``GeoTiffReader`` will read in all of the bands of our
GeoTiff.

.. code:: scala

    val geoTiff: MultibandGeoTiff = GeoTiffReader.readMultiband(path)

And that's it! We now have our ``MutlibandGeoTiff``.

Beginner Tip
^^^^^^^^^^^^

A good way to ensure that your codes works properly is to give the
return data type for each of your ``val``\ s and ``def``\ s. If by
chance your return type and is different from what is actually returned,
the compiler will throw an error. In addition, this will also make your
code easier to read and understand for both you and others as well.
Example:

.. code:: scala

    val multiPath = "path/to/a/multiband/geotiff.tif"

    // This will give you the wrong result!
    val geoTiff = GeoTiffReader.readSingleband(multiPath)

    // This will cause your compiler to throw an error
    val geoTiff: MultibandGeoTiff = GeoTiffReader.readSingleband(multiPath)

Before we move on to the next section, I'd like to take moment and talk
about an alternative way in which you can read in GeoTiffs. Both
``SinglebandGeoTiff``\ s and ``MultibandGeoTiff``\ s have their own
``apply`` methods, this means that you can give your parameter(s)
directly to their companion objects and you'll get back a new instance
of the class.

For ``SinglebandGeoTiff``\ s:

.. code:: scala

    import geotrellis.raster.io.geotiff.SinglebandGeoTiff

    val path: String = "path/to/raster/data/geotiff-test-files/lzw_int32.tif"
    val geoTiff: SinglebandGeoTiff = SinglebandGeoTiff(path)

There are two differences found within this code from the previous
example. The first is this:

.. code:: scala

    import geotrellis.raster.io.geotiff.SinglebandGeoTiff

As stated earlier, ``SinglebandGeoTiff`` and ``MultibandGeoTiff`` are
found within a different folder of ``geotrellis.raster.io.geotiff``.
This is important to keep in mind when importing, as it can cause your
code not to compile if you refer to the wrong sub-folder.

The second line that was changed is:

.. code:: scala

    val geoTiff: SinglebandGeoTiff = SinglebandGeoTiff(path)

Here, we see ``SinglebandGeoTiff``'s ``apply`` method being used on
``path``. Which returns the same thing as
``GeoTiffReader.readSingleband(path)``, but with less verbosity.

``MultibandGeoTiff``\ s are the exact same as their singleband
counterparts.

.. code:: scala

    import geotrellis.raster.io.geotiff.MultibandGeoTiff

    val path: String = "raster/data/geotiff-test-files/3bands/3bands-striped-band.tif"
    val geoTiff: MultibandGeoTiff = MultibandGeoTiff(path)

Our overview of basic GeoTiff reading is now done! But keep reading! For
you have greater say over how your GeoTiff will be read than what has
been shown. - - -

Expanding Our Vocab
-------------------

We can read GeoTiffs, now what? Well, there's actually more that we can
do when reading in a file. Sometimes you have a compressed GeoTiff, or
other times you might want to read in only a sub-section of GeoTiff and
not the whole thing. In either case, GeoTrellis can handle these issues
with ease.

Dealing With Compressed GeoTiffs
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Compression is a method in which data is stored with fewer bits and can
then be uncompressed so that all data becomes available. This applies to
GeoTiffs as well. When reading in a GeoTiff, you can state whether or
not you want a compressed file to be uncompressed or not.

.. code:: scala

    import geotrellis.raster.io.geotiff.reader.GeoTiffReader
    import geotrellis.raster.io.geotiff._

    // reading in a compressed GeoTiff and keeping it compressed
    val compressedGeoTiff: SinglebandGeoTiff = GeoTiffReader.readSingleband("path/to/compressed/geotiff.tif", false, false)

    // reading in a compressed GeoTiff and uncompressing it
    val compressedGeoTiff: SinglebandGeoTiff = GeoTiffReader.readSingleband("path/to/compressed/geotiff.tif", true, false)

As you can see from the above code sample, the first ``Boolean`` value
is what determines whether or not the file should be decompressed or
not. What does the other ``Boolean`` value for? We'll get to that soon!
For right now, though, we'll just focus on the first one.

Why would you want to leave a file compressed or have uncompressed when
reading it? One of the benefits of using compressed GeoTiffs is that
might lead to better performance depending on your system and the size
of the file. Another instance where the compression is needed is if your
file is over 4GB is size. This is because when a GeoTiff is uncompressed
in GeoTrellis, it is stored in an Array. Anything over 4GB is larger
than the max array size for Java, so trying read in anything bigger will
cause your process to crash.

By default, decompression occurs on all read GeoTiffs. Thus, these two
lines of code are the same.

.. code:: scala

    // these will both return the same thing!
    GeoTiffReader.readSingleband("path/to/compressed/geotiff.tif")
    GeoTiffReader.readSingleband("path/to/compressed/geotiff.tif", true, false)

In addition, both ``SinglebandGeoTiff`` and ``MultibandGeoTiff`` have a
method, ``compressed``, that uncompresses a GeoTiff when it is read in.

.. code:: scala

    SinglebandGeoTiff.compressed("path/to/compressed/geotiff.tif")
    MultibandGeoTiff.compressed("path/to/compressed/geotiff.tif")

Streaming GeoTiffs
^^^^^^^^^^^^^^^^^^

Remember that mysterious second parameter from earlier? It determines if
a GeoTiff should be read in via streaming or not. What is streaming?
Streaming is process of not reading in all of the data of a file at
once, but rather getting the data as you need it. It's like a "lazy
read". Why would you want this? The benefit of streaming is that it
allows you to work with huge or just parts of files. In turn, this makes
it possible to read in sub-sections of GeoTiffs and/or not having to
worry about memory usage when working with large files.

Tips For Using This Feature
'''''''''''''''''''''''''''

It is important to go over the strengths and weaknesses of this feature
before use. If implemented well, the WindowedGeoTiff Reader can save you
a large amount of time. However, it can also lead to further problems if
it is not used how it was intended.

It should first be stated that this reader was made to read in **sections**
of a Geotiff. Therefore, reading in either the entire, or close to the whole
file will either be comparable or slower than reading in the entire file at
once and then cropping it. In addition, crashes may occur depending on the
size of the file.

Reading in Small Files
''''''''''''''''''''''

Smaller files are GeoTiffs that are less than or equal to 4GB in isze.
The way to best utilize the reader for these kinds of files differs from
larger ones.

To gain optimum performance, the principle to follow is: **the smaller
the area selected, the faster the reading will be**. What the exact
performance increase will be depends on the bandtype of the file. The
general pattern is that the larger the datatype is, quicker it will be
at reading. Thus, a Float64 GeoTiff will be loaded at a faster rate than
a UByte GeoTiff. There is one caveat to this rule, though. Bit bandtype
is the smallest of all the bandtypes, yet it can be read in at speed
that is similar to Float32.

For these files, 90% of the file is the cut off for all band and storage
types. Anything more may cause performance declines.

Reading in Large Files
''''''''''''''''''''''

Whereas small files could be read in full using the reader, larger files
cannot as they will crash whatever process you're running. The rules for
these sorts of files are a bit more complicated than that of their
smaller counterparts, but learning them will allow for much greater
performance in your analysis.

One similarity that both large and small files share is that they have
the same principle: **the smaller the area selected, the faster the
reading will be**. However, while smaller files may experience slowdown
if the selected area is too large, these bigger files will crash.
Therefore, this principle must be applied more strictly than with the
previous file sizes.

In large files, the pattern of performance increase is the reverse of
the smaller files. Byte bandtype can not only read faster, but are able
to read in larger areas than bigger bandtypes. Indeed, the area which
you can select is limited to what the bandtype of the GeoTiff is. Hence,
an additional principle applies for these large files: **the smaller the
bandtype, the larger of an area you can select**. The exact size for
each bandtype is not known, estimates have been given in the table
bellow that should provide some indication as to what size to select.

+------------+---------------------------------+
| BandType   | Area Threshold Range In Cells   |
+============+=================================+
| Byte       | [5.76 \* 109, 6.76 \* 109)      |
+------------+---------------------------------+
| Int16      | [3.24 \* 109, 2.56 \* 109)      |
+------------+---------------------------------+
| Int32      | [1.44 \* 109, 1.96 \* 109)      |
+------------+---------------------------------+
| UInt16     | [1.96 \* 109, 2.56 \* 109)      |
+------------+---------------------------------+
| UInt32     | [1.44 \* 109, 1.96 \* 109)      |
+------------+---------------------------------+
| Float32    | [1.44 \* 109, 1.96 \* 109)      |
+------------+---------------------------------+
| Float64    | [3.6 \* 108, 6.4 \* 108)        |
+------------+---------------------------------+

--------------

How to Use This Feature
'''''''''''''''''''''''

Using this feature is straight forward and easy. There are two ways to
implement the WindowedReader: Supplying the desired extent with the path
to the file, and cropping an already existing file that is read in
through a stream.

Using Apply Methods


Supplying an extent with the file's path and having it being read in
windowed can be done in the following ways:

.. code:: scala

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

    // supplying the extent as an Option[Extent]

    // if the file is singleband
    SinglebandGeoTiff(path, Some(e))
    // or
    GeoTiffReader.readSingleband(path, Some(e))

    // if the file is multiband
    MultibandGeoTiff(path, Some(e))
    // or
    GeoTiffReader.readMultiband(path, Some(e))

Using Object Methods


Cropping an already loaded GeoTiff that was read in through Streaming.
By using this method, the actual file isn't loaded into memory, but its
data can still be accessed. Here's how to do the cropping:

.. code:: scala

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

--------------

Conclusion
----------

That takes care of reading local GeoTiff files! It should be said,
though, that what we went over here does not just apply to reading local
files. In fact, reading in GeoTiffs from other sources have similar
parameters that you can use to achieve the same goal.
