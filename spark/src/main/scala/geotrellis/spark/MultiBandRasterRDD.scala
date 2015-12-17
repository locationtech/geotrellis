package geotrellis.spark

import geotrellis.raster.{Tile, MultiBandTile}
import geotrellis.spark.io.Bridge
import org.apache.spark.rdd.RDD
import spray.json.JsonFormat

import scala.reflect.ClassTag

class MultiBandRasterRDD[K: ClassTag](val tileRdd: RDD[(K, MultiBandTile)], val metaData: RasterMetaData)
  extends BoundRDD[K, MultiBandTile](tileRdd) {
  override val partitioner = tileRdd.partitioner

}

object MultiBandRasterRDD {
  implicit def implicitToRDD[K](rasterRdd: MultiBandRasterRDD[K]): RDD[(K, MultiBandTile)] = rasterRdd

  implicit def bridge[K: JsonFormat : ClassTag] =
    new Bridge[(RDD[(K, MultiBandTile)], RasterMetaData), MultiBandRasterRDD[K]] {
      def unapply(b: MultiBandRasterRDD[K]) = (b.tileRdd, b.metaData)

      def apply(a: (RDD[(K, MultiBandTile)], RasterMetaData)) = new MultiBandRasterRDD(a._1, a._2)
    }
}
