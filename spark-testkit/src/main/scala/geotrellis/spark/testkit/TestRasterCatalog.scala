package geotrellis.spark.testkit

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.json._
import geotrellis.spark.io.index._
import org.apache.spark._
import geotrellis.raster.io.geotiff.reader._
import geotrellis.vector._
import geotrellis.proj4._
import geotrellis.spark.tiling._
import geotrellis.raster.mosaic._
import scala.reflect._

object TestRasterCatalog {
  val defaultCrs = LatLng
  val defaultLayoutSceheme = ZoomedLayoutScheme(tileSize = 256)

  /** Create RasterRDD from single band GeoTiff */
  def createRasterRDD(path: String)(implicit sc: SparkContext): RasterRDD[SpatialKey] = {
    val tif = GeoTiffReader.readSingleBand(path)
    createRasterRDD(tif.tile, tif.extent, tif.crs)
  }

  def createRasterRDD(tile: Tile, extent: Extent, crs: CRS = LatLng, layoutScheme: LayoutScheme = ZoomedLayoutScheme(tileSize = 256))
  (implicit sc: SparkContext): RasterRDD[SpatialKey] = {
    val layoutLevel: LayoutLevel = layoutScheme.levelFor(crs.worldExtent, CellSize(extent, tile.cols, tile.rows))
    createRasterRDD(tile, extent, crs, layoutLevel.tileLayout) 
  }
  
  def createRasterRDD(tile: Tile, extent: Extent, tileLayout: TileLayout)(implicit sc: SparkContext): RasterRDD[SpatialKey] = {
    createRasterRDD(tile, extent, defaultCrs, tileLayout)
  }

  def createRasterRDD(tile: Tile, extent: Extent, crs: CRS, tileLayout: TileLayout)(implicit sc: SparkContext): RasterRDD[SpatialKey] = {
    val worldExtent = crs.worldExtent

    val outputMapTransform = new MapKeyTransform(worldExtent, tileLayout.layoutCols, tileLayout.layoutRows)
    val metaData = RasterMetaData(tile.cellType, extent, crs, tileLayout)

    val tmsTiles = 
      for {
        (col, row) <- outputMapTransform(extent).coords // iterate over all tiles we should have for input extent in the world layout
      } yield {
        val key = SpatialKey(col, row)
        val worldTileExtent = outputMapTransform(key)
        val outTile = ArrayTile.empty(tile.cellType, tileLayout.tileCols, tileLayout.tileRows)       
        outTile.merge(worldTileExtent, extent, tile)
        key -> outTile
      }

    asRasterRDD(metaData) {
      sc.parallelize(tmsTiles)
    }
  }
}

class TestRasterCatalog(val attributeStore: TestAttributeStore)(implicit sc: SparkContext) extends AttributeCaching[TestLayerMetaData] {
  private var data = Map.empty[LayerId, Array[(T, Tile)] forSome { type T}]

  def read[K: Boundable: ClassTag](layerId: LayerId, rasterQuery: RasterRDDQuery[K]): RasterRDD[K] = {
    try {
      val metadata  = getLayerMetadata(layerId)
      val keyBounds = getLayerKeyBounds(layerId)
      val index     = getLayerKeyIndex(layerId)

      val queryBounds = rasterQuery(metadata.rasterMetaData, keyBounds)
      val tiles = 
        data(layerId)
          .asInstanceOf[Array[(K, Tile)]]
          .filter{ tup => queryBounds.includeKey(tup._1) }
      new RasterRDD[K](sc.parallelize(tiles), metadata.rasterMetaData)
    } catch {
      case e: AttributeNotFoundError => throw new LayerNotFoundError(layerId)
    }    
  }

  def read[K: Boundable: ClassTag](layerId: LayerId): RasterRDD[K] =
    read(layerId, new RasterRDDQuery[K])  

  def query[K: Boundable: ClassTag](layerId: LayerId): BoundRasterRDDQuery[K] ={
    new BoundRasterRDDQuery[K](new RasterRDDQuery[K], read(layerId, _))
  }

  def writer[K: SpatialComponent: Boundable: ClassTag](keyIndexMethod: KeyIndexMethod[K], clobber: Boolean): Writer[LayerId, RasterRDD[K]] = 
    new Writer[LayerId, RasterRDD[K]] {
      def write(layerId: LayerId, rdd: RasterRDD[K]): Unit = {
         rdd.persist()
         
         val md = TestLayerMetaData(
            layerId = layerId,
            keyClass = classTag[K].toString,
            rasterMetaData = rdd.metaData)
         val keyBounds = implicitly[Boundable[K]].getKeyBounds(rdd)
         val index = {
           val indexKeyBounds = {
             val imin = keyBounds.minKey.updateSpatialComponent(SpatialKey(0, 0))
             val imax = keyBounds.maxKey.updateSpatialComponent(SpatialKey(rdd.metaData.tileLayout.layoutCols - 1, rdd.metaData.tileLayout.layoutRows - 1))
             KeyBounds(imin, imax)
           }
           keyIndexMethod.createIndex(indexKeyBounds)
         }

         setLayerMetadata(layerId, md)
         setLayerKeyBounds(layerId, keyBounds)
         setLayerKeyIndex(layerId, index)

         data = data updated (layerId, rdd.collect)

         rdd.unpersist(blocking = false)
      }
    }

  def tileReader[K: ClassTag](layerId: LayerId): K => Tile = {
    (key: K) => {
      data(layerId)
        .find{ case (k, tile) => k.asInstanceOf[K] == key }
        .get
        ._2
    }
  }
}


case class TestLayerMetaData(
  layerId: LayerId,
  keyClass: String,
  rasterMetaData: RasterMetaData
)