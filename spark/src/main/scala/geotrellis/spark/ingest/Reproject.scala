package geotrellis.spark.ingest

import geotrellis.spark._
import geotrellis.spark.utils._
import geotrellis.raster._
import geotrellis.raster.reproject._
import geotrellis.vector._
import geotrellis.proj4._
import org.apache.spark.rdd._

import monocle.syntax._

import scala.reflect.ClassTag

object Reproject {
  def apply[T: IngestKey](rdd: RDD[(T, Tile)], destCRS: CRS): RDD[(T, Tile)] = {
    rdd.map  {  { tup =>
      val (key, tile) = tup
      val ProjectedExtent(extent, crs) = key.projectedExtent
      val Raster(newTile, newExtent) = tile.reproject(extent, crs, destCRS)
      val newKey = key.updateProjectedExtent(ProjectedExtent(newExtent, destCRS))
      (newKey, newTile)
    } }
  }

  def apply[K: SpatialComponent: ClassTag](rdd: RasterRDD[K], destCRS: CRS): RasterRDD[K] = {
    val bcMetaData = rdd.sparkContext.broadcast(rdd.metaData)

    val reprojectedTiles = 
      rdd.map { case (key, tile) =>
        val metaData = bcMetaData.value
        val crs = metaData.crs
        val mapTransform = metaData.mapTransform

        val extent = mapTransform(key)
        val Raster(newTile, newExtent) = tile.reproject(extent, crs, destCRS)
        ((key, newExtent), newTile)
      }

    val metaData = 
      RasterMetaData.fromRdd(reprojectedTiles, destCRS, rdd.metaData.tileLayout) { key => key._2 }

    val tiler: Tiler[(K, Extent), K] = {
        val getExtent = (inKey: (K, Extent)) => inKey._2
        val createKey = (inKey: (K, Extent), spatialComponent: SpatialKey) => inKey._1.updateSpatialComponent(spatialComponent)
        Tiler(getExtent, createKey)
      }

    tiler(reprojectedTiles, metaData)
  }
}
