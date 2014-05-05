package geotrellis.spark.rdd
import geotrellis.spark.formats.MultiLevelTileIdWritable
import geotrellis.spark.metadata.PyramidMetadata

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path

class MultiLevelTileIdPartitioner extends org.apache.spark.Partitioner {
  @transient
  private var partitioners = Map[Int, TileIdPartitioner]()
  @transient
  private var offsets = Map[Int, Int]()

  override def getPartition(key: Any) = {
    val mltw = key.asInstanceOf[MultiLevelTileIdWritable]
    partitioners(mltw.zoom).getPartition(mltw) + offsets(mltw.zoom)
  }
  override def numPartitions = partitioners.foldLeft(0)((count, entry) => count + entry._2.numPartitions)
  override def toString = {
    "MultiLevelTileIdPartitioner split points: \n" +
      {
        partitioners.map {
          case (level, partitioner) =>
            s"${level}: ${partitioner}\n"
        }
      }
  }
}

object MultiLevelTileIdPartitioner {
  def apply(splitGenerators: Map[Int, SplitGenerator], pyramid: Path, conf: Configuration): MultiLevelTileIdPartitioner = {
    val meta = PyramidMetadata(pyramid, conf)
    val mltp = new MultiLevelTileIdPartitioner

    mltp.partitioners =
      (for ((level, gen) <- splitGenerators)
        yield (level -> TileIdPartitioner(gen, new Path(pyramid, level.toString), conf))) ++
        Map(meta.maxZoomLevel -> TileIdPartitioner(new Path(pyramid, meta.maxZoomLevel.toString), conf)) // add base zoom level

    var cumOffset = 0
    mltp.offsets =
      (for ((level, partitioner) <- mltp.partitioners.toSeq.sortWith(_._1 > _._1)) yield {
        val curLevelOffset = cumOffset
        cumOffset = cumOffset + mltp.partitioners(level).numPartitions
        (level -> curLevelOffset)
      }).toMap
    mltp
  }

}