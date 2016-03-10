package geotrellis.spark.ingest

import geotrellis.proj4._
import geotrellis.raster._
import geotrellis.raster.resample.{NearestNeighbor, ResampleMethod}
import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.spark.reproject._
import geotrellis.vector._

import org.apache.spark.Partitioner
import org.apache.spark.rdd._
import org.apache.spark.storage.StorageLevel
import scala.reflect.ClassTag

object MultibandIngest {
  def apply[T: ClassTag: ? => TilerKeyMethods[T, K]: Component[?, ProjectedExtent], K: SpatialComponent: Boundable: ClassTag](
    sourceTiles: RDD[(T, MultibandTile)],
    destCRS: CRS,
    layoutScheme: LayoutScheme,
    pyramid: Boolean = false,
    cacheLevel: StorageLevel = StorageLevel.NONE,
    resampleMethod: ResampleMethod = NearestNeighbor,
    partitioner: Option[Partitioner] = None,
    bufferSize: Option[Int] = None)
    (sink: (MultibandRasterRDD[K], Int) => Unit): Unit =
  {
    val (_, rasterMetadata) = RasterMetadata.fromRdd(sourceTiles, layoutScheme)
    val tiledRdd = sourceTiles.tileToLayout(rasterMetadata, resampleMethod).cache()
    val contextRdd = new ContextRDD(tiledRdd, rasterMetadata)
    val (zoom, rasterRdd) = bufferSize.fold(contextRdd.reproject(destCRS, layoutScheme))(contextRdd.reproject(destCRS, layoutScheme, _))
    rasterRdd.persist(cacheLevel)

    def buildPyramid(zoom: Int, rdd: MultibandRasterRDD[K]): List[(Int, MultibandRasterRDD[K])] = {
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
