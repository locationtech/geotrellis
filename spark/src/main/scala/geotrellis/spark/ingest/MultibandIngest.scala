package geotrellis.spark.ingest

import geotrellis.proj4._
import geotrellis.raster._
import geotrellis.raster.resample.{NearestNeighbor, ResampleMethod}
import geotrellis.spark._
import geotrellis.spark.pyramid._
import geotrellis.spark.reproject._
import geotrellis.spark.tiling._
import geotrellis.vector._
import geotrellis.util._

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
    bufferSize: Option[Int] = None,
    targetZoom: Option[Int] = None)
    (sink: (MultibandTileLayerRDD[K], Int) => Unit): Unit =
  {
    val (_, tileLayerMetadata) = sourceTiles.collectMetadata(layoutScheme)
    val tiledRdd = sourceTiles.tileToLayout(tileLayerMetadata, resampleMethod).cache()
    val contextRdd = new ContextRDD(tiledRdd, tileLayerMetadata)
    val (zoom, tileLayerRdd) = bufferSize.fold(contextRdd.reproject(destCRS, layoutScheme))(contextRdd.reproject(destCRS, layoutScheme, _))
    tileLayerRdd.persist(cacheLevel)

    def buildPyramidUp(zoom: Int, rdd: MultibandTileLayerRDD[K]): List[(Int, MultibandTileLayerRDD[K])] = {
      if (zoom >= 1) {
        rdd.persist(cacheLevel)
        sink(rdd, zoom)
        val pyramidLevel @ (nextZoom, nextRdd) = Pyramid.up(rdd, layoutScheme, zoom, partitioner)
        pyramidLevel :: buildPyramidUp(nextZoom, nextRdd)
      } else {
        sink(rdd, zoom)
        List((zoom, rdd))
      }
    }

    def buildPyramidDown(currentZoom: Int, stopZoom: Int, rdd: MultibandTileLayerRDD[K]): List[(Int, MultibandTileLayerRDD[K])] = {
      if (currentZoom >= 0 && currentZoom < stopZoom) {
        rdd.persist(cacheLevel)
        if(currentZoom != zoom) sink(rdd, currentZoom)
        val pyramidLevel @ (nextZoom, nextRdd) = Pyramid.down(rdd, layoutScheme, currentZoom, partitioner)
        pyramidLevel :: buildPyramidDown(nextZoom, stopZoom, nextRdd)
      } else {
        sink(rdd, currentZoom)
        List((currentZoom, rdd))
      }
    }

    if (pyramid) {
      buildPyramidUp(zoom, tileLayerRdd).foreach { case (z, rdd) => rdd.unpersist(true) }
      targetZoom.foreach { stopZoom =>
        if(stopZoom > zoom)
          buildPyramidDown(zoom, stopZoom, tileLayerRdd).foreach { case (z, rdd) => rdd.unpersist(true) }
      }
    } else sink(tileLayerRdd, zoom)

  }
}
