package geotrellis.spark.rdd
import geotrellis.spark._
import geotrellis.spark.metadata.PyramidMetadata
import org.apache.spark.Partition
import org.apache.spark.TaskContext
import org.apache.spark.rdd.RDD
import geotrellis.spark.op.local.AddOpMethods

/*class RasterRDD(val prev: RDD[TileIdRaster], val meta: PyramidMetadata) 
	extends RDD(prev)
	with AddOpMethods[RDD[TileIdRaster]]{

  override def getPartitions: Array[Partition] = firstParent.partitions

  override def compute(split: Partition, context: TaskContext) =
    firstParent.iterator(split, context)

}*/

trait RasterRDD[+Repr <: RDD[TileIdRaster]] extends AddOpMethods[RDD[TileIdRaster]] { self: Repr =>

  var meta: PyramidMetadata = _

}