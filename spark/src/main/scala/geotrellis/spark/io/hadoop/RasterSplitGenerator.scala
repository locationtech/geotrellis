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

package geotrellis.spark.io.hadoop

import geotrellis.raster._
import geotrellis.spark.tiling._

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
  gridBounds: GridBounds,
  indexGridTransform: IndexGridTransform,
  increment: (Int, Int))
  extends SplitGenerator {
  // if increment is -1 splits return an empty sequence
  // also, we start with s+(i-1) as the first split point needs to be there, not at s    
  def splits: Array[Long] = {
    val (xi, yi) = increment

    // we can't have a case where both x and y have non-trivial (i.e., neither -1 or 1) increments
    assert(!(xi > -1 && yi > 1))
    if (xi > -1) {
      val xl = (gridBounds.colMin + (xi - 1) to gridBounds.colMax by xi).toList
      val xr = if (gridBounds.width % xi == 0) xl else xl :+ gridBounds.colMax
      val splits = for (
        y <- gridBounds.rowMin to gridBounds.rowMax;
        x <- xr
      ) yield indexGridTransform.gridToIndex(x, y)
      splits.dropRight(1).toArray
    }
    else {
      val splits = 
        for (row <- gridBounds.rowMin + (yi - 1) until gridBounds.rowMax by yi)
        yield indexGridTransform.gridToIndex(gridBounds.colMax, row)
      splits.toArray
    }

  }

}

object RasterSplitGenerator {
  
  // assume tiles can be compressed 30% (so, compressionFactor - 1)
  final val CompressionFactor = 1.3
  
  def apply(gridBounds: GridBounds, indexGridTransform: IndexGridTransform, tileSizeBytes: Int, blockSizeBytes: Long) = {
    val increment = computeIncrement(gridBounds, tileSizeBytes, blockSizeBytes)
    new RasterSplitGenerator(gridBounds, indexGridTransform, increment)
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
  def computeIncrement(gridBounds: GridBounds, tileSizeBytes: Int, blockSizeBytes: Long) = {
    val tilesPerBlock = (blockSizeBytes / tileSizeBytes).toInt
    val tileCount = gridBounds.width * gridBounds.height
       
    // return -1 if it doesn't make sense to have splits, splits will handle this accordingly
    val increment =
      if (blockSizeBytes <= 0 || tilesPerBlock >= tileCount)
        (-1, -1)
      else if (gridBounds.width > tilesPerBlock)
        ((tilesPerBlock * CompressionFactor).toInt, 1)
      else
        (-1, ((tilesPerBlock / gridBounds.width) * CompressionFactor).toInt)

    increment
  }
}
