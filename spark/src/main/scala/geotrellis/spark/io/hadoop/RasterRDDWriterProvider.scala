package geotrellis.spark.io.hadoop

import geotrellis.spark._
import geotrellis.spark.utils._
import geotrellis.spark.io._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.hadoop.formats._
import geotrellis.spark.io.index._
import geotrellis.raster._

import org.apache.hadoop.fs.Path
import org.apache.spark.{SparkContext, Logging}
import org.apache.spark.SparkContext._
import org.apache.hadoop.fs.Path
import org.apache.hadoop.io.SequenceFile
import org.apache.hadoop.mapreduce.lib.output.{MapFileOutputFormat, SequenceFileOutputFormat}
import org.apache.hadoop.mapreduce.Job

import scala.reflect._

trait RasterRDDWriterProvider[Key] extends Logging {

  // A Key.type, an apply method for it, and typeclasses for classtagging and ordering must be defined
  type KeyWritable
  implicit val kwCTaggable: ClassTag[KeyWritable]
  implicit val kwOrderable: Ordering[KeyWritable]
  val applyKeyWritable: (Long, Key) => KeyWritable

  def writer(catalogConfig: HadoopRasterCatalogConfig, layerMetaData: HadoopLayerMetaData, keyIndex: KeyIndex[Key], clobber: Boolean = true)(implicit sc: SparkContext): RasterRDDWriter[Key] = {
    val layerPath = layerMetaData.path

    new RasterRDDWriter[Key] {
      def write(layerId: LayerId, rdd: RasterRDD[Key]): Unit = {
        val conf = sc.hadoopConfiguration

        val fs = layerPath.getFileSystem(sc.hadoopConfiguration)

        if(fs.exists(layerPath)) {
          if(clobber) {
            logDebug(s"Deleting $layerPath")
            fs.delete(layerPath, true)
          } else
            sys.error(s"Directory already exists: $layerPath")
        }

        val job = Job.getInstance(conf)
        job.getConfiguration.set("io.map.index.interval", "1")
        SequenceFileOutputFormat.setOutputCompressionType(job, SequenceFile.CompressionType.RECORD)

        logInfo(s"Saving RasterRDD for $layerId to ${layerPath}")

        // Figure out how many partitions there should be based on block size.
        val partitions = {
          val blockSize = fs.getDefaultBlockSize(layerPath)
          val tileCount = rdd.count
          val tileSize = rdd.metaData.tileLayout.tileSize * rdd.metaData.cellType.bytes
          val tilesPerBlock = {
            val tpb = (blockSize / tileSize) * catalogConfig.compressionFactor
            if(tpb == 0) {
              logWarning(s"Tile size is too large for this filesystem (tile size: $tileSize, block size: $blockSize)")
              1
            } else tpb
          }

          math.ceil(tileCount / tilesPerBlock.toDouble).toInt
        }

        // Sort the writables, and cache as we'll be computing this RDD twice.
        val closureKeyIndex = keyIndex
        val sortedWritable = {
          // Define methods called within KryoClosure inside a local scope
          val app = applyKeyWritable
          rdd
            .map(KryoClosure { case (key, tile) => (app(closureKeyIndex.toIndex(key), key), TileWritable(tile)) })
            .sortByKey(numPartitions = partitions)
            .cache
        }

        sortedWritable
          .saveAsNewAPIHadoopFile(
            layerPath.toUri.toString,
            classTag[KeyWritable].runtimeClass,
            classOf[TileWritable],
            classOf[MapFileOutputFormat],
            job.getConfiguration
        )

        logInfo(s"Finished saving tiles to ${layerPath}")
      }
    }
  }
}
