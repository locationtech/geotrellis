Architecture Decision Records
=============================

This is a collection of subdocuments that describe why (or why not) we
made a particular design decision in GeoTrellis.

0001 - Streaming Writes
-----------------------

Context
^^^^^^^

To write streaming data (e.g. ``RDD[(K, V)]``) to an ``S3`` backend it
is necessary to map over rdd partitions and to send multiple async PUT
requests for all elements of a certain partition, it is important to
synchronize these requests in order to be sure, that after calling a
``writer`` function all data was ingested (or at least attempted). Http
status error ``503 Service Unavailable`` requires resending a certain
PUT request (with exponential backoff) due to possible network problems
this error was caused by. ``Accumulo`` and ``Cassandra`` writers work in
a similar fashion.

To handle this situation we use the ``Task`` abstraction from Scalaz,
which uses it's own ``Future`` implementation. The purpose of this
research is to determine the possibility of removing the heavy
``Scalaz`` dependency. In a near future we will likely depend on the
``Cats`` library, which is lighter, more modular, and covers much of the
same ground as Scalaz. Thus, to depend on ``Scalaz`` is not ideal.

Decision
^^^^^^^^

We started by a moving from Scalaz ``Task`` to an implementation based
on the scala standard library ``Future`` abstraction. Because
``List[Future[A]]`` is convertable to ``Future[List[A]]`` it was thought
that this simpler home-grown solution might be a workable alternative.

Every ``Future`` is basically some calculation that needs to be
submitted to a thread pool. When you call
``(fA: Future[A]).flatMap(a => fB: Future[B])``, both ``Future[A]`` and
``Future[B]`` need to be submitted to the thread pool, even though they
are not running concurrently and could run on the same thread. If
``Future`` was unsuccessful it is possible to define recovery strategy
(in case of ``S3`` it is neccesary).

We faced two problems: difficulties in ``Future`` synchronization
(``Future.await``) and in ``Future`` delay functionality (as we want an
exponential backoff in the ``S3`` backend case).

We can await a ``Future`` until it's done (``Duration.Inf``), but we can
not be sure that ``Future`` was completed exactly at this point (for
some reason - this needs further investigation - it completes a bit
earlier/later).

