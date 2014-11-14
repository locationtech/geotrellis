package geotrellis.spark.io.hadoop

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.hadoop.formats._
import geotrellis.spark.utils.KryoClosure

import org.apache.hadoop.fs.Path
import org.apache.hadoop.io.SequenceFile
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat
import org.apache.hadoop.mapreduce.lib.output.MapFileOutputFormat
import org.apache.hadoop.mapreduce.{JobContext, Job}
import org.apache.spark._
import org.apache.spark.SparkContext._
import org.apache.spark.rdd._
import scala.reflect._
import scala.util.{Failure, Success, Try}

/** Provides a catalog for storing rasters in any Hadoop supported file system.
  * 
  * The user provides a root path, for example, hdfs://localhost:8020/user/hadoop/geotrellis.
  * The metadata for each raster will be stored in the same folder as the tiles.
  * By default layers will be stored with a folder of the layer name and subfolder of layer zoom.
  * For instance, if we have a layer with name "NLCD" with zoom level 10, the path to the data might be
  * hdfs://localhost:8020/user/hadoop/geotrellis/NLCD/10
  *
  * By providing Params argument, you may specify a subfolder to use in the catalogRoot.
  * For instance, providing Params of "us/2011" will instead save the above layer to:
  * hdfs://localhost:8020/user/hadoop/geotrellis/us/2011/NLCD/10
  *
  * If Params have been provided on save they must be used on load for the layer to be found.
  * To get more sophisticated folder hierarchy please use paramsConfig which will allow you to define
  * default subfolders to be used on both loading and saving based on both on raster Key and LayerId.
  */
class HadoopCatalog private (
    sc: SparkContext,
    val metaDataCatalog: HadoopMetaDataCatalog,
    rootPath: Path,
    paramsConfig: DefaultParams[String],
    catalogConfig: HadoopCatalog.Config)
  extends Catalog with Logging {

  type Params = String // subdirectory under the rootPath, ex: "nlcd/2011"
  type SupportedKey[K] = HadoopWritable[K]

  def paramsFor[K: HadoopWritable: ClassTag](id: LayerId): String =
    paramsConfig.paramsFor[K](id).getOrElse("")

  def pathFor(id: LayerId, subDir: String) =
    if (subDir == "")
      new Path(rootPath, catalogConfig.layerDataDir(id))
    else
      new Path(new Path(rootPath, subDir), catalogConfig.layerDataDir(id))

  private def writeSplits[K: HadoopWritable](splits: Array[K], raster: Path, conf: Configuration): Unit = {
    val splitFile = new Path(raster, catalogConfig.splitsFile)
    HdfsUtils.writeArray[K](splitFile, conf, splits)
  }

  private def readSplits[K: HadoopWritable: ClassTag](raster: Path, conf: Configuration): Array[K] = {
    val splitFile = new Path(raster, catalogConfig.splitsFile)
    HdfsUtils.readArray[K](splitFile, conf)
  }

  def load[K: HadoopWritable : ClassTag](id: LayerId, metaData: RasterMetaData, subDir: String, filters: FilterSet[K]): Try[RasterRDD[K]] =
    Try {
      val keyWritable = implicitly[HadoopWritable[K]]
      import keyWritable.implicits._

      val path = pathFor(id, subDir)
      val dataPath = path.suffix(catalogConfig.SEQFILE_GLOB)

      logDebug(s"Loading $id from $dataPath")

      val conf = sc.hadoopConfiguration
      val inputConf = sc.hadoopConfiguration.withInputPath(path.suffix(catalogConfig.SEQFILE_GLOB))

      val writableRdd: RDD[(keyWritable.Writable, TileWritable)] =
        if(filters.isEmpty) {
          sc.newAPIHadoopRDD[keyWritable.Writable, TileWritable, SequenceFileInputFormat[keyWritable.Writable, TileWritable]](
            inputConf,
            classOf[SequenceFileInputFormat[keyWritable.Writable, TileWritable]],
            classTag[keyWritable.Writable].runtimeClass.asInstanceOf[Class[keyWritable.Writable]], // ¯\_(ツ)_/¯
            classOf[TileWritable]
          )
        } else {
          val partitioner = KeyPartitioner[K](readSplits(path, conf))

          val _filters = sc.broadcast(filters)
          val includeKey: keyWritable.Writable => Boolean =
          { writable =>
            _filters.value.includeKey(keyWritable.toValue(writable))
          }

          val includePartition: Partition => Boolean =
          { partition =>
            val minKey = partitioner.minKey(partition.index)
            val maxKey = partitioner.maxKey(partition.index)

            _filters.value.includePartition(minKey, maxKey)
          }

          new PreFilteredHadoopRDD[keyWritable.Writable, TileWritable](
            sc,
            classOf[SequenceFileInputFormat[keyWritable.Writable, TileWritable]],
            classTag[keyWritable.Writable].runtimeClass.asInstanceOf[Class[keyWritable.Writable]],
            classOf[TileWritable],
            inputConf
          )(includePartition)(includeKey)
        }
      val md = metaData // else $outer will refer to HadoopCatalog
      asRasterRDD(md) {
        writableRdd
          .map  { case (keyWritable, tileWritable) =>
            (keyWritable.toValue, tileWritable.toTile(md))
        }
      }

    }

  def save[K: HadoopWritable : ClassTag](id: LayerId, subDir: String, rdd: RasterRDD[K], clobber: Boolean): Try[Unit] =
    Try {
      val keyWritable = implicitly[HadoopWritable[K]]
      import keyWritable.implicits._

      val conf = rdd.context.hadoopConfiguration
      val path: Path = pathFor(id, subDir)
      val fs = rootPath.getFileSystem(sc.hadoopConfiguration)
      logDebug(s"Exists: $path, ${fs.exists(path)}")
      if(fs.exists(path)) {
        if(clobber) {
          logDebug(s"Deleting $path")
          fs.delete(path, true)
        } else
          sys.error(s"Directory already exists: $path")
      }

      val job = Job.getInstance(conf)
      job.getConfiguration.set("io.map.index.interval", "1")
      SequenceFileOutputFormat.setOutputCompressionType(job, SequenceFile.CompressionType.RECORD)

      logInfo(s"Saving RasterRDD for $id to ${path}")

      // Figure out how many partitions there should be based on block size.
      val partitions = {
        val blockSize = fs.getDefaultBlockSize(path)
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
          path.toUri.toString,
          implicitly[ClassTag[keyWritable.Writable]].runtimeClass,
          classOf[TileWritable],
          classOf[MapFileOutputFormat],
          job.getConfiguration
        )

      writeSplits(splits, path, conf)
      logInfo(s"Finished saving tiles to ${path}")
      metaDataCatalog.save(id, subDir, rdd.metaData, clobber)
    }
}

