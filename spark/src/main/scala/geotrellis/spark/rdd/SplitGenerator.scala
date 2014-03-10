package geotrellis.spark.rdd

import geotrellis.spark.tiling.TileExtent
import geotrellis.spark.tiling.TmsTiling

trait SplitGenerator {
  def getSplits: Seq[Long]
}

object SplitGenerator {
  def EMPTY = new SplitGenerator { def getSplits = Seq() }
}

case class RasterSplitGenerator(
  tileExtent: TileExtent,
  zoom: Int,
  increment: Int = -1)
  extends SplitGenerator {
  // if increment is -1 getSplits return an empty sequence
  // also, we start with s+(i-1) as the first split point needs to be there, not at s
  def getSplits = 
    for (i <- tileExtent.ymin+(increment-1) until tileExtent.ymax by increment) 
      yield TmsTiling.tileId(tileExtent.xmax, i, zoom)
}

object RasterSplitGenerator {
  def apply(tileExtent: TileExtent, zoom: Int, tileSizeBytes: Int, blockSizeBytes: Long) = {
    new RasterSplitGenerator(tileExtent, zoom, computeIncrement(tileExtent, tileSizeBytes, blockSizeBytes))
  }
  // the goal is to come up with an increment such that tiles are partitioned roughly by hdfs block size
  // so if the block size is 64MB, we want each partition to have at most that much. 
  def computeIncrement(tileExtent: TileExtent, tileSizeBytes: Int, blockSizeBytes: Long) = {
    val tilesPerBlock = (blockSizeBytes / tileSizeBytes).toLong
    val tileCount = tileExtent.width * tileExtent.height

    // return -1 if it doesn't make sense to have splits, getSplits will handle this accordingly
    val increment =
      if (blockSizeBytes <= 0 || tilesPerBlock >= tileCount)
        -1
      else // assume compression ratio of 20%. If it is more than that, well, we'll have empty space in that block
        ((tilesPerBlock / tileExtent.width) * 1.2).toInt 
    increment
  }
}

