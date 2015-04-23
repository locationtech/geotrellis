package geotrellis.spark.io.index.hilbert

import geotrellis.spark._
import geotrellis.spark.io.index.KeyIndex

import com.google.uzaygezen.core.CompactHilbertCurve
import com.google.uzaygezen.core.MultiDimensionalSpec
import com.google.uzaygezen.core.BitVector
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
import com.google.common.collect.ImmutableList

import scala.collection.JavaConversions._
import spire.syntax.cfor._

object HilbertSpatialKeyIndex {
  def apply(minKey: SpatialKey, maxKey: SpatialKey, spatialResolution: Int): HilbertSpatialKeyIndex =
    apply(new KeyBounds(minKey, maxKey), spatialResolution)

  def apply(keyBounds: KeyBounds[SpatialKey], spatialResolution: Int): HilbertSpatialKeyIndex =
    apply(keyBounds, spatialResolution, spatialResolution)

  def apply(keyBounds: KeyBounds[SpatialKey], xResolution: Int, yResolution: Int): HilbertSpatialKeyIndex =
    new HilbertSpatialKeyIndex(keyBounds, xResolution, yResolution)
}

class HilbertSpatialKeyIndex(keyBounds: KeyBounds[SpatialKey], xResolution: Int, yResolution: Int) extends KeyIndex[SpatialKey] {
  val chc = {
    val dimensionSpec =
      new MultiDimensionalSpec( 
        List(
          math.pow(2, xResolution).toInt,
          math.pow(2, yResolution).toInt
        ).map(new java.lang.Integer(_)) 
      )

    new CompactHilbertCurve(dimensionSpec)
  }

  def toIndex(key: SpatialKey): Long = {
    val bitVectors = 
      Array(
        BitVectorFactories.OPTIMAL.apply(xResolution),
        BitVectorFactories.OPTIMAL.apply(yResolution)
      )

    bitVectors(0).copyFrom(key.col.toLong)
    bitVectors(1).copyFrom(key.row.toLong)

    val hilbertBitVector = BitVectorFactories.OPTIMAL.apply(chc.getSpec.sumBitsPerDimension)

    chc.index(bitVectors, 0, hilbertBitVector)

    hilbertBitVector.toExactLong
  }

  def indexRanges(keyRange: (SpatialKey, SpatialKey)): Seq[(Long, Long)] = {

    val ranges: java.util.List[LongRange] = 
      List(
        LongRange.of(keyRange._1.col, keyRange._2.col),
        LongRange.of(keyRange._1.row, keyRange._2.row)
      )

    val  regionInspector: RegionInspector[LongRange, LongContent] = 
      SimpleRegionInspector.create(
        List(ranges),
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
        Int.MaxValue, //the maxFilteredIndexRanges should be positive, not 0
        true,
        LongRangeHome.INSTANCE,
        new LongContent(0L)
      )

    chc.accept(new ZoomingSpaceVisitorAdapter(chc, queryBuilder))
    val filteredIndexRanges = queryBuilder.get.getFilteredIndexRanges
    val size = filteredIndexRanges.size
    val result = Array.ofDim[(Long, Long)](size)

    cfor(0)(_ < size, _ + 1) { i =>
      val range = filteredIndexRanges.get(i)
      result(i) = (range.getIndexRange.getStart, range.getIndexRange.getEnd)
    }

    result
  }
}
