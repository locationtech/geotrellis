package geotrellis.spark

import geotrellis.raster._

import org.apache.spark.rdd._

package object rdd {
  def asRasterRDD(metaData: LayerMetaData)(f: =>RDD[TmsTile]): RasterRDD =
    new RasterRDD(f, metaData)

  implicit class MakeRasterRDD(val prev: RDD[TmsTile]) {
    def toRasterRDD(metaData: LayerMetaData) = new RasterRDD(prev, metaData)
  }

  implicit class MakeRasterRDD2(val prev: RDD[(Long, Tile)]) {
    def toRasterRDD(metaData: LayerMetaData) = new RasterRDD(prev, metaData)
  }
}
