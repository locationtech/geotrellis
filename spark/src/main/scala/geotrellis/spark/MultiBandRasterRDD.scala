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
  implicit def constructor[K: JsonFormat : ClassTag] =
    new ContainerConstructor[K, MultiBandTile, MultiBandRasterRDD[K]] {
      type MetaDataType = RasterMetaData
      implicit def metaDataFormat = geotrellis.spark.io.json.RasterMetaDataFormat

      def getMetaData(raster: MultiBandRasterRDD[K]): RasterMetaData =
        raster.metaData

      def makeContainer(rdd: RDD[(K, MultiBandTile)], bounds: KeyBounds[K], metadata: MetaDataType) =
        new MultiBandRasterRDD(rdd, metadata)

    }
}