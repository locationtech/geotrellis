package geotrellis.spark.ingest

import geotrellis.raster.resample.{NearestNeighbor, ResampleMethod}
import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.spark.reproject._
import geotrellis.raster._
import geotrellis.proj4._
import org.apache.spark.rdd._
import org.apache.spark.storage.StorageLevel
import scala.reflect.ClassTag

object MultibandIngest{
  def apply[T: IngestKey: ClassTag: ? => TilerKeyMethods[T, K], K: SpatialComponent: ClassTag](
    sourceTiles: RDD[(T, MultibandTile)],
    destCRS: CRS,
    layoutScheme: LayoutScheme,
    pyramid: Boolean = false,
    cacheLevel: StorageLevel = StorageLevel.NONE,
    resampleMethod: ResampleMethod = NearestNeighbor)
    (sink: (MultibandRasterRDD[K], Int) => Unit): Unit =
  {
    val reprojectedTiles = sourceTiles.reproject(destCRS, resampleMethod).cache()
    val (zoom, rasterMetadata) =
      RasterMetadata.fromRdd(reprojectedTiles, destCRS, layoutScheme)(_.projectedExtent.extent)
    val tiledRdd = sourceTiles.cutTiles(rasterMetadata, resampleMethod).cache()
    val rasterRdd = new ContextRDD(tiledRdd, rasterMetadata)

    def buildPyramid(zoom: Int, rdd: MultibandRasterRDD[K]): List[(Int, MultibandRasterRDD[K])] = {
      if (zoom > 1) {
        rdd.persist(cacheLevel)
        sink(rdd, zoom)
        val pyramidLevel@(nextZoom, nextRdd) = Pyramid.up(rdd, layoutScheme, zoom)
        pyramidLevel :: buildPyramid(nextZoom, nextRdd)
      } else
        List((zoom, rdd))
    }

    if (pyramid)
      buildPyramid(zoom, rasterRdd)
        .foreach { case (z, rdd) => rdd.unpersist(true) }
    else
      sink(rasterRdd, zoom)

  }
}
