package geotrellis.spark.io.hadoop.spatial

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.hadoop.formats._
import geotrellis.raster._

import org.apache.spark.{SparkContext, Logging}
import org.apache.spark.SparkContext._
import org.apache.hadoop.fs.Path
import org.apache.hadoop.io.SequenceFile
import org.apache.hadoop.mapreduce.lib.output.{MapFileOutputFormat, SequenceFileOutputFormat}
import org.apache.hadoop.mapreduce.Job


object SpatialRasterRDDWriterProvider extends RasterRDDWriterProvider[SpatialKey] with Logging {

  def index(tileLayout: TileLayout, keyBounds: KeyBounds[SpatialKey]): KeyIndex[SpatialKey] =
    new RowMajorSpatialKeyIndex(tileLayout.layoutCols)


  def writer(catalogConfig: HadoopRasterCatalogConfig, layerMetaData: HadoopLayerMetaData, keyIndex: KeyIndex[SpatialKey], clobber: Boolean = true)(implicit sc: SparkContext) = {
    val layerPath = layerMetaData.path

    new RasterRDDWriter[SpatialKey] {
      def write(layerId: LayerId, rdd: RasterRDD[SpatialKey]): Unit = {
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
        val sortedWritable =
          rdd
            .map { case (key, tile) => (SpatialKeyWritable(closureKeyIndex.toIndex(key), key), TileWritable(tile)) }
            .sortByKey(numPartitions = partitions)
            .cache

        // Run over the partitions and collect the first values, as they will be named the split values.
        // TODO: WRITE SPLITS
        // val splits: Array[K] =
        //   sortedWritable
        //     .mapPartitions { iter =>
        //     if(iter.hasNext) Seq(iter.next._1.toValue).iterator else sys.error(s"Empty partition.")
        //   }
        //     .collect

        // Write the RDD.
        sortedWritable
          .saveAsNewAPIHadoopFile(
          layerPath.toUri.toString,
            classOf[SpatialKeyWritable],
            classOf[TileWritable],
            classOf[MapFileOutputFormat],
            job.getConfiguration
        )

//        writeSplits(splits, layerPath, conf)
        logInfo(s"Finished saving tiles to ${layerPath}")
      }

    }
  }
}
