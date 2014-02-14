package geotrellis.spark.rdd
import geotrellis.spark._
import geotrellis.spark.metadata.PyramidMetadata
import org.apache.spark.Partition
import org.apache.spark.TaskContext
import org.apache.spark.rdd.RDD
import geotrellis.spark.op.local.AddOpMethods

class RasterRDD(val prev: RDD[TileIdRaster], val meta: PyramidMetadata) 
	extends RDD[TileIdRaster](prev) 
	with AddOpMethods[RasterRDD] {

  override def getPartitions: Array[Partition] = firstParent.partitions

  override def compute(split: Partition, context: TaskContext) =
    firstParent.iterator(split, context)

}
