package geotrellis.spark.ingest

import geotrellis.raster.resample.{NearestNeighbor, ResampleMethod}
import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.spark.reproject._
import geotrellis.raster._
import geotrellis.proj4._
import org.apache.spark.Partitioner
import org.apache.spark.rdd._
import org.apache.spark.storage.StorageLevel
import scala.reflect.ClassTag

object MultiBandIngest {
  def apply[T: ProjectedExtentComponent: ClassTag: ? => TilerKeyMethods[T, K], K: SpatialComponent: ClassTag](
    sourceTiles: RDD[(T, MultiBandTile)],
    destCRS: CRS,
    layoutScheme: LayoutScheme,
    bufferSize: Int = 256 * 256,
    pyramid: Boolean = false,
    cacheLevel: StorageLevel = StorageLevel.NONE,
    resampleMethod: ResampleMethod = NearestNeighbor,
    partitioner: Option[Partitioner] = None)
    (sink: (MultiBandRasterRDD[K], Int) => Unit): Unit =
  {
    val (_, rasterMetaData) =
      RasterMetaData.fromRdd(sourceTiles, destCRS, layoutScheme)(_.projectedExtent.extent)
    val tiledRdd = sourceTiles.tileToLayout(rasterMetaData, resampleMethod).cache()
    val contextRdd = new ContextRDD(tiledRdd, rasterMetaData)
    val (zoom, rasterRdd) = contextRdd.reproject(destCRS, layoutScheme, bufferSize)
    rasterRdd.cache()

    def buildPyramid(zoom: Int, rdd: MultiBandRasterRDD[K]): List[(Int, MultiBandRasterRDD[K])] = {
      if (zoom >= 1) {
        rdd.persist(cacheLevel)
        sink(rdd, zoom)
        val pyramidLevel @ (nextZoom, nextRdd) = Pyramid.up(rdd, layoutScheme, zoom, partitioner)
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