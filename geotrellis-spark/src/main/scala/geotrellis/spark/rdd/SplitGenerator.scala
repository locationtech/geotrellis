package geotrellis.spark.rdd

import geotrellis.spark.tiling.TileBounds
import geotrellis.spark.tiling.TmsTiling

trait SplitGenerator {
  def getSplits: Seq[Long]
}

object SplitGenerator {
  def EMPTY = new SplitGenerator { def getSplits = Seq() }
}

case class RasterSplitGenerator(
  tileBounds: TileBounds,
  zoom: Int,
  increment: Int = -1)
  extends SplitGenerator {
  // if increment is -1 getSplits return an empty sequence
  // also, we start with s+(i-1) as the first split point needs to be there, not at s
  def getSplits = for (i <- tileBounds.s+(increment-1) until tileBounds.n by increment) yield TmsTiling.tileId(tileBounds.e, i, zoom)
}

object RasterSplitGenerator {
  def apply(tileBounds: TileBounds, zoom: Int, tileSizeBytes: Int, blockSizeBytes: Long) = {
    new RasterSplitGenerator(tileBounds, zoom, computeIncrement(tileBounds, tileSizeBytes, blockSizeBytes))
  }
  // the goal is to come up with an increment such that tiles are partitioned roughly by hdfs block size
  // so if the block size is 64MB, we want each partition to have at most that much. 
  def computeIncrement(tileBounds: TileBounds, tileSizeBytes: Int, blockSizeBytes: Long) = {
    val tilesPerBlock = (blockSizeBytes / tileSizeBytes).toLong
    val tileCount = tileBounds.width * tileBounds.height

    // return -1 if it doesn't make sense to have splits, getSplits will handle this accordingly
    val increment =
      if (blockSizeBytes <= 0 || tilesPerBlock >= tileCount)
        -1
      else // assume compression ratio of 20%. If it is more than that, well, we'll have empty space in that block
        ((tilesPerBlock / tileBounds.width) * 1.2).toInt 
    increment
  }
}

