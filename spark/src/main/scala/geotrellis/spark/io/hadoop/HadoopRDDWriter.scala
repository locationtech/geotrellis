package geotrellis.spark.io.hadoop

import com.typesafe.scalalogging.slf4j.LazyLogging
import geotrellis.spark._
import geotrellis.spark.io.hadoop.formats._
import geotrellis.spark.io.index._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.avro.codecs._
import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext
import org.apache.hadoop.fs.Path
import org.apache.hadoop.io._
import org.apache.hadoop.mapreduce.lib.output.{MapFileOutputFormat, SequenceFileOutputFormat}
import org.apache.hadoop.mapreduce.Job
import scala.reflect._


object HadoopRDDWriter extends LazyLogging {

  def write[K: AvroRecordCodec, V: AvroRecordCodec](
    rdd: RDD[(K, V)],
    path: Path,
    keyIndex: KeyIndex[K],
    clobber: Boolean = true,
    tileSize: Int = 256*256*8,
    compressionFactor: Double = 1.3
  )(implicit sc: SparkContext): Unit = {

    val conf = sc.hadoopConfiguration

    val fs = path.getFileSystem(sc.hadoopConfiguration)

    if(fs.exists(path)) {
      if(clobber) {
        logger.debug(s"Deleting $path")
        fs.delete(path, true)
      } else
        throw new Exception(s"Directory already exists: $path")
    }

    val job = Job.getInstance(conf)
    job.getConfiguration.set("io.map.index.interval", "1")
    SequenceFileOutputFormat.setOutputCompressionType(job, SequenceFile.CompressionType.RECORD)

    // Figure out how many partitions there should be based on block size.
    val partitions = {
      val blockSize = fs.getDefaultBlockSize(path)
      val tileCount = rdd.count()
      val tilesPerBlock = {
        val tpb = (blockSize / tileSize) * compressionFactor
        if(tpb == 0) {
          logger.warn(s"Tile size is too large for this filesystem (tile size: $tileSize, block size: $blockSize)")
          1
        } else tpb
      }
      math.ceil(tileCount / tilesPerBlock.toDouble).toInt
    }

    // Sort the writables, and cache as we'll be computing this RDD twice.
    val closureKeyIndex = keyIndex
    val codec = KeyValueRecordCodec[K, V]

    rdd
      .groupBy { case (key, _) => closureKeyIndex.toIndex(key) }
      .map { case (index, pairs) =>
        (new LongWritable(index), new BytesWritable(AvroEncoder.toBinary(pairs.toVector)(codec)))
      }
      .sortByKey(numPartitions = partitions)
      .saveAsNewAPIHadoopFile(
        path.toUri.toString,
        classOf[LongWritable],
        classOf[BytesWritable],
        classOf[MapFileOutputFormat],
        job.getConfiguration
      )

    logger.info(s"Finished saving tiles to ${path}")
  }
}
