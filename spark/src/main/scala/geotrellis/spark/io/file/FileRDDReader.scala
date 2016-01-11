package geotrellis.spark.io.file

import geotrellis.spark._
import geotrellis.spark.io.avro.codecs.KeyValueRecordCodec
import geotrellis.spark.io.index.{MergeQueue, KeyIndex, IndexRanges}
import geotrellis.spark.io.avro.{AvroEncoder, AvroRecordCodec}
import geotrellis.spark.utils.KryoWrapper
import geotrellis.spark.utils.cache.Cache
import geotrellis.raster.io.Filesystem

import org.apache.avro.Schema
import org.apache.commons.io.IOUtils
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

import spire.syntax.cfor._
import scala.collection.mutable
import scala.reflect.ClassTag

class FileRDDReader[K: Boundable: AvroRecordCodec: ClassTag, V: AvroRecordCodec: ClassTag](implicit sc: SparkContext) {

  def read(
    keyPath: Long => String,
    queryKeyBounds: Seq[KeyBounds[K]],
    decomposeBounds: KeyBounds[K] => Seq[(Long, Long)],
    writerSchema: Option[Schema] = None,
    cache: Option[Cache[Long, Array[Byte]]] = None,
    numPartitions: Int = sc.defaultParallelism
  ): RDD[(K, V)] = {
    val ranges = if (queryKeyBounds.length > 1)
      MergeQueue(queryKeyBounds.flatMap(decomposeBounds))
    else
      queryKeyBounds.flatMap(decomposeBounds)

    val bins = IndexRanges.bin(ranges, numPartitions)

    val boundable = implicitly[Boundable[K]]
    val includeKey = (key: K) => KeyBounds.includeKey(queryKeyBounds, key)(boundable)
    val _recordCodec = KeyValueRecordCodec[K, V]
    val kwWriterSchema = KryoWrapper(writerSchema) //Avro Schema is not Serializable
  
    sc.parallelize(bins, bins.size)
      .mapPartitions { partition: Iterator[Seq[(Long, Long)]] =>
        val resultPartition = mutable.ListBuffer[(K, V)]()

        for(
          rangeList <- partition;
          range <- rangeList
        ) {
          val (start, end) = range
          cfor(start)(_ <= end, _ + 1) { index =>
            val path = keyPath(index)
            val bytes: Array[Byte] =
              cache match {
                case Some(cache) =>
                  cache.getOrInsert(index, Filesystem.slurp(path))
                case None =>
                  Filesystem.slurp(path)
              }
            val recs = AvroEncoder.fromBinary(kwWriterSchema.value.getOrElse(_recordCodec.schema), bytes)(_recordCodec)
            resultPartition ++= recs.filter { row => includeKey(row._1) }
          }
        }

        resultPartition.iterator
      }
  }
}
