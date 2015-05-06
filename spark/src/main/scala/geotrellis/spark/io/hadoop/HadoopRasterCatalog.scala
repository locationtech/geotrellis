package geotrellis.spark.io.hadoop

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.json._
import geotrellis.spark.io.hadoop.formats._
import geotrellis.spark.io.index._
import geotrellis.spark.utils._
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
import spray.json._

case class HadoopRasterCatalogConfig(
  /** Compression factor for determining how many tiles can fit into
    * one block on a Hadoop-readable file system. */
  compressionFactor: Double,

  /** Name of the splits file that contains the partitioner data */
  splitsFile: String,

  /** Name of file that will contain the metadata under the layer path. */
  metaDataFileName: String,

  /** Name of the subdirectory under the catalog root that will hold the attributes. */
  attributeDir: String,

  /** Creates a subdirectory path based on a layer id. */
  layerDataDir: LayerId => String
) {
  /** Sequence file data directory for reading data.
    * Don't see a reason why the API would allow this to be modified
    */
  final val SEQFILE_GLOB = "/*[0-9]*/data"
}

object HadoopRasterCatalogConfig {
  val DEFAULT =
    HadoopRasterCatalogConfig(
      compressionFactor = 1.3, // Assume tiles can be compressed 30% (so, compressionFactor - 1)
      splitsFile = "splits",
      metaDataFileName = "metadata.json",
      attributeDir = "attributes",
      layerDataDir = { layerId: LayerId => s"${layerId.name}/${layerId.zoom}" }
    )
}


object HadoopRasterCatalog {
  def apply(
    rootPath: Path,
    catalogConfig: HadoopRasterCatalogConfig = HadoopRasterCatalogConfig.DEFAULT)(implicit sc: SparkContext
  ): HadoopRasterCatalog = {
    HdfsUtils.ensurePathExists(rootPath, sc.hadoopConfiguration)
    val attributeStore = new HadoopAttributeStore(sc.hadoopConfiguration, new Path(rootPath, catalogConfig.attributeDir))
    new HadoopRasterCatalog(rootPath, attributeStore, catalogConfig)
  }
}

class HadoopRasterCatalog(
  rootPath: Path,
  val attributeStore: HadoopAttributeStore,
  catalogConfig: HadoopRasterCatalogConfig)(implicit sc: SparkContext
) {

  def reader[K: RasterRDDReader: JsonFormat: ClassTag](): FilterableRasterRDDReader[K] =
    new FilterableRasterRDDReader[K] {
      def read(layerId: LayerId, filterSet: FilterSet[K]): RasterRDD[K] = {
        val metaData = attributeStore.read[HadoopLayerMetaData](layerId, "metadata")
        val keyBounds = attributeStore.read[KeyBounds[K]](layerId, "keyBounds")                
        val index = attributeStore.read[KeyIndex[K]](layerId, "keyIndex")
        implicitly[RasterRDDReader[K]]
          .read(catalogConfig, metaData, index, keyBounds)(layerId, filterSet)
      }
    }

  def writer[K: RasterRDDWriter: Ordering: JsonFormat: SpatialComponent: ClassTag](keyIndexMethod: KeyIndexMethod[K]): Writer[LayerId, RasterRDD[K]] =
    writer[K](keyIndexMethod, "")

  def writer[K: RasterRDDWriter: Ordering: JsonFormat: SpatialComponent: ClassTag](keyIndexMethod: KeyIndexMethod[K], clobber: Boolean): Writer[LayerId, RasterRDD[K]] =
    writer(keyIndexMethod, "", clobber)

  def writer[K: RasterRDDWriter: Ordering: JsonFormat: SpatialComponent: ClassTag](keyIndexMethod: KeyIndexMethod[K], subDir: Path): Writer[LayerId, RasterRDD[K]] =
    writer[K](keyIndexMethod, subDir.toString)

  def writer[K: RasterRDDWriter: Ordering: JsonFormat: SpatialComponent: ClassTag](keyIndexMethod: KeyIndexMethod[K], subDir: Path, clobber: Boolean): Writer[LayerId, RasterRDD[K]] =
    writer[K](keyIndexMethod, subDir.toString, clobber)

  def writer[K: RasterRDDWriter: Ordering: JsonFormat: SpatialComponent: ClassTag](keyIndexMethod: KeyIndexMethod[K], subDir: String): Writer[LayerId, RasterRDD[K]] =
    writer[K](keyIndexMethod, subDir, clobber = true)

  def writer[K: RasterRDDWriter: Ordering: JsonFormat: SpatialComponent: ClassTag](keyIndexMethod: KeyIndexMethod[K], subDir: String, clobber: Boolean): Writer[LayerId, RasterRDD[K]] =
    new Writer[LayerId, RasterRDD[K]] {
      def write(layerId: LayerId, rdd: RasterRDD[K]): Unit = {
        rdd.persist()

        val layerPath = 
          if (subDir == "")
            new Path(rootPath, catalogConfig.layerDataDir(layerId))
          else
            new Path(new Path(rootPath, subDir), catalogConfig.layerDataDir(layerId))
              val md = HadoopLayerMetaData(
        
          keyClass = classTag[K].toString, 
          rasterMetaData = rdd.metaData, 
          path = layerPath)

        val minKey = rdd.map(_._1).min
        val maxKey = rdd.map(_._1).max
        val keyBounds = KeyBounds(minKey, maxKey)

        val keyIndex = {
          val indexKeyBounds = {
            val imin = minKey.updateSpatialComponent(SpatialKey(0, 0))
            val imax = maxKey.updateSpatialComponent(SpatialKey(rdd.metaData.tileLayout.layoutCols - 1, rdd.metaData.tileLayout.layoutRows - 1))
            KeyBounds(imin, imax)
          }
          keyIndexMethod.createIndex(indexKeyBounds)
        }

        val rddWriter = implicitly[RasterRDDWriter[K]]
          .write(catalogConfig, md, keyIndex, clobber)(layerId, rdd)

        attributeStore.write(layerId, "keyIndex", keyIndex)
        attributeStore.write(layerId, "keyBounds", keyBounds)
        attributeStore.write(layerId, "metadata", md)

        rdd.unpersist(blocking = false)
      }
    }

  def readTile[K: JsonFormat: TileReader: ClassTag](layerId: LayerId): Reader[K, Tile] = 
    new Reader[K, Tile] {
      val readTile = {
        val layerMetaData = attributeStore.read[HadoopLayerMetaData](layerId, "metadata")
        val keyBounds = attributeStore.read[KeyBounds[K]](layerId, "keyBounds")
        val index = attributeStore.read[KeyIndex[K]](layerId, "keyIndex")
        implicitly[TileReader[K]].read(catalogConfig, layerMetaData, index)(_)
      }

      def read(key: K) = readTile(key)
    }
}
