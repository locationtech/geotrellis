package geotrellis.spark.io.hadoop

import geotrellis.spark._
import geotrellis.spark.util._
import geotrellis.spark.partition._
import geotrellis.spark.io.hadoop.formats._
import geotrellis.spark.io.index._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.avro.codecs._

import com.typesafe.scalalogging.slf4j.LazyLogging
import org.apache.hadoop.fs.Path
import org.apache.hadoop.io._
import org.apache.hadoop.mapreduce.lib.output._
import org.apache.hadoop.mapreduce.{Job, RecordWriter}
import org.apache.hadoop.conf.Configuration
import org.apache.spark.rdd._
import org.apache.spark.SparkContext

import scala.reflect._

object HadoopRDDWriter extends LazyLogging {

  /**
    * When record being written would exceed the block size of the current MapFile
    * opens a new file to continue writing. This allows to split partition into block-sized
    * chunks without foreknowledge of how big it is.
    */
  class MultiMapWriter(layerPath: String, partition: Int, blockSize: Long, indexInterval: Int) {
    private var writer: MapFile.Writer = null // avoids creating a MapFile for empty partitions
    private var bytesRemaining = 0l

    private def getWriter(firstIndex: Long) = {
      val path = new Path(layerPath, f"part-r-${partition}%05d-${firstIndex}")
      bytesRemaining = blockSize - 16*1024 // buffer by 16BK for SEQ file overhead
      val writer =
        new MapFile.Writer(
          new Configuration,
          path.toString,
          MapFile.Writer.keyClass(classOf[LongWritable]),
          MapFile.Writer.valueClass(classOf[BytesWritable]),
          MapFile.Writer.compression(SequenceFile.CompressionType.NONE))
      writer.setIndexInterval(indexInterval)
      writer
    }

    def write(key: LongWritable, value: BytesWritable): Unit = {
      val recordSize = 8 + value.getLength
      if (writer == null) {
        writer = getWriter(key.get)
      } else if (bytesRemaining - recordSize < 0) {
        writer.close()
        writer = getWriter(key.get)
      }
      writer.append(key, value)
      bytesRemaining -= recordSize
    }

    def close(): Unit =
      if (null != writer) writer.close()
  }

  def write[K: AvroRecordCodec: ClassTag, V: AvroRecordCodec: ClassTag](
    rdd: RDD[(K, V)],
    path: Path,
    keyIndex: KeyIndex[K],
    indexInterval: Int = 4
  ): Unit = {
    implicit val sc = rdd.sparkContext
    val fs = path.getFileSystem(sc.hadoopConfiguration)
    if(fs.exists(path)) throw new Exception(s"Directory already exists: $path")

    val codec = KeyValueRecordCodec[K, V]
    val blockSize = fs.getDefaultBlockSize(path)
    val layerPath = path.toString

    implicit val ord: Ordering[K] = Ordering.by(keyIndex.toIndex)
    rdd
      .repartitionAndSortWithinPartitions(IndexPartitioner(keyIndex, rdd.partitions.length))
      .mapPartitionsWithIndex[Unit] { (pid, iter) =>
        var writer = new MultiMapWriter(layerPath, pid, blockSize, indexInterval)
        for ( (index, pairs) <- GroupConsecutiveIterator(iter)(r => keyIndex.toIndex(r._1))) {
          writer.write(
            new LongWritable(index),
            new BytesWritable(AvroEncoder.toBinary(pairs.toVector)(codec)))
        }
        writer.close()
        // TODO: collect statistics on written records and return those
        Iterator.empty
      }.count

    fs.createNewFile(new Path(layerPath, "_SUCCESS"))
    logger.info(s"Finished saving tiles to ${path}")
  }
}
