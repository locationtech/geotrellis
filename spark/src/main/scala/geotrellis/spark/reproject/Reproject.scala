package geotrellis.spark.reproject

import geotrellis.proj4._
import geotrellis.raster._
import geotrellis.raster.reproject._
import geotrellis.spark._
import geotrellis.spark.ingest._
import geotrellis.vector._
import org.apache.spark.rdd._

import scala.reflect.ClassTag

object Reproject {
  def apply[T: IngestKey, TileType: ReprojectView](
      rdd: RDD[(T, TileType)],
      destCRS: CRS,
      options: ReprojectOptions): RDD[(T, TileType)] = {

    rdd.map { case (key, tile) =>
      val ProjectedExtent(extent, crs) = key.projectedExtent
      val Product2(newTile , newExtent) = tile.reproject(extent, crs, destCRS)
      val newKey = key.updateProjectedExtent(ProjectedExtent(newExtent, destCRS))
      (newKey, newTile)
    }
  }

  def apply[K: SpatialComponent: ClassTag](
      rdd: RasterRDD[K],
      destCRS: CRS,
      options: ReprojectOptions): RasterRDD[K] = {

    val crs = rdd.metaData.crs
    val mapTransform = rdd.metaData.layout.mapTransform

    val reprojectedTiles =
      rdd.map { case (key, tile) =>
        val extent = mapTransform(key)
        val Raster(newTile, newExtent) = tile.reproject(extent, crs, destCRS)
        ((key, newExtent), newTile)
      }

    val metadata =
      RasterMetaData.fromRdd(reprojectedTiles, destCRS, rdd.metaData.layout) { key => key._2 }

    val tiler: Tiler[(K, Extent), K, Tile] = {
      val getExtent = (inKey: (K, Extent)) => inKey._2
      val createKey = (inKey: (K, Extent), spatialComponent: SpatialKey) => inKey._1.updateSpatialComponent(spatialComponent)
      Tiler(getExtent, createKey)
    }

    new RasterRDD(tiler(reprojectedTiles, metadata, options.method), metadata)
  }

  def apply[K: SpatialComponent: ClassTag](
      rdd: MultiBandRasterRDD[K],
      destCRS: CRS,
      options: ReprojectOptions): MultiBandRasterRDD[K] = {

    val crs = rdd.metaData.crs
    val mapTransform = rdd.metaData.layout.mapTransform

    val reprojectedTiles =
      rdd.map { case (key, tile) =>
        val extent = mapTransform(key)
        val MultiBandRaster(newTile, newExtent) = tile.reproject(extent, crs, destCRS)
        ((key, newExtent), newTile)
      }

    val metadata =
      RasterMetaData.fromRdd(reprojectedTiles, destCRS, rdd.metaData.layout) { key => key._2 }

    val tiler: Tiler[(K, Extent), K, MultiBandTile] = {
      val getExtent = (inKey: (K, Extent)) => inKey._2
      val createKey = (inKey: (K, Extent), spatialComponent: SpatialKey) => inKey._1.updateSpatialComponent(spatialComponent)
      Tiler(getExtent, createKey) _
    }

    new MultiBandRasterRDD(tiler(reprojectedTiles, metadata, options.method), metadata)
  }

  def apply[K: SpatialComponent: ClassTag](rdd: RasterRDD[K], destCRS: CRS): RasterRDD[K] =
    apply(rdd, destCRS, ReprojectOptions.DEFAULT)

  def apply[T: IngestKey, TileType: ReprojectView](rdd: RDD[(T, TileType)], destCRS: CRS): RDD[(T, TileType)] =
    apply(rdd, destCRS, ReprojectOptions.DEFAULT)

  def apply[K: SpatialComponent: ClassTag](rdd: MultiBandRasterRDD[K], destCRS: CRS): MultiBandRasterRDD[K] =
    apply(rdd, destCRS, ReprojectOptions.DEFAULT)
}
