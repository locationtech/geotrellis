package geotrellis.spark

import geotrellis.raster._

import org.apache.spark.rdd._

package object rdd {
  def asRasterRDD[I](metaData: LayerMetaData)(f: =>RDD[TileTup[I]]): RasterRDD[I] =
    new RasterRDD(f, metaData)

  def asTmsRasterRDD(metaData: LayerMetaData)(f: =>RDD[TmsTile]): TmsRasterRDD =
    new TmsRasterRDD(f, metaData)

  implicit class MakeRasterRDD(val prev: RDD[TmsTile]) {
    def toRasterRDD(metaData: LayerMetaData) = new TmsRasterRDD(prev, metaData)
  }

  implicit class MakeRasterRDD2(val prev: RDD[(Long, Tile)]) {
    def prevAsTmsTiles = 
      prev.mapPartitions({ seq => seq.map { case (id, tile) => TmsTile(id, tile) } }, true)
    def toRasterRDD(metaData: LayerMetaData) = new TmsRasterRDD(prevAsTmsTiles, metaData)
  }
}
