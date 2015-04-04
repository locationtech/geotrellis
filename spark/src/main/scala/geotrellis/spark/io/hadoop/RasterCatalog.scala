package geotrellis.spark.io.hadoop

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.hadoop.formats._
import geotrellis.spark.op.stats._
import geotrellis.raster._
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.hadoop.io.SequenceFile
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat
import org.apache.hadoop.mapreduce.lib.output.{MapFileOutputFormat, SequenceFileOutputFormat}
import org.apache.hadoop.mapreduce.{JobContext, Job}
import org.apache.spark._
import org.apache.spark.rdd._
import org.apache.spark.SparkContext._
import scala.reflect._

case class RasterCatalogConfig(
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

object RasterCatalogConfig {
  val DEFAULT =
    RasterCatalogConfig(
      compressionFactor = 1.3, // Assume tiles can be compressed 30% (so, compressionFactor - 1)
      splitsFile = "splits",
      metaDataFileName = "metadata.json",
      layerDataDir = { layerId: LayerId => s"${layerId.name}/${layerId.zoom}" }
    )
}


object RasterCatalog {
  def apply(rootPath: Path,
            paramsConfig: DefaultParams[String] = BaseParams,
            catalogConfig: RasterCatalogConfig = RasterCatalogConfig.DEFAULT)(implicit sc: SparkContext): RasterCatalog = {
    HdfsUtils.ensurePathExists(rootPath, sc.hadoopConfiguration)
    val metaDataCatalog = new HadoopLayerMetaDataCatalog(sc.hadoopConfiguration, rootPath, catalogConfig.metaDataFileName)
    new RasterCatalog(metaDataCatalog, rootPath, paramsConfig, catalogConfig)
  }

  lazy val BaseParams = new DefaultParams[String](Map.empty.withDefaultValue(""), Map.empty)
}

class RasterCatalog(
    val metaDataCatalog: Store[LayerId, HadoopLayerMetaData],
    rootPath: Path,
    paramsConfig: DefaultParams[String],
    catalogConfig: RasterCatalogConfig)(implicit sc: SparkContext) {
  def defaultPath[K: ClassTag](id: LayerId) = {
    val subDir = paramsConfig.paramsFor[K](id).getOrElse("")
    if (subDir == "")
      new Path(rootPath, catalogConfig.layerDataDir(id))
    else
      new Path(new Path(rootPath, subDir), catalogConfig.layerDataDir(id))
  }


  def reader[K: RasterRDDReaderProvider](): RasterRDDReader[K] = 
    new RasterRDDReader[K] {
      def read(layerId: LayerId): RasterRDD[K] = {
        val metaData = metaDataCatalog.read(layerId)
        implicitly[RasterRDDReaderProvider[K]].reader(catalogConfig, metaData).read(layerId)
      }
    }
  
  def writer[K: RasterRDDWriterProvider: ClassTag](): Writer[LayerId, RasterRDD[K]] =
    writer[K](clobber = true)

  def writer[K: RasterRDDWriterProvider: ClassTag](clobber: Boolean): Writer[LayerId, RasterRDD[K]] =
    new Writer[LayerId, RasterRDD[K]] {
      def write(layerId: LayerId, rdd: RasterRDD[K]): Unit = {
        val layerPath = defaultPath[K](layerId)
        val rddWriter = implicitly[RasterRDDWriterProvider[K]].writer(catalogConfig, layerPath, clobber)
        val md = HadoopLayerMetaData(layerId, rdd.metaData, layerPath)

        rddWriter.write(layerId, rdd)

        // Write metadata afer raster, since writing the raster could clobber the directory
        metaDataCatalog.write(layerId, md)
      }
    }

  def writer[K: RasterRDDWriterProvider: ClassTag](layerPath: String): Writer[LayerId, RasterRDD[K]] =
    writer[K](layerPath, clobber = true)

  def writer[K: RasterRDDWriterProvider: ClassTag](layerPath: String, clobber: Boolean): Writer[LayerId, RasterRDD[K]] =
    writer[K](new Path(layerPath), clobber)

  def writer[K: RasterRDDWriterProvider: ClassTag](layerPath: Path): Writer[LayerId, RasterRDD[K]] =
    writer[K](layerPath, clobber = true)

  def writer[K: RasterRDDWriterProvider: ClassTag](layerPath: Path, clobber: Boolean): Writer[LayerId, RasterRDD[K]] = {
    val rddWriter = implicitly[RasterRDDWriterProvider[K]].writer(catalogConfig, layerPath, clobber)

    new Writer[LayerId, RasterRDD[K]] {
      def write(layerId: LayerId, rdd: RasterRDD[K]): Unit = {
        val md = HadoopLayerMetaData(layerId, rdd.metaData, layerPath)

        rddWriter.write(layerId, rdd)

        // Write metadata afer raster, since writing the raster could clobber the directory
        metaDataCatalog.write(layerId, md)
      }
    }
  }

  def tileReader[K: TileReaderProvider](layerId: LayerId): Reader[K, Tile] = {
    val layerMetaData = metaDataCatalog.read(layerId)
    implicitly[TileReaderProvider[K]].reader(layerMetaData)
  }
}
