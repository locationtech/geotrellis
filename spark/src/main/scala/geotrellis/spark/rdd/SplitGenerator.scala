/*
 * Copyright (c) 2014 DigitalGlobe.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
  def splits: Array[Long]
}

object SplitGenerator {
  def EMPTY = new SplitGenerator { def splits = Array() }
}

case class RasterSplitGenerator(
  tileExtent: TileExtent,
  zoom: Int,
  increment: (Int, Int))
  extends SplitGenerator {
  // if increment is -1 splits return an empty sequence
  // also, we start with s+(i-1) as the first split point needs to be there, not at s    
  def splits: Array[Long] = {
    val (xi, yi) = increment

    // we can't have a case where both x and y have non-trivial (i.e., neither -1 or 1) increments
    assert(!(xi > -1 && yi > 1))
    if (xi > -1) {
      val xl = (tileExtent.xmin + (xi - 1) to tileExtent.xmax by xi).toList
      val xr = if (tileExtent.width % xi == 0) xl else xl :+ tileExtent.xmax
      val splits = for (
        y <- tileExtent.ymin to tileExtent.ymax;
        x <- xr
      ) yield TmsTiling.tileId(x, y, zoom)
      splits.dropRight(1).toArray
    }
    else {
      val splits = 
        for (y <- tileExtent.ymin + (yi - 1) until tileExtent.ymax by yi)
        yield TmsTiling.tileId(tileExtent.xmax, y, zoom)
      splits.toArray
    }

  }

}

object RasterSplitGenerator {
  
  // assume tiles can be compressed 30% (so, compressionFactor - 1)
  final val CompressionFactor = 1.3
  
  def apply(tileExtent: TileExtent, zoom: Int, tileSizeBytes: Int, blockSizeBytes: Long) = {
    new RasterSplitGenerator(tileExtent, zoom, computeIncrement(tileExtent, tileSizeBytes, blockSizeBytes))
  }

  /*
   * This method computes the number of tiles that can fit onto a single HDFS block. 
   * The return value is a Tuple2 [xInc, yInc] where xInc is the run of x tiles that can fit 
   * in a block and yInc is the number of rows of tiles that can fit in a block. If xInc > -1 
   * then yInc can't be > 1. 
   * 
   * The rules are:
   * 1. If the total number of tiles is less than that can fit onto a block, we return [-1,-1]. 
   * This means no splits would be generated. 
   * 2. If the row is too wide for a single hdfs block, we return [X, 1] where X is estimated
   * using tilesPerBlock and compression factor.   
   * 3. Otherwise, we figure how many roles of tiles can fit onto a block, so we return [-1, Y]
   * using tilesPerBlock and compression factor.
   */
  def computeIncrement(tileExtent: TileExtent, tileSizeBytes: Int, blockSizeBytes: Long) = {
    val tilesPerBlock = (blockSizeBytes / tileSizeBytes).toInt
    val tileCount = tileExtent.width * tileExtent.height
       
    // return -1 if it doesn't make sense to have splits, splits will handle this accordingly
    val increment =
      if (blockSizeBytes <= 0 || tilesPerBlock >= tileCount)
        (-1, -1)
      else if (tileExtent.width > tilesPerBlock)
        ((tilesPerBlock * CompressionFactor).toInt, 1)
      else
        (-1, ((tilesPerBlock / tileExtent.width) * CompressionFactor).toInt)

    increment
  }
}
