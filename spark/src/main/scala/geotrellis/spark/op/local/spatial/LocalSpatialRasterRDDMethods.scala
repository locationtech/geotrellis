package geotrellis.spark.op.local.spatial

import geotrellis.raster.op.local._
import geotrellis.spark._
import geotrellis.vector.Geometry

abstract class LocalSpatialRasterRDDMethods[K: SpatialComponent] extends RasterRDDMethods[K] with Serializable {

  /** Masks this raster by the given Geometry. */
  def mask(geom: Geometry): RasterRDD[K] = {
    mask(Seq(geom))
  }

  /** Masks this raster by the given Geometry. */
  def mask(geoms: Iterable[Geometry]): RasterRDD[K] = {
    val mapTransform = rasterRDD.metaData.mapTransform
    rasterRDD.mapPairs { case (k, tile) =>
      val key = k.spatialComponent
      val result  = tile.mask(mapTransform(key), geoms)
      (k, result)
    }
  }

}