Having a threadpool of ``Future``\ s and having some ``List[Future[A]``,
awaiting of these ``Futures`` does not guarantees completeness of each
``Future`` of a threadpool. Recovering a ``Future`` we produce a *new*
``Future``, so that recoved ``Future``\ s and recursive ``Future``\ s
are *new* ``Future``\ s in the same threadpool. It isn't obvious how to
await all *necessary* ``Future``\ s. Another problem is *delayed*
``Future``\ s, in fact such behaviour can only be achieved by creating
*blocking* ``Future``\ s. As a workaround to such a situation, and to
avoid *blocking* ``Future``\ s, it is possible to use a ``Timer``, but
in fact that would be a sort of separate ``Future`` pool.

Let's observe Scalaz ``Task`` more closely, and compare it to native
scala ``Future``\ s. With ``Task`` we recieve a bit more control over
calculations. In fact ``Task`` is not a concurrently running
computation, it’s a description of a computation, a lazy sequence of
instructions that may or may not include instructions to submit some of
calculations to thread pools. When you call
``(tA: Task[A]).flatMap(a => tB: Task[B])``, the ``Task[B]`` will by
default just continue running on the same thread that was already
executing ``Task[A]``. Calling ``Task.fork`` pushes the task into the
thread pool. Scalaz ``Task``\ s operates with their own ``Future``
implementation. Thus, having a stream of ``Task``\ s provides more
control over concurrent computations.

`Some
implementations <https://gist.github.com/pomadchin/33b53086cbf81a6256ddb452090e4e3b>`__
were written, but each had synchronization problems. This attempt to get
rid of the Scalaz dependency is not as trival as we had anticipated.

This is not a critical decision and, if necessary, we can come back to
it later.

Consequences
^^^^^^^^^^^^

All implementations based on ``Future``\ s are non-trival, and it
requires time to implement a correct write stream based on native
``Future``\ s.
`Here <https://gist.github.com/pomadchin/33b53086cbf81a6256ddb452090e4e3b>`__
are the two simplest and most transparent implementation variants, but
both have synchronization problems.

Scalaz ``Task``\ s seem to be better suited to our needs. ``Task``\ s
run on demand, and there is no requirement of instant submission of
``Task``\ s into a thread pool. As described above, ``Task`` is a lazy
sequence of intructions and some of them could submit calculations into
a thread pool. Currently it makes sense to depend on Scalaz.

0002 - HDFS Raster Layers
-------------------------

Context
^^^^^^^

Raster layer is a regular grid of raster tiles, represented as a
``RDD[(K, V)]`` where ``K`` contains the column, row, and/or time.
Raster layer storage scheme must support two forms of queries with
different requirements:

1. Distributed bounding box queries

   -  Minimum time between start of the query and time at which records
      are inspected for a match
   -  Minimum number of records discarded during query refinement stage

2. Key/Value look-ups

   -  Clear mapping from any K to a single block file
   -  Efficient seeks to any random value in the layer

HDFS does not provide any active index management so we must carefully
define a storage and indexing scheme that supports both of those cases.

Decision
^^^^^^^^

The design builds on an established pattern of mapping a
multi-dimensional tile key to a one-dimensional index using a space
filling curve (SFC). This requires definition of bounding spatial extent
and resolution but provides a total ordering for our records.

MapFiles
^^^^^^^^

The layer will be sorted and written to multiple Hadoop MapFiles.
``MapFile`` consist of two files:

-  ``data`` file is a ``SequenceFile`` of ``LongWritable`` and
   ``BytesWritable`` key/value pairs where the key is the SFC index and
   value bytes are Avro encoded ``Vector[(K,V)]`` where all ``K``\ s map
   to the given SFC index.
-  ``index`` file is a ``SequenceFile`` which maps a ``LongWritable`` in
   seen in ``data`` file to its offset at some defined
   ``indexInterval``.

When ``MapFile`` is open the ``index`` is read fully and allows fast
random seeks into the ``data`` file.

Each map file will consequently correspond to an SFC range from from
first to last key stored in the file. Because the whole layer is sorted
before being written we can assume that that ranges covered by the map
files are exclusive.

It will be important to know which SFC range each file corresponds to
and to avoid creating an addition overall index file we record the value
of the first SFC index stored in the map file as part of the file name.

We experimented with using a bloom filter index, but it did not appear
appropriate. Because each file will be restricted to be no bigger than a
single HDFS block (64M/128M) the time to compute and store the bloom
filter does not offer any speed improvements on per-file basis.

Single Value Queries
^^^^^^^^^^^^^^^^^^^^

In a single value query we are given an instance of ``K`` and we must
produce a corresponding ``V`` or an error. The first step is to locate
the ``MapFile`` which potentially contains ``(K, V)`` record. Because
the layer records are indexed by their SFC index we map ``K`` to
``i: Long`` and determine which file contains potential match by
examining the file listing and finding the file with maximum starting
index that is less than equal ``i``. At this point the ``MapFile`` must
be opened and queried for the key.

The file listing is a comparatively expensive operation that is cached
when we create a ``Reader[K, V]`` instance for a given layer from
``HadoopValueReader``. Additionally as we maintain an LRU cache of
``MapFiles``\ s as we open them to satisfy client requests. Because SFC
preserves some spatial locality of the records, geographically close
records are likely to be close in SFC index, and we expect key/value
queries to be geographically grouped, for instance requests from a map
viewer. This leads us to expect that ``MapFile`` LRU cache can have a
high hit-rate.

Once we have located a record with matching SFC index we must verify
that it contains a matching ``K``. This is important because several
distinct values of ``K`` can potentially map to the same SFC index.

Bounding Box Queries
^^^^^^^^^^^^^^^^^^^^

To implement bounding box queries we extend ``FileInputFormat``, the
critical task is to filter the potential file list to remove any files
which do not have a possible match. This step happens on the Spark
driver process so it is good to perform this task without opening the
files themselves. Again we exploit the fact that file names contain the
first index written and assume that a file covers SFC range from that
value until the starting index of the file with the next closest index.

Next the query bounding box is decomposed into separate list of SFC
ranges. A single contiguous bounding box will likely decompose into many
hundreds or even thousands of SFC ranges. These ranges represent all of
the points on SFC index which intersect the query region. Finally we
discard any ``MapFile`` whose SFC index range does not intersect the the
bounding box SFC ranges.

The job of inspecting each ``MapFile`` is distributed to executors which
perform in-sync traversal of query SFC ranges and file records until the
end of each candidate file is reached. The resulting list of records is
checked against the original bounding box as a query refinement step.

Layer Writing
^^^^^^^^^^^^^

When writing a layer we will receive ``RDD[(K, V)] with Metadata[M]``
with unknown partitioning. It is possible that two records which will
map to the same SFC index are in fact located on different partitions.

Before writing we must ensure that all records that map to a given SFC
index value reside on the same partition and we are able to write them
in order. This can be expressed as a
``rdd.groupByKey(k => sfcIndex(k)).sortByKey``. However we can avoid the
double shuffle implied here by partitioning the ``rdd`` on SFC index of
each record and defining partition breaks by inspecting dataset bounding
box which is a required part of ``M``. This approach is similar to using
``RangePartitioner`` but without the requirement of record sampling.
Critically we instruct Spark to sort the records by their SFC index
during the single shuffle cause by repartitioning.

With records thus partitioned and sorted we can start writing them to
``MapFile``\ s. Each produced file will have the name of
``part-r-<partition number>-<first record index>``. This is trivial to
do because we have the encoded record when we need to open the file for
writing. Additionally we keep track to number of bytes written to each
file so we can close it and roll over to a new file if the next record
written is about to cross the HDFS block boundary. Keeping files to a
single block is a standard advise that optimizes their locality, it is
now not possible to have a single file that is stored across two HDFS
nodes.

Consequences
^^^^^^^^^^^^

This storage strategy provides key features which are important for
performance:

-  Writing is handled using a single shuffle, which is minimum required
   to get consistency
-  Sorting the records allows us to view them as exclusive ranges and
   filter large number of files without opening them
-  Storing index information in the file name allows us to perform query
   planning without using a secondary index or opening any of the
   individual files
-  Individual files are guaranteed to never exceed block boundary
-  There is a clear and efficient mapping from any ``K`` to a file
   potentially containing the matching record

Testing showed that ``HadoopValueReader`` LRU caching strategy is
effective and it provides sufficient performance to support serving a
rendered tile layer to a web client directly from HDFS. It is likely
that this performance can be further improved by adding an actor-based
caching layer to re-order the requests and read ``MapFile``\ s in order.

Because each file represents an exclusive range and there is no layer
wide index to be updated there is a possibility of doing an incremental
layer update where we only change those ``MapFile``\ s which intersect
with the updated records.

0003 - Readers / Writers Multithreading
---------------------------------------

Context
^^^^^^^

Not all GeoTrellis readers and writers implemented using MR jobs
(Accumulo RDDReader, Hadoop RDDReaders), but using socket reads as well.
This (socket) this approach allows to define paralelizm level depending
on system configuration, like CPU, RAM, FS. In case of ``RDDReaders``,
that would be threads amount per rdd partition, in case of
``CollectionReaders``, that would be threads amount per whole
collection.

All numbers are more impericall rather than have strong theory
approvals. Test cluster works in a local network to exclude possible
network issues. Reads tested on ~900 objects per read request of landsat
tiles (`test
project <https://github.com/geotrellis/geotrellis-landsat-emr-demo>`__).

Test cluster
^^^^^^^^^^^^

-  Apache Spark 1.6.2
-  Apache Hadoop 2.7.2
-  Apache Accumulo 1.7.1
-  Cassandra 3.7

Decision
^^^^^^^^

Was benchmarked functions calls performace depending on RAM / and CPU
cores availble.

File Backend
^^^^^^^^^^^^

``FileCollectionReader`` optimal (or reasonable in most cases) pool size
equal to cores number. As well there could be FS restrictions, that
depends on a certain FS settings.

-  *collection.reader: number of CPU cores available to the virtual
   machine*
-  *rdd.reader / writer: number of CPU cores available to the virtual
   machine*

Hadoop Backend
^^^^^^^^^^^^^^

In case of ``Hadoop`` we can use up to 16 threads without reall
significant memory usage increment, as ``HadoopCollectionReader`` keeps
in cache up to 16 ``MapFile.Readers`` by default (by design). However
using more than 16 threads would not improve performance signifiicantly.

-  *collection.reader: number of CPU cores available to the virtual
   machine*

S3 Backend
^^^^^^^^^^

``S3`` threads number is limited only by the backpressure, and that's an
impericall number to have max performance and not to have lots of
useless failed requests.

-  *collection.reader: number of CPU cores available to the virtual
   machine, <= 8*
-  *rdd.reader / writer: number of CPU cores available to the virtual
   machine, <= 8*

Accumulo Backend
^^^^^^^^^^^^^^^^

Numbers in the table provided are average for warmup calls. Same results
valid for all backends supported, and the main really performance
valueable configuration property is avaible CPU cores, results table:

*4 CPU cores result (m3.xlarge):*

+-----------+-------------------+------------------------------------+
| Threads   | Reads time (ms)   | Comment                            |
+===========+===================+====================================+
| 4         | ~15,541           | -                                  |
+-----------+-------------------+------------------------------------+
| 8         | ~18,541           | ~500mb+ of ram usage to previous   |
+-----------+-------------------+------------------------------------+
| 32        | ~20,120           | ~500mb+ of ram usage to previous   |
+-----------+-------------------+------------------------------------+

*8 CPU cores result (m3.2xlarge):*

+-----------+-------------------+------------------------------------+
| Threads   | Reads time (ms)   | Comment                            |
+===========+===================+====================================+
| 4         | ~12,532           | -                                  |
+-----------+-------------------+------------------------------------+
| 8         | ~9,541            | ~500mb+ of ram usage to previous   |
+-----------+-------------------+------------------------------------+
| 32        | ~10,610           | ~500mb+ of ram usage to previous   |
+-----------+-------------------+------------------------------------+

-  *collection.reader: number of CPU cores available to the virtual
   machine*

Cassandra Backend
^^^^^^^^^^^^^^^^^

*4 CPU cores result (m3.xlarge):*

+-----------+-------------------+-----------+
| Threads   | Reads time (ms)   | Comment   |
+===========+===================+===========+
| 4         | ~7,622            | -         |
+-----------+-------------------+-----------+
| 8         | ~9,511            | Higher    |
|           |                   | load on a |
|           |                   | driver    |
|           |                   | node + (+ |
|           |                   | ~500mb of |
|           |                   | ram usage |
|           |                   | to        |
|           |                   | previous) |
+-----------+-------------------+-----------+
| 32        | ~13,261           | Higher    |
|           |                   | load on a |
|           |                   | driver    |
|           |                   | node + (+ |
|           |                   | ~500mb of |
|           |                   | ram usage |
|           |                   | to        |
|           |                   | previous) |
+-----------+-------------------+-----------+

*8 CPU cores result (m3.2xlarge):*

+-----------+-------------------+-----------+
| Threads   | Reads time (ms)   | Comment   |
+===========+===================+===========+
| 4         | ~8,100            | -         |
+-----------+-------------------+-----------+
| 8         | ~4,541            | Higher    |
|           |                   | load on a |
|           |                   | driver    |
|           |                   | node + (+ |
|           |                   | ~500mb of |
|           |                   | ram usage |
|           |                   | to        |
|           |                   | previous) |
+-----------+-------------------+-----------+
| 32        | ~7,610            | Higher    |
|           |                   | load on a |
|           |                   | driver    |
|           |                   | node + (+ |
|           |                   | ~500mb of |
|           |                   | ram usage |
|           |                   | to        |
|           |                   | previous) |
+-----------+-------------------+-----------+

-  *collection.reader: number of CPU cores available to the virtual
   machine*
-  *rdd.reader / writer: number of CPU cores available to the virtual
   machine*

Conclusion
^^^^^^^^^^

For all backends performance result are pretty similar to ``Accumulo``
and ``Cassandra`` backend numbers. In order not to duplicate data these
numbers were omitted. Thread pool size mostly depend on CPU cores
available, less on RAM. In order not to loose performance should not be
used threads more than CPU cores available for java machine, otherwise
that can lead to significant performance loss.

0004 - ETL Pipelne
------------------

Context
^^^^^^^

The current GeoTrellis ETL does not allow us to determine ETL as a pipeline of transformations / actions.
This document describes a new approach(inspired by [PDAL Pipeline](https://www.pdal.io/pipeline.html))
with a new ETL JSON description.

Decision
^^^^^^^^

We can divide the current ETL into the following steps:

* Load
* Reproject
* Tile
* Pyramid
* Render
* Save

It is possible to represent all these steps as JSON objects, and an array of these objects would be an ETL instruction pipeline.
There still would be three different json inputs: ``input.json``, ``output.json`` and ``backend-profiles.json`` as it seems to be
a reasonable way to divide parameters semantically.


input.json
^^^^^^^^^^

The input schema would be mostly without changes. The only difference would be in the field ``name``,
which should be added as a CLI argument, as an override option. That would allow us to skip some unnecessary
machinery, in the case where all inputs have the same name (handled as a single layer). Another option,
is to have the ``name`` field as an optional field of a ``{save | update | reindex}`` pipeline step.


backend-profiles.json
^^^^^^^^^^^^^^^^^^^^^

Without significant changes, as it already provides minimal information about backends credentials.

output.json
^^^^^^^^^^^

* Reproject
* Tile
* Pyramid
* Render
* Save | Update | Reindex

*Reproject definition:*

.. code:: javascript

  {
    "type": "reproject",
    "method": "{buffered | per-tile}",
    "crs": "{EPSG code | EPSG name | proj4 string}"
  }

* *method* — ``{buffered | per-tile}`` reproject methods
* *crs* — ``{EPSG code | EPSG name | proj4 string}`` destination CRS

*Tile definition:*

.. code:: javascript

 {
    "type": "tile",
    "maxZoom": 19,
    "tileSize": 256,
    "resampleMethod": "{nearest-neighbor | bilinear | cubic-convolution | cubic-spline | lanczos}",
    "layoutScheme": "zoomed",
    "keyIndexMethod": {
      "type": "{zorder | hilbert}",
      "temporalResolution": 86400000
    },
    "cellSize": {
      "width": 0.5,
      "height": 0.5
    },
    "partitions": 5000 // optional // tile into some layout scheme
  }

* *maxZoom* — max zoom level [optional field]
* *tileSize* — destination tile size [optional field]
* *resampleMethod* — ``{nearest-neighbor | bilinear | cubic-convolution | cubic-spline | lanczos}`` methods are possible
* *keyIndexMethod:*
    * *type* — ``{zorder | hilbert}``
    * *temporalResolution* — temporal resolution in ms, if specified it would be a temporal index [optional field]
* *cellSize* — cellSize [optional field]
* *partitions* — partitions number after tiling

*Pyramid definition:*

.. code:: javascript

  {
    "type": "pyramid"
  }

*Render definition:*

.. code:: javascript

  {
    "type": "render",
    "format": "{tiff | png}",
    "path": "{path | pattern}"
  }

* *format* — ``{tiff | png}`` supported formats
* *path* — ``{path | pattern}`` output path, can be specified as a pattern

*{Save | Update | Reindex} definition:*

.. code:: javascript

  {
    "type": "{save | update | reindex}",
    "name": "layer name",
    "backend": {
      "path": "path or table",
      "profile": "profile name"
    }
  }

* *name* — layer name, all inputs would be saved / updated / reindexed with that name
* *backend:*
    * *path* — path or table name
    * *profile* — profile name, can be specified in the ``backend-profiles.json``, default profiles available: ``{file | hadoop | s3}``

*Pipeline example:*

.. code:: javascript

  [
    {
      "type": "reproject",
      "method": "{buffered | per-tile}",
      "crs": "{EPSG code | EPSG name | proj4 string}",
      "cellSize": {
        "width": 0.5,
        "height": 0.5
      }
    },
    {
      "type": "tile",
      "maxZoom": 19,
      "tileSize": 256,
      "resampleMethod": "bilinear",
      "layoutScheme": "zoomed",
      "keyIndexMethod": {
        "type": "zorder",
        "temporalResolution": 86400000
      },
      "partitions": 5000
    },
    {
      "type": "pyramid"
    },
    {
      "type": "render",
      "format": "{tiff | png}",
      "path": "{path | pattern}"
    },
    {
      "type": "{save | reindex | update}",
      "name": "layer name",
      "backend": {
        "path": "path or table",
        "profile": "profile name"
      }
    }
  ]

Conclusion
^^^^^^^^^^

The current ``input.json`` and ``backend-profiles.json`` are seems to be already fine. Significant changes should be introduced
into ``output.json`` as specified above. That would allow us to construct Pipelines similar to what PDAL allows. In addition,
such an approach allows us to not have complicated API extensions, which can be implemented just by implementing desired
pipeline steps functions.
