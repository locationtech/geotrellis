package geotrellis.spark.io.hadoop

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.index._
import geotrellis.spark.io.json._
import org.apache.hadoop.fs.Path
import org.apache.spark._
import spray.json._

import scala.reflect._

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
) extends AttributeCaching[HadoopLayerMetaData] {

  def read[K: RasterRDDReader: Boundable: JsonFormat: ClassTag](layerId: LayerId, query: RasterRDDQuery[K]): RasterRDD[K] = {
    try {
      val metadata  = getLayerMetadata(layerId)
      val keyBounds = getLayerKeyBounds(layerId)                
      val index     = getLayerKeyIndex(layerId)

      implicitly[RasterRDDReader[K]]
        .read(catalogConfig, metadata, index, keyBounds)(layerId, query(metadata.rasterMetaData, keyBounds))
    } catch {
      case e: AttributeNotFoundError => throw new LayerNotFoundError(layerId)
    }
  }

  def read[K: RasterRDDReader: Boundable: JsonFormat: ClassTag](layerId: LayerId): RasterRDD[K] =
    query[K](layerId).toRDD

  def query[K: RasterRDDReader: Boundable: JsonFormat: ClassTag](layerId: LayerId): BoundRasterRDDQuery[K] =
    new BoundRasterRDDQuery[K](new RasterRDDQuery[K], read(layerId, _))

  def writer[K: RasterRDDWriter: Boundable:Ordering: JsonFormat: SpatialComponent: ClassTag](keyIndexMethod: KeyIndexMethod[K]): Writer[LayerId, RasterRDD[K]] =
    writer[K](keyIndexMethod, "")

  def writer[K: RasterRDDWriter: Boundable:Ordering: JsonFormat: SpatialComponent: ClassTag](keyIndexMethod: KeyIndexMethod[K], clobber: Boolean): Writer[LayerId, RasterRDD[K]] =
    writer(keyIndexMethod, "", clobber)

  def writer[K: RasterRDDWriter: Boundable:Ordering: JsonFormat: SpatialComponent: ClassTag](keyIndexMethod: KeyIndexMethod[K], subDir: Path): Writer[LayerId, RasterRDD[K]] =
    writer[K](keyIndexMethod, subDir.toString)

  def writer[K: RasterRDDWriter: Boundable:Ordering: JsonFormat: SpatialComponent: ClassTag](keyIndexMethod: KeyIndexMethod[K], subDir: Path, clobber: Boolean): Writer[LayerId, RasterRDD[K]] =
    writer[K](keyIndexMethod, subDir.toString, clobber)

  def writer[K: RasterRDDWriter: Boundable:Ordering: JsonFormat: SpatialComponent: ClassTag](keyIndexMethod: KeyIndexMethod[K], subDir: String): Writer[LayerId, RasterRDD[K]] =
    writer[K](keyIndexMethod, subDir, clobber = true)

  def writer[K: RasterRDDWriter: Boundable: Ordering: JsonFormat: SpatialComponent: ClassTag](keyIndexMethod: KeyIndexMethod[K], subDir: String, clobber: Boolean): Writer[LayerId, RasterRDD[K]] =
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

        val keyBounds = implicitly[Boundable[K]].getKeyBounds(rdd)

        val keyIndex = {
          val indexKeyBounds = {
            val imin = keyBounds.minKey.updateSpatialComponent(SpatialKey(0, 0))
            val imax = keyBounds.maxKey.updateSpatialComponent(SpatialKey(rdd.metaData.tileLayout.layoutCols - 1, rdd.metaData.tileLayout.layoutRows - 1))
            KeyBounds(imin, imax)
          }
          keyIndexMethod.createIndex(indexKeyBounds)
        }

        val rddWriter = implicitly[RasterRDDWriter[K]]
        rddWriter.write(catalogConfig, md, keyIndex, clobber)(layerId, rdd)

        setLayerMetadata(layerId, md)
        setLayerKeyBounds(layerId, keyBounds)
        setLayerKeyIndex(layerId, keyIndex)

        rdd.unpersist(blocking = false)
      }
    }

  def tileReader[K: Boundable: JsonFormat: TileReader: ClassTag](layerId: LayerId): Reader[K, Tile] = {
    // TODO: There should be a way to do this with a Reader, not touching any InputFormats
    val metadata  = getLayerMetadata(layerId)
    val keyBounds = getLayerKeyBounds(layerId)                
    val index     = getLayerKeyIndex(layerId)
    val boundable = implicitly[Boundable[K]]
    
    val readTile = (key: K) => {
      val tileKeyBounds = KeyBounds(key, key)
      boundable.intersect(tileKeyBounds, keyBounds) match {
        case Some(kb) =>
          try {
            implicitly[TileReader[K]].read(catalogConfig, metadata, index, kb)
          } catch {
            case e: UnsupportedOperationException => throw new TileNotFoundError(key, layerId)
          }          
        case None => 
          throw new TileNotFoundError(key, layerId)
      }
    }
    
    new Reader[K, Tile] {
      def read(key: K) = readTile(key)
    }
  }
}
