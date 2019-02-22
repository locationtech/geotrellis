Cassandra backend performance
*****************************

Bottom Line Up Front
====================

The existing schema encoding in Cassandra for the GeoTrellis backend is probably about as fast
as it can be for tile reads. 

Issues related to this research
------------------------------

* `Original issue <https://github.com/locationtech/geotrellis/issues/2831>`__
* `The actual PR <https://github.com/locationtech/geotrellis/pull/2855>`__
* `The historical branch with all changes related to this research <https://github.com/locationtech/geotrellis/tree/rnd/cassandra-indexing>`__

Idea
====

Some open source contributions were made to experiment with an alternative schema 
encoding for the Cassandra GeoTrellis backend.  The general idea was that the schema used today
makes no attempt to take advantage of the SFC locality guarantees that GeoTrellis
works so hard to provide.  It would be nice if it were possible to use those locality
guarantees to speed up read access to tiles.

Read performance is still quite good with the existing schema encoding, but the hope was
to make it even better by storing off keys in the same neighborhood within the SFC into 
the same partitions in Cassandra.  Typically partitions in Cassandra are used for range query, 
which is outside of scope for the GeoTrellis project since it goes against the underlying 
design principles of the tiling API and can lead to bottlenecks at the coordinator level.
However, if the partition size were configurable it could potentially be tuned and sized 
appropriately to take advantage of Cassandra's row and partition caches, effectively trading
heap space, marginal additional write overhead, and marginal additional storage overhead for 
faster read times.  Depending on the use case for GeoTrellis, it may be advantageous to 
make this trade when working in applications that are heavier on reads than they are on writes. 

That was the hope anyway... in practice the testing that was done did not seem to indicate 
any performance benefits.

Experiment
==========

The existing schema encoding for storing GeoTrellis tiles in Cassandra looks like this:

.. code:: sql

    CREATE TABLE geotrellis_bench.tiles_write (
        key varint,
        name text,
        zoom int,
        value blob,
        PRIMARY KEY (key, name, zoom)
    ) WITH CLUSTERING ORDER BY (name ASC, zoom ASC)
        AND bloom_filter_fp_chance = 0.01
        AND caching = {'keys': 'ALL', 'rows_per_partition': 'NONE'}
        AND comment = ''
        AND compaction = {'class': 'org.apache.cassandra.db.compaction.SizeTieredCompactionStrategy', 'max_threshold': '32', 'min_threshold': '4'}
        AND compression = {'chunk_length_in_kb': '64', 'class': 'org.apache.cassandra.io.compress.LZ4Compressor'}
        AND crc_check_chance = 1.0
        AND dclocal_read_repair_chance = 0.1
        AND default_time_to_live = 0
        AND gc_grace_seconds = 864000
        AND max_index_interval = 2048
        AND memtable_flush_period_in_ms = 0
        AND min_index_interval = 128
        AND read_repair_chance = 0.0
        AND speculative_retry = '99PERCENTILE';

The order in which the ``PRIMARY KEY`` is defined here is important as it dictates the size of the partition.
In this case, because ``key`` is declared as the first field in the ``PRIMARY KEY`` it is used as the ``Parition Key``
(in Cassandra parlance) and ``name`` and ``zoom`` are used as so-called ``Clustering Columns``.  The ``Partition Key``
identifies on which node, within the Cassandra ring, the row data is stored (this is computed via a murmer3 
hash by default).  The ``Clustering Columns`` identify the order in which the row is sorted on the node it
was hashed to by the ``Partition Key``.

The ``key`` here is the range of the SFC.  The ``name`` is the name of the layer, and the ``zoom`` is the zoom level.

When Cassandra performs a read, it does so at the partition level, meaning an entire partition is read into memory
at once.  It is therefore important to keep partition sizes down to avoid lots of heap churn within Cassandra.

Typically once a partition has been read, it is discarded after a query is completed.  Thus, only range queries 
benefit from partitions.  If, however, Cassandra's row cache could be leveraged to keep partitions "hot" in 
memory after an initial read for some delta of time, then it would be possible to leverage the locality assumptions 
of the SFC to potentially speed up amortized access time - namely that data in the same neighborhood of the SFC 
tends to be accessed at the same time. 

In an attempt to take advantage of this, a new CQL encoding was introduced that looks like this:

