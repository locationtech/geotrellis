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
availble, less on RAM. In order not to loose performane should not be
used threads more than CPU cores availble for java machine, otherwise
that can lead to significant performance loss.
