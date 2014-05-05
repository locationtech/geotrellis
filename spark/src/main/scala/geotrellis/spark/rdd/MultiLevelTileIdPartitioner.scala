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
        for((level, partitioner) <- partitioners) 
          yield s"${level}: ${partitioner}\n"        
      }
  }
}

object MultiLevelTileIdPartitioner {

  /*
   * Create a multi-level TileIdPartitioner over levels 1 through n, given the split generator for 
   * those levels and a path to the pyramid. The code will overwrite the splits file for each level
   * in the generator. 
   * 
   */
  def apply(splitGenerators: Map[Int, SplitGenerator],
            pyramid: Path,
            conf: Configuration): MultiLevelTileIdPartitioner = {
    val mltp = new MultiLevelTileIdPartitioner

    // input validation
    val keys = splitGenerators.keys.toList
    require(keys.distinct.length == keys.length) // ensure keys are all distinct

    mltp.partitioners =
      for ((level, gen) <- splitGenerators)
        yield (level -> TileIdPartitioner(gen, new Path(pyramid, level.toString), conf))

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