object HadoopCatalog {
  case class Config(
    /** Compression factor for determining how many tiles can fit into
      * one block on a Hadoop-readable file system. */
    compressionFactor: Double,

    /** Name of the splits file that contains the partitioner data */
    splitsFile: String,

    /** Name of file that will contain the metadata under the layer path. */
    metaDataFileName: String,

    /** Creates a subdirectory path based on a layer id. */
    layerDataDir: LayerId => String
  ) {
    /** Sequence file data directory for reading data.
      * Don't see a reason why the API would allow this to be modified
      */
    final val SEQFILE_GLOB = "/*[0-9]*/data"
  }



  object Config {
    val DEFAULT = 
      Config(
        compressionFactor = 1.3, // Assume tiles can be compressed 30% (so, compressionFactor - 1)
        splitsFile = "splits",
        metaDataFileName = "metadata.json",
        layerDataDir = { layerId: LayerId => s"${layerId.name}/${layerId.zoom}" }
      )
}
  /** Use val as base in Builder pattern to make your own table mappings. */
  val BaseParams = new DefaultParams[String](Map.empty.withDefaultValue(""), Map.empty)

  def apply(sc: SparkContext, rootPath: Path,
            paramsConfig: DefaultParams[String] = HadoopCatalog.BaseParams,
            catalogConfig: HadoopCatalog.Config = Config.DEFAULT): HadoopCatalog = {
    HdfsUtils.ensurePathExists(rootPath, sc.hadoopConfiguration)
    val metaDataCatalog = new HadoopMetaDataCatalog(sc, rootPath, catalogConfig.layerDataDir, catalogConfig.metaDataFileName)
    new HadoopCatalog(sc, metaDataCatalog, rootPath, paramsConfig, catalogConfig)
  }
}
