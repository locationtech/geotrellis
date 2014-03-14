/** ************************************************************************
 *  Copyright (c) 2014 DigitalGlobe.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  ************************************************************************
 */

package geotrellis.spark.rdd

import geotrellis.spark.tiling.TileExtent
import geotrellis.spark.tiling.TmsTiling

/*
 * SplitsGenerator provides an interface to derive split points with a default implementation
 * RasterSplitGenerator, which derives split points based on how many tiles can fit on a block.
 *
 */
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
    for (i <- tileExtent.ymin + (increment - 1) until tileExtent.ymax by increment)
      yield TmsTiling.tileId(tileExtent.xmax, i, zoom)
}

object RasterSplitGenerator {
  def apply(tileExtent: TileExtent, zoom: Int, tileSizeBytes: Int, blockSizeBytes: Long) = {
    new RasterSplitGenerator(tileExtent, zoom, computeIncrement(tileExtent, tileSizeBytes, blockSizeBytes))
  }

  /*
   * The partitioner tries to fit n rows of tiles to a split. The rules are:
   * 1. If the row is too wide for a single hdfs block, we return 1. 
   * This means each split may spill onto more than one hdfs block
   * 2. If the total number of tiles is less than that can fit onto a block, 
   * we return -1. This means no splits would be generated. 
   * 3. Otherwise, we try and maximize n such that each block will have at 
   * most that much. Here, we assume tiles get 0 compression ratio, so we are 
   * extremely conservative in figuring how many tiles can fit in a block.
   */
  def computeIncrement(tileExtent: TileExtent, tileSizeBytes: Int, blockSizeBytes: Long) = {
    val tilesPerBlock = (blockSizeBytes / tileSizeBytes).toLong
    val tileCount = tileExtent.width * tileExtent.height

    // return -1 if it doesn't make sense to have splits, getSplits will handle this accordingly
    val increment =
      if (blockSizeBytes <= 0 || tilesPerBlock >= tileCount)
        -1
      else if (tileExtent.width > tilesPerBlock)
        1
      else
        (tilesPerBlock / tileExtent.width).toInt

    increment
  }
}

