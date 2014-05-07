package geotrellis.spark.rdd
import geotrellis.spark.formats.TileIdZoomWritable
import geotrellis.spark.metadata.PyramidMetadata
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import java.io.ObjectOutputStream
import java.io.ObjectInputStream

class MultiLevelTileIdPartitioner extends org.apache.spark.Partitioner {
  @transient
  private var partitioners = Map[Int, TileIdPartitioner]()
  @transient
  private var offsets = Map[Int, Int]()

  override def getPartition(key: Any) = {
    val tzw = key.asInstanceOf[TileIdZoomWritable]
    partitioners(tzw.zoom).getPartition(tzw) + offsets(tzw.zoom)
  }
  
  def getPartitionForZoom(key: TileIdZoomWritable) = 
    partitioners(key.zoom).getPartition(key)

  override def numPartitions = partitioners.foldLeft(0)((count, entry) => count + entry._2.numPartitions)

  override def toString = {
    "MultiLevelTileIdPartitioner:\n" +
      "Split points: \n" +
      {
        for ((level, partitioner) <- partitioners)
          yield s"${level}: ${partitioner}\n"
      } +
      "Offsets: \n" +
      {
        for ((level, offset) <- offsets)
          yield s"${level}: ${offset}\n"
      }
  }

  override def equals(other: Any): Boolean =
    other match {
      case that: MultiLevelTileIdPartitioner => that.partitioners == partitioners && that.offsets == offsets
      case _                                 => false
    }

  override def hashCode: Int = 41 * partitioners.hashCode + offsets.hashCode

  private def writeObject(out: ObjectOutputStream) {
    out.defaultWriteObject()
    out.writeObject(partitioners)
    out.writeObject(offsets)
  }

  private def readObject(in: ObjectInputStream) {
    in.defaultReadObject()
    partitioners = in.readObject().asInstanceOf[Map[Int, TileIdPartitioner]]
    offsets = in.readObject().asInstanceOf[Map[Int, Int]]
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