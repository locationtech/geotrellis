package geotrellis.spark.ingest

import geotrellis.spark._
import geotrellis.raster._
import geotrellis.raster.reproject._
import geotrellis.vector._
import geotrellis.proj4._
import org.apache.spark.rdd._

import monocle.syntax._

import scala.reflect.ClassTag

object Reproject {
  def reproject[T: IngestKey](rdd: RDD[(T, Tile)], destCRS: CRS): RDD[(T, Tile)] = {
    rdd.map { case (key, tile) =>
      val ProjectedExtent(extent, crs) = key.projectedExtent
      val (newTile, newExtent) = tile.reproject(extent, crs, destCRS)
      val newKey = key.updateProjectedExtent(ProjectedExtent(newExtent, destCRS))
      (newKey, newTile)
    }
  }

  def reproject[K: SpatialComponent: ClassTag](rdd: RasterRDD[K], destCRS: CRS): RasterRDD[K] = {
    val bcMetaData = rdd.sparkContext.broadcast(rdd.metaData)

    val reprojectedTiles = 
      rdd.map { case (key, tile) =>
        val metaData = bcMetaData.value
        val crs = metaData.crs
        val mapTransform = metaData.mapTransform

        val extent = mapTransform(key)
        val (newTile, newExtent) = tile.reproject(extent, crs, destCRS)
        ((key, newExtent), newTile)
      }

    val metaData = 
      RasterMetaData.fromRdd(reprojectedTiles, destCRS, rdd.metaData.tileLayout) { key => key._2 }

    val tiler = 
      new Tiler[(K, Extent), K] {
        def getExtent(inKey: (K, Extent)): Extent = inKey._2
        def createKey(inKey: (K, Extent), spatialComponent: SpatialKey): K = inKey._1.updateSpatialComponent(spatialComponent)
      }

    tiler.tile(reprojectedTiles, metaData)
  }
}
