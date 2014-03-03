package geotrellis.spark.rdd

import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.spark.metadata.Context
import geotrellis.spark.op.local.AddOpMethods
import geotrellis.spark.op.local.DivideOpMethods
import geotrellis.spark.op.local.MultiplyOpMethods
import geotrellis.spark.op.local.SubtractOpMethods
import geotrellis.spark.formats.ArgWritable
import geotrellis.spark.formats.TileIdWritable

import org.apache.spark.Partition
import org.apache.spark.TaskContext
import org.apache.spark.rdd.RDD

class RasterRDD(val prev: RDD[Tile], val opCtx: Context)
  extends RDD[Tile](prev)
  with AddOpMethods[RasterRDD]
  with SubtractOpMethods[RasterRDD]
  with MultiplyOpMethods[RasterRDD]
  with DivideOpMethods[RasterRDD] {

  override def getPartitions: Array[Partition] = firstParent.partitions

  override def compute(split: Partition, context: TaskContext) =
    firstParent.iterator(split, context)

  def toWritable = 
    mapPartitions({ partition =>
      partition.map { tile =>
        (TileIdWritable(tile.id), ArgWritable.fromRasterData(tile.raster.data))
      }
    }, true)

  def mapTiles(f: Tile => Tile): RasterRDD =
    mapPartitions({ partition =>
      partition.map { tile =>
        f(tile)
      }
    }, true)
    .withContext(opCtx)

  def combineTiles(other: RasterRDD)(f: (Tile,Tile) => Tile): RasterRDD =
    zipPartitions(other, true) { (partition1, partition2) =>
      partition1.zip(partition2).map { case (tile1, tile2) =>
        f(tile1, tile2)
      }
    }
    .withContext(opCtx)
}

object RasterRDD {
  implicit def hadoopRddToRasterRDD(rhrdd: RasterHadoopRDD): RasterRDD =
    rhrdd.toRasterRDD
}