.. code:: sql

    CREATE TABLE geotrellis_bench.tiles_read (
        name text,
        zoom int,
        zoombin int,
        key varint,
        value blob,
        PRIMARY KEY ((name, zoom, zoombin), key)
    ) WITH CLUSTERING ORDER BY (key ASC)
        AND bloom_filter_fp_chance = 0.01
        AND caching = {'keys': 'ALL', 'rows_per_partition': '64'}
        AND comment = ''
        AND compaction = {'class': 'org.apache.cassandra.db.compaction.SizeTieredCompactionStrategy', 'max_threshold': '32', 'min_threshold': '4'}
        AND compression = {'chunk_length_in_kb': '64', 'class': 'org.apache.cassandra.io.compress.LZ4Compressor'}
        AND crc_check_chance = 1.0
        AND dclocal_read_repair_chance = 0.1
        AND default_time_to_live = 0
        AND gc_grace_seconds = 864000
        AND max_index_interval = 2048
        AND memtable_flush_period_in_ms = 0
        AND min_index_interval = 128
        AND read_repair_chance = 0.0
        AND speculative_retry = '99PERCENTILE';

Note that in this case the ``Partition Key`` is a ``Composite Key`` made up of several fields.  In this case, the
three fields together determine the partition in which the row is stored and the ``key``
(the remaining ``Clustering Column``) determines the order within the partition.

The new ``zoombin`` parameter is calculated by a partitioning of the range of the SFC similar to the code below:

.. code:: scala

    @transient private lazy val zoomBinIntervals: ZoomBinIntervals = {
      /**
        * NOTE: This makes an assumption that the range of a SFC index can not be updated in such a way
        * that the `indexRanges` would change without recomputing the index itself (and thus reindexing the data on disk).
        * If this is *NOT* the case then we'll need to compute these zoomBinIntervals "on the fly", which is
        * more computationally expensive and may discount any time savings we may gain from introducing the bin in the
        * first place.
        */
      val ranges = keyIndex.indexRanges(keyIndex.keyBounds)
    
      val binRanges = ranges.toVector.map{ range =>
        val vb = new VectorBuilder[Interval[BigInt]]()
        cfor(range._1)(_ <= range._2, _ + tilesPerPartition){ i =>
          vb += Interval.openUpper(i, i + tilesPerPartition)
        }
        vb.result()
      }
        
      CassandraIndexing.ZoomBinIntervals(binRanges.flatten.zipWithIndex)
    }

    private def zoomBin(
      index: BigInteger
    ): java.lang.Integer = {
      zoomBinIntervals.intervals.find{ case (interval, idx) => interval.contains(index) }.map {
        _._2: java.lang.Integer
      }.getOrElse(0: java.lang.Integer)
    }

The ``tilesPerPartition`` is a configuration-driven value chosen by the client.  It is also used as the value
for the ``rows_per_partition`` to cache in the Cassandra schema encoding and is positively correlated
both to partition sizes and heap usage by Cassandra instances. 

Testing Environment
-------------------

To benchmark the differences between this new (hereby termed "read-optimized") schema encoding and the 
existing (hereby termed "write-optimized") schema encoding, we compared write-heavy and read-heavy 
operations.  

Hardware:
~~~~~~~~~
 - Single Node 
 - 4-core (8 thread) Xeon processor
 - 64 GB RAM
 - SSD
 
Cassandra Setup:
~~~~~~~~~~~~~~~~
 - `ccm <https://github.com/riptano/ccm>`__
 - 3 instances
 - vnodes turned on, 256 vnodes per instance
 
More "production grade" testing would have been done, but access to cloud resources for testing were limited
so unfortunately the only benchmarking available was to simulate a full-scale Cassandra cluster 
on a local developer asset.

Workload:
---------
 - `CassandraIndexStrategySpec.scala <CassandraIndexStrategySpec.scala>`__
 - 15 iterations of writing all of Zoom Level 6 to the Cassandra backend (both read-optimized and write-optimized)
 - 100 iterations of reading 16 x 16 blocks of tiles from both read and write optimized schemas
 - tilesPerPartition = 64

Results
=======

For the write-heavy initial workload here are the results: 

.. note::

    Average write-time for READ optimized schema: 2451.9333333333334ms
    Average write-time for WRITE optimized schema: 1119.6666666666667ms
    STDDEV write-time for READ optimized schema: 973.7087495185041ms
    STDDEV write-time for WRITE optimized schema: 183.95712060755415ms

For the 16x16 read-heavy workload here are the results:

.. note::

    Average read-time for READ optimized schema: 311.19ms
    Average read-time for WRITE optimized schema: 135.7ms
    STDDEV read-time for READ optimized schema: 170.76438123917995ms
    STDDEV read-time for WRITE optimized schema: 23.697468219200122ms

Not only were the read-optimized times, on the average, significantly worse than the write-optimized times, 
they also exhibited more variance.  

Changing the ``tilesPerPartition`` did seem to speed up read times, but never to the extent that the read-optimized
schema beat out the write-optimized variant.  

With these disappointing results, further investigation was suspended. 

Future Work
===========

It would be interesting to run these tests against a production Cassandra cluster.  It would also be interesting
to fiddle with more of the input parameters to the test cases since there are a lot of different variables 
to contend with. 