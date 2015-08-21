package geotrellis.spark

import geotrellis.raster.MultiBandTile
import org.apache.spark.rdd.RDD

import scala.reflect.ClassTag

class MultiBandRasterRDD[K: ClassTag](val tileRdd: RDD[(K, MultiBandTile)], val metaData: RasterMetaData) extends BoundRDD[K, MultiBandTile](tileRdd) {
  override val partitioner = tileRdd.partitioner

}
