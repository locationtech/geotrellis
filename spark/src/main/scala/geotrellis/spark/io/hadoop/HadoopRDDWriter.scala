package geotrellis.spark.io.hadoop

import com.typesafe.scalalogging.slf4j.LazyLogging
import geotrellis.spark._
import geotrellis.spark.io.hadoop.formats._
import geotrellis.spark.io.index._
import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext
import org.apache.hadoop.fs.Path
import org.apache.hadoop.io.SequenceFile
import org.apache.hadoop.mapreduce.lib.output.{MapFileOutputFormat, SequenceFileOutputFormat}
import org.apache.hadoop.mapreduce.Job
import scala.reflect._


class HadoopRDDWriter[K, V](catalogConfig: HadoopCatalogConfig)(implicit format: HadoopFormat[K, V]) extends LazyLogging {

  def write(
    rdd: RDD[(K, V)],
    path: Path,
    keyIndex: KeyIndex[K],
    clobber: Boolean = true,
    tileSize: Int = 256*256*8)
  (implicit sc: SparkContext): Unit = {
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
        val tpb = (blockSize / tileSize) * catalogConfig.compressionFactor
        if(tpb == 0) {
          logger.warn(s"Tile size is too large for this filesystem (tile size: $tileSize, block size: $blockSize)")
          1
        } else tpb
      }
      math.ceil(tileCount / tilesPerBlock.toDouble).toInt
    }

    // Sort the writables, and cache as we'll be computing this RDD twice.
    val closureKeyIndex = keyIndex
    val fmt = format
    implicit val ctk: ClassTag[fmt.KW] = ClassTag(fmt.kClass)
    implicit val ctv: ClassTag[fmt.VW] = ClassTag(fmt.vClass)

    rdd
      .map { case (key, value) =>
        val kw = fmt.kClass.newInstance()
        kw.set(closureKeyIndex.toIndex(key), key)
        val kv = fmt.vClass.newInstance()
        kv.set(value)
        (kw, kv)
      }
      .sortByKey(numPartitions = partitions)
      .saveAsNewAPIHadoopFile(
        path.toUri.toString,
        fmt.kClass,
        fmt.vClass,
        fmt.fullOutputFormatClass,
        job.getConfiguration
      )

    logger.info(s"Finished saving tiles to ${path}")
  }
}

