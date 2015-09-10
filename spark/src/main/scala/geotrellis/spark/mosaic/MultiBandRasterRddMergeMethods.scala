package geotrellis.spark.mosaic

import geotrellis.spark._

class MultiBandRasterRddMergeMethods[K: SpatialComponent](rdd: MultiBandRasterRDD[K]) extends MergeMethods[MultiBandRasterRDD[K]] {
   def merge(other: MultiBandRasterRDD[K]) = {
     val (outRdd, _) = (rdd.tileRdd, rdd.metaData.layout).merge(other.tileRdd, rdd.metaData.layout)
     new MultiBandRasterRDD(outRdd, rdd.metaData)
   }
}
