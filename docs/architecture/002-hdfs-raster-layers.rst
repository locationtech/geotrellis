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
