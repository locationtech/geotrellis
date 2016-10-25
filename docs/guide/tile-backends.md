GeoTrellis isn't picky about how you store your data. This guide describes
the various tile layer backends we support, how to use them, and why you
might choose one over the other.

To Be or not to Be a Backend
----------------------------
The Scala classes that underpin each backend all inherit from the same group
of traits, meaning they agree on behaviour:

- `AttributeStore` - save and access layer attributes (metadata, etc.)
- `LayerReader` - read `RDD[(K, V)] with Metadata[M]`
- `LayerWriter` - write `RDD[(K, V)] with Metadata[M]`
- `LayerUpdater`
- `LayerReindexer`
- `LayerCopier`
- `LayerDeleter`
- `LayerMover`
- `LayerManager`

The top three are used most often.

File System
-----------

**Choose your file system if:** you want to perform local data ingests or processing.

HDFS
----

**Choose HDFS if:** ???

The [**Hadoop Distributed File System**](https://hadoop.apache.org/).
HDFS-based tile layer IO functionality is found in the `geotrellis-spark`
package, which makes it another default backend insofar as no other
GeoTrellis modules are necessary to get it going. It also doesn't require
any other external processes or containerization, since in the local case
HDFS uses your filesystem.

S3
--

**Choose S3 if:** you have large amounts of data to store, can pay for
external storage, and want to access the data from anywhere.

Accumulo
--------

Cassandra
---------

HBase
-----
