package geotrellis.spark

import geotrellis.raster.{Tile, MultiBandTile}
import geotrellis.spark.io.ContainerConstructor
import org.apache.spark.rdd.RDD
import spray.json.JsonFormat

import scala.reflect.ClassTag

class MultiBandRasterRDD[K: ClassTag](val tileRdd: RDD[(K, MultiBandTile)], val metaData: RasterMetaData)
  extends BoundRDD[K, MultiBandTile](tileRdd) {
  override val partitioner = tileRdd.partitioner

}

object MultiBandRasterRDD {
  implicit def implicitToRDD[K](rasterRdd: MultiBandRasterRDD[K]): RDD[(K, MultiBandTile)] = rasterRdd

  implicit def constructor[K: JsonFormat : ClassTag] =
    new ContainerConstructor[K, MultiBandTile, RasterMetaData, MultiBandRasterRDD[K]] {
      def getMetaData(raster: MultiBandRasterRDD[K]): RasterMetaData =
        raster.metaData

      def makeContainer(rdd: RDD[(K, MultiBandTile)], bounds: KeyBounds[K], metadata: RasterMetaData) =
        new MultiBandRasterRDD(rdd, metadata)
    }
}
