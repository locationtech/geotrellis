package geotrellis.spark

import geotrellis.raster._
import geotrellis.vector.Extent

import org.apache.spark.rdd._

package object rdd {
  /** Keeps with the convention while still using simple tups, nice */
  implicit class TileTuple[K](tup: (K, Tile)) {
    def id: K = tup._1
    def tile: Tile = tup._2
  }

  def asRasterRDD[K](metaData: LayerMetaData)(f: =>RDD[(K, Tile)]): RasterRDD[K] =
    new RasterRDD(f, metaData)

  implicit class MakeRasterRDD(val prev: RDD[TmsTile]) {
    def toRasterRDD(metaData: LayerMetaData) = new RasterRDD[TileId](prev, metaData)
  }

  implicit class MakeRasterRDD2(val prev: RDD[(Long, Tile)]) {
    def prevAsTmsTiles = 
      prev.mapPartitions({ seq => seq.map { case (id, tile) => TmsTile(id, tile) } }, true)
    def toRasterRDD(metaData: LayerMetaData) = new RasterRDD[TileId](prevAsTmsTiles, metaData)
  }
}
