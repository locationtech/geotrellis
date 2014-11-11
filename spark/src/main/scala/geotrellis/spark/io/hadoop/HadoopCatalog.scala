package geotrellis.spark.io.hadoop

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.hadoop.formats._

import org.apache.hadoop.fs.Path
import org.apache.hadoop.io.SequenceFile
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat
import org.apache.hadoop.mapreduce.lib.output.MapFileOutputFormat
import org.apache.hadoop.mapreduce.Job
import org.apache.spark._
import org.apache.spark.SparkContext._
import org.apache.spark.rdd._
import scala.reflect._
import scala.util.{Failure, Success, Try}

/** Provides a catalog for storing rasters in any Hadoop supported file system.
  * 
  * The user provides a root path, for example, hdfs://localhost:8020/user/hadoop/geotrellis.
  * The meta data for all rasters will be stored under the 'metadata' directory by default, e.g. hdfs://localhost:8020/user/hadoop/geotrellis/metadata.
  * Layer data will be stored under the 'layers' folder, in a subfolder with that layer's name, and then one level deeper with the zoom level.
  * For instance, if we have a layer with name "NLCD" with zoom level 10, the path to the data might be
  * hdfs://localhost:8020/user/hadoop/geotrellis/layers/NLCD/10
  */
class HadoopCatalog private (sc: SparkContext, val metaDataCatalog: HadoopMetaDataCatalog, rootPath: Path, catalogConfig: HadoopCatalog.Config) 
    extends Catalog with Logging {
  type Params = Path
  type SupportedKey[K] = HadoopWritable[K]

  def paramsFor[K: SupportedKey: ClassTag](layerId: LayerId): Path =
    new Path(rootPath, catalogConfig.layerDataDir(layerId))

  private def writeSplits[K: HadoopWritable](splits: Array[K], raster: Path, conf: Configuration): Unit = {
    val keyWritable = implicitly[HadoopWritable[K]]
    val splitFile = new Path(raster, catalogConfig.splitsFile)
    HdfsUtils.writeArray[K](splitFile, conf, splits)
  }

  private def readSplits[K: HadoopWritable: ClassTag](raster: Path, conf: Configuration): Array[K] = {
    val keyWritable = implicitly[HadoopWritable[K]]
    val splitFile = new Path(raster, catalogConfig.splitsFile)
    HdfsUtils.readArray[K](splitFile, conf)
  }

  def load[K: HadoopWritable: ClassTag](metaData: LayerMetaData, path: Path, filters: FilterSet[K]): Try[RasterRDD[K]] =
    Try {
      val keyWritable = implicitly[HadoopWritable[K]]
      import keyWritable.implicits._

      val conf = sc.hadoopConfiguration

      val updatedConf =
        sc.hadoopConfiguration.withInputPath(path.suffix(catalogConfig.SEQFILE_GLOB))

      val writableRdd: RDD[(keyWritable.Writable, TileWritable)] =
        if(filters.isEmpty) {
          sc.newAPIHadoopRDD[keyWritable.Writable, TileWritable, SequenceFileInputFormat[keyWritable.Writable, TileWritable]](
            updatedConf,
            classOf[SequenceFileInputFormat[keyWritable.Writable, TileWritable]],
            classTag[keyWritable.Writable].runtimeClass.asInstanceOf[Class[keyWritable.Writable]], // ¯\_(ツ)_/¯
            classOf[TileWritable]
          )
        } else {
          val partitioner = KeyPartitioner[K](readSplits(path, conf))

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

  protected def save[K: HadoopWritable: ClassTag](rdd: RasterRDD[K], layerMetaData: LayerMetaData, path: Path, clobber: Boolean): Try[Unit] =
    Try {
      val keyWritable = implicitly[HadoopWritable[K]]
      import keyWritable.implicits._

      val conf = rdd.context.hadoopConfiguration
      val fs = path.getFileSystem(conf)
      
      if(fs.exists(path)) {
        if(clobber) {
          logDebug(s"Deleting $path")
          fs.delete(path, true)
        } else
          sys.error(s"Directory already exists: $path")
      }

      val job = new Job(conf)
      job.getConfiguration.set("io.map.index.interval", "1")
      SequenceFileOutputFormat.setOutputCompressionType(job, SequenceFile.CompressionType.RECORD)

      val pathString = path.toUri.toString

      logInfo(s"Saving RasterRDD to ${path}")

      // Figure out how many partitions there should be based on block size.
      val partitions = {
        val blockSize = fs.getDefaultBlockSize(path)
        val tileCount = rdd.count
        val tileSize = layerMetaData.rasterMetaData.tileLayout.tileSize * layerMetaData.rasterMetaData.cellType.bytes
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

      // Write the RDD.      
      sortedWritable
        .saveAsNewAPIHadoopFile(
          pathString,
          implicitly[ClassTag[keyWritable.Writable]].runtimeClass,
          classOf[TileWritable],
          classOf[MapFileOutputFormat],
          job.getConfiguration
        )

      writeSplits(splits, path, conf)
      
      logInfo(s"Finished saving tiles to ${path}")
    }
}

object HadoopCatalog {
  case class Config(
    /** Compression factor for determining how many tiles can fit into
      * one block on a Hadoop-readable file system. */
    compressionFactor: Double,

    /** Name of directory that will contain the metadata under the root path. */
    metaDataDir: String,

    /** Name of the directory that will contain the layer data under the root path */
    layerDir: String,

    /** Name of the splits file that contains the partitioner data */
    splitsFile: String,

    /** Creates a subdirectory path based on a layer id. */
    layerDataDir: LayerId => String,

    /** Creates a metadata file name based on a layer id. */
    metaDataFileName: LayerId => String
  ) {
    /** Sequence file data directory for reading data. 
      * Don't see a reason why the API would allow this to be modified
      */
    final val SEQFILE_GLOB = "/*[0-9]*/data"      
  }

  object Config {
    val DEFAULT = 
      Config(
        // Assume tiles can be compressed 30% (so, compressionFactor - 1)
        compressionFactor = 1.3,
        metaDataDir = "metadata",
        layerDir = "layers",
        splitsFile = "splits",
        layerDataDir = { layerId: LayerId => s"${layerId.name}/${layerId.zoom}" },
        metaDataFileName = { layerId: LayerId => s"${layerId.name}_${layerId.zoom}_metadata.json" }
      )
  }

  def apply(sc: SparkContext, rootPath: Path, catalogConfig: HadoopCatalog.Config = Config.DEFAULT): HadoopCatalog = {
    val fs = rootPath.getFileSystem(sc.hadoopConfiguration)

    // Ensure root directory exists.
    if(!fs.exists(rootPath)) {
      fs.mkdirs(rootPath)
    } else {
      if(!fs.isDirectory(rootPath)) {
        sys.error(s"Catalog path $rootPath does not exist on file system ${fs.getUri}")
      }
    }

    // Ensure path exists.
    val metaDataPath = new Path(rootPath, catalogConfig.metaDataDir)
    if(!fs.exists(metaDataPath)) {
      fs.mkdirs(rootPath)
    } else {
      if(!fs.isDirectory(rootPath)) {
        sys.error(s"Catalog path $rootPath does not exist on file system ${fs.getUri}")
      }
    }

    val metaDataCatalog = new HadoopMetaDataCatalog(sc, metaDataPath, catalogConfig.metaDataFileName)
    new HadoopCatalog(sc, metaDataCatalog, rootPath, catalogConfig)
  }
}
