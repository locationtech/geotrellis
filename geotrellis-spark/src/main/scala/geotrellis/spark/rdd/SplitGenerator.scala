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
  def getSplits = for (i <- tileBounds.s until tileBounds.n by increment) yield TmsTiling.tileId(tileBounds.e, i, zoom)
}

object RasterSplitGenerator {
  def apply(tileBounds: TileBounds, zoom: Int, tileSizeBytes: Int, blockSizeBytes: Long) = {
    new RasterSplitGenerator(tileBounds, zoom, computeIncrement(tileBounds, tileSizeBytes, blockSizeBytes))
  }
  def computeIncrement(tileBounds: TileBounds, tileSizeBytes: Int, blockSizeBytes: Long) = {
    val tilesPerBlock = (blockSizeBytes / tileSizeBytes).toLong
    val tileCount = tileBounds.width * tileBounds.height

    // return -1 if it doesn't make sense to have splits, getSplits will handle this accordingly
    val increment =
      if (blockSizeBytes <= 0 || tilesPerBlock >= tileCount)
        -1
      else
        (tilesPerBlock / tileBounds.width).toInt
    increment
  }
}

