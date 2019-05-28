/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.layers.index.hilbert

import geotrellis.tiling.{SpatialKey, KeyBounds}
import geotrellis.layers._
import geotrellis.layers.index.KeyIndex

import com.google.uzaygezen.core.CompactHilbertCurve
import com.google.uzaygezen.core.MultiDimensionalSpec
import com.google.uzaygezen.core.BitVectorFactories
import com.google.uzaygezen.core.BacktrackingQueryBuilder
import com.google.uzaygezen.core.RegionInspector
import com.google.uzaygezen.core.SimpleRegionInspector
import com.google.uzaygezen.core.LongContent
import com.google.uzaygezen.core.PlainFilterCombiner
import com.google.uzaygezen.core.ZoomingSpaceVisitorAdapter
import com.google.uzaygezen.core.ranges.LongRange
import com.google.uzaygezen.core.ranges.LongRangeHome
import com.google.common.base.Functions

import scala.collection.JavaConverters._
import spire.syntax.cfor._

object HilbertSpatialKeyIndex {
  def apply(minKey: SpatialKey, maxKey: SpatialKey, spatialResolution: Int): HilbertSpatialKeyIndex =
    apply(new KeyBounds(minKey, maxKey), spatialResolution)

  def apply(keyBounds: KeyBounds[SpatialKey], spatialResolution: Int): HilbertSpatialKeyIndex =
    apply(keyBounds, spatialResolution, spatialResolution)

  def apply(keyBounds: KeyBounds[SpatialKey], xResolution: Int, yResolution: Int): HilbertSpatialKeyIndex =
    new HilbertSpatialKeyIndex(keyBounds, xResolution, yResolution)
}

class HilbertSpatialKeyIndex(val keyBounds: KeyBounds[SpatialKey], val xResolution: Int, val yResolution: Int) extends KeyIndex[SpatialKey] {
  val minKey = keyBounds.minKey

  @transient lazy val chc = {
    val dimensionSpec = new MultiDimensionalSpec(List(xResolution, yResolution).map(new java.lang.Integer(_)).asJava)
    new CompactHilbertCurve(dimensionSpec)
  }

  def toIndex(key: SpatialKey): BigInt = {
    val bitVectors =
      Array(
        BitVectorFactories.OPTIMAL.apply(xResolution),
        BitVectorFactories.OPTIMAL.apply(yResolution)
      )

    val col = key.col - minKey.col
    val row = key.row - minKey.row
    bitVectors(0).copyFrom(col.toLong)
    bitVectors(1).copyFrom(row.toLong)

    val hilbertBitVector = BitVectorFactories.OPTIMAL.apply(chc.getSpec.sumBitsPerDimension)

    chc.index(bitVectors, 0, hilbertBitVector)

    hilbertBitVector.toExactLong
  }

  def indexRanges(keyRange: (SpatialKey, SpatialKey)): Seq[(BigInt, BigInt)] = {
    val minCol = keyBounds.minKey.col
    val minRow = keyBounds.minKey.row

    val ranges: java.util.List[LongRange] =
      List( //LongRange is exclusive on upper bound, adjusting for it here with + 1
        LongRange.of(keyRange._1.col - minCol, keyRange._2.col - minCol + 1),
        LongRange.of(keyRange._1.row - minRow, keyRange._2.row - minRow + 1)
      ).asJava

    val  regionInspector: RegionInspector[LongRange, LongContent] =
      SimpleRegionInspector.create(
        List(ranges).asJava,
        new LongContent(1),
        Functions.identity[LongRange](),
        LongRangeHome.INSTANCE,
        new LongContent(0L)
      )

    val combiner =
      new PlainFilterCombiner[LongRange, java.lang.Long, LongContent, LongRange](LongRange.of(0, 1));

    val queryBuilder =
      BacktrackingQueryBuilder.create(
        regionInspector,
        combiner,
        Int.MaxValue,
        true,
        LongRangeHome.INSTANCE,
        new LongContent(0L)
      )

    chc.accept(new ZoomingSpaceVisitorAdapter(chc, queryBuilder))
    val filteredIndexRanges = queryBuilder.get.getFilteredIndexRanges
    val size = filteredIndexRanges.size
    val result = Array.ofDim[(BigInt, BigInt)](size)

    cfor(0)(_ < size, _ + 1) { i =>
      val range = filteredIndexRanges.get(i)
      //LongRange is exclusive on upper bound, adjusting for it here with - 1
      result(i) = (BigInt(range.getIndexRange.getStart), BigInt(range.getIndexRange.getEnd) - 1)
    }

    result
  }
}
