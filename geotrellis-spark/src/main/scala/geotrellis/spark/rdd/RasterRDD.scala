package geotrellis.spark.rdd
import geotrellis.spark._
import geotrellis.spark.metadata.Context
import geotrellis.spark.op.local.AddOpMethods
import geotrellis.spark.op.local.DivideOpMethods
import geotrellis.spark.op.local.MultiplyOpMethods
import geotrellis.spark.op.local.SubtractOpMethods

import org.apache.spark.Partition
import org.apache.spark.TaskContext
import org.apache.spark.rdd.RDD

class RasterRDD(val prev: RDD[TileIdRaster], val opCtx: Context)
  extends RDD[TileIdRaster](prev)
  with AddOpMethods[RasterRDD]
  with SubtractOpMethods[RasterRDD]
  with MultiplyOpMethods[RasterRDD]
  with DivideOpMethods[RasterRDD] {

  override def getPartitions: Array[Partition] = firstParent.partitions

  override def compute(split: Partition, context: TaskContext) =
    firstParent.iterator(split, context)

}
