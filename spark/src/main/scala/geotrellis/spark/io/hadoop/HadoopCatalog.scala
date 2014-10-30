package geotrellis.spark.io.hadoop

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.hadoop.formats._

import org.apache.hadoop.fs.Path
import org.apache.hadoop.io.SequenceFile
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat
import org.apache.hadoop.mapred.SequenceFileOutputFormat
import org.apache.hadoop.mapred.MapFileOutputFormat
import org.apache.hadoop.mapred.JobConf
import org.apache.spark._
import org.apache.spark.SparkContext._
import org.apache.spark.rdd._
import scala.reflect._
import scala.util.{Failure, Success, Try}

class HadoopCatalog private (sc: SparkContext, val metaDataCatalog: HadoopMetaDataCatalog, rootPath: Path) extends Catalog with Logging {
  type Params = Path
  type SupportedKey[K] = HadoopWritable[K]

  // assume tiles can be compressed 30% (so, compressionFactor - 1)
  final val COMPRESSION_FACTOR = 1.3

  // TODO: Figure out how we're laying out the rasters in HDFS.
  // Maybe inject a strategy for this?
  def paramsFor(layerId: LayerId): Path =
    new Path(new Path(rootPath, layerId.name), layerId.zoom.toString)

  def load[K: HadoopWritable: ClassTag](metaData: LayerMetaData, path: Path, filters: FilterSet[K]): Try[RasterRDD[K]] =
    Try {
      val keyWritable = implicitly[HadoopWritable[K]]
      import keyWritable.implicits._

      val conf = sc.hadoopConfiguration

      val updatedConf =
        sc.hadoopConfiguration.withInputPath(path.suffix(HadoopCatalog.SEQFILE_GLOB))

      val writableRdd: RDD[(keyWritable.Writable, TileWritable)] =
        if(filters.isEmpty) {
          sc.newAPIHadoopRDD[keyWritable.Writable, TileWritable, SequenceFileInputFormat[keyWritable.Writable, TileWritable]](
            updatedConf,
            classOf[SequenceFileInputFormat[keyWritable.Writable, TileWritable]],
            classTag[keyWritable.Writable].runtimeClass.asInstanceOf[Class[keyWritable.Writable]], // ¯\_(ツ)_/¯
            classOf[TileWritable]
          )
        } else {
          val partitioner = KeyPartitioner[K](HadoopCatalog.readSplits(path, conf))

          val includeKey: keyWritable.Writable => Boolean =
          { writable =>
            filters.includeKey(writable.toValue)
          }

          val includePartition: Partition => Boolean =
          { partition =>
            val minKey = partitioner.minKey(partition.index)
            val maxKey = partitioner.maxKey(partition.index)

            filters.includePartition(minKey, maxKey)
          }

          new PreFilteredHadoopRDD[keyWritable.Writable, TileWritable](
            sc,
            classOf[SequenceFileInputFormat[keyWritable.Writable, TileWritable]],
            classTag[keyWritable.Writable].runtimeClass.asInstanceOf[Class[keyWritable.Writable]],
            classOf[TileWritable],
            updatedConf
          )(includePartition)(includeKey)
        }

      val md = metaData.rasterMetaData
      asRasterRDD(md) {
        writableRdd
          .map { case (keyWritable, tileWritable) =>
            (keyWritable.toValue, tileWritable.toTile(md))
        }
      }

    }

  def save[K: HadoopWritable: ClassTag](rdd: RasterRDD[K], layerMetaData: LayerMetaData, path: Path): Try[Unit] =
    Try {
      val keyWritable = implicitly[HadoopWritable[K]]
      import keyWritable.implicits._

      val conf = rdd.context.hadoopConfiguration
      val jobConf = new JobConf(conf)

      jobConf.set("io.map.index.interval", "1")
      SequenceFileOutputFormat.setOutputCompressionType(jobConf, SequenceFile.CompressionType.RECORD)

      val pathString = path.toUri.toString

      logInfo("Saving RasterRDD to ${path.toUri.toString} out...")

      // Figure out how many partitions there should be based on block size.
      val partitions = {
        val fs = path.getFileSystem(conf)
        val blockSize = fs.getDefaultBlockSize(path)
        val tileCount = rdd.count
        val tileSize = layerMetaData.rasterMetaData.tileLayout.tileSize * layerMetaData.rasterMetaData.cellType.bytes
        val tilesPerBlock = {
          val tpb = (blockSize / tileSize) * COMPRESSION_FACTOR
          if(tpb == 0) {
            logWarning(s"Tile size is too large for this filesystem (tile size: $tileSize, block size: $blockSize)")
            1
          } else tpb
        }

        math.ceil(tileCount / tilesPerBlock.toDouble).toInt
      }

      // Sort the writables, and cache as we'll be computing this RDD twice.
      val sortedWritable =
        rdd
          .map { case (key, tile) => (key.toWritable, TileWritable(tile)) }
          .sortByKey(numPartitions = partitions)
          .cache

      // Run over the partitions and collect the first values, as they will be named the split values.
      val splits: Array[K] = 
        sortedWritable
          .mapPartitions { iter =>
            if(iter.hasNext) Seq(iter.next._1.toValue).iterator else sys.error(s"Empty partition.")
           }
          .collect

      HadoopCatalog.writeSplits(splits, path, conf)

      // Write the RDD.
      sortedWritable
        .saveAsHadoopFile(
          pathString,
          implicitly[ClassTag[keyWritable.Writable]].runtimeClass,
          classOf[TileWritable],
          classOf[MapFileOutputFormat],
          jobConf
        )

      logInfo(s"Finished saving tiles to ${path}")
    }
}

object HadoopCatalog {
  final val SPLITS_FILE = "splits"
  final val SEQFILE_GLOB = "/*[0-9]*/data"

  def apply(sc: SparkContext, rootPath: Path): HadoopCatalog = {
    val fs = rootPath.getFileSystem(sc.hadoopConfiguration)
    // Ensure root directory exists.
    if(!fs.exists(rootPath)) {
      fs.mkdirs(rootPath)
    } else {
      if(!fs.isDirectory(rootPath)) {
        sys.error(s"Catalog path $rootPath does not exist on file system ${fs.getUri}")
      }
    }

    val metaDataPath = new Path(rootPath, HadoopMetaDataCatalog.METADATA_DIR)
    // Ensure path exists.
    if(!fs.exists(metaDataPath)) {
      fs.mkdirs(rootPath)
    } else {
      if(!fs.isDirectory(rootPath)) {
        sys.error(s"Catalog path $rootPath does not exist on file system ${fs.getUri}")
      }
    }

    val metaDataCatalog = new HadoopMetaDataCatalog(sc, metaDataPath)
    new HadoopCatalog(sc, metaDataCatalog, rootPath)
  }

  def writeSplits[K: HadoopWritable](splits: Array[K], raster: Path, conf: Configuration): Unit = {
    val keyWritable = implicitly[HadoopWritable[K]]
    val splitFile = new Path(raster, SPLITS_FILE)
    HdfsUtils.writeArray[K](splitFile, conf, splits)
  }

  def readSplits[K: HadoopWritable: ClassTag](raster: Path, conf: Configuration): Array[K] = {
    val keyWritable = implicitly[HadoopWritable[K]]
    val splitFile = new Path(raster, SPLITS_FILE)
    HdfsUtils.readArray[K](splitFile, conf)
  }
}
