package geotrellis.spark.io.hadoop

import geotrellis.spark._
import geotrellis.raster._

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

import com.github.nscala_time.time.Imports._

trait KeyIndex[K] extends Serializable {
  def toIndex(key: K): Long
  def indexRanges(keyRange: (K, K)): Seq[(Long, Long)]
}

/** Represents a row major ordering for SpatialKey */
class RowMajorSpatialKeyIndex(layoutCols: Int) extends KeyIndex[SpatialKey] {
  def toIndex(key: SpatialKey): Long = 
    toIndex(key.col, key.row)

  def toIndex(col: Int, row: Int): Long =
    (layoutCols * row + col).toLong

  def indexRanges(keyRange: (SpatialKey, SpatialKey)): Seq[(Long, Long)] = {
    val SpatialKey(colMin, rowMin) = keyRange._1
    val SpatialKey(colMax, rowMax) = keyRange._2

    val cols = colMax - colMin + 1
    val rows = rowMax - rowMin

    val result = Array.ofDim[(Long, Long)](rowMax - rowMin + 1)

    cfor(0)(_ <= rows, _ + 1) { i =>
      val row = rowMin + i
      val min = toIndex(colMin, row)
      result(i) = (min, min + cols)
    }
    result
  }
}

class HilbertSpatialKeyIndex(minKey: SpatialKey, maxKey: SpatialKey, spatialResolution: Int) extends KeyIndex[SpatialKey] {
  val chc = {
    val dimensionSpec =
      new MultiDimensionalSpec( 
        List(
          math.pow(2, spatialResolution).toInt,
          math.pow(2, spatialResolution).toInt
        ).map(new java.lang.Integer(_)) 
      )

    new CompactHilbertCurve(dimensionSpec)
  }

  def toIndex(key: SpatialKey): Long = {
    val bitVectors = 
      Array(
        BitVectorFactories.OPTIMAL.apply(spatialResolution),
        BitVectorFactories.OPTIMAL.apply(spatialResolution)
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
        0,
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


class SpaceTimeKeyIndex(minKey: SpaceTimeKey, maxKey: SpaceTimeKey, spatialResolution: Int, temporalResolution: Int) extends KeyIndex[SpaceTimeKey] {
  val startMillis = minKey.temporalKey.time.getMillis
  val timeWidth = maxKey.temporalKey.time.getMillis - startMillis

  val chc = {
    val dimensionSpec =
      new MultiDimensionalSpec( 
        List(
          math.pow(2, spatialResolution).toInt,
          math.pow(2, spatialResolution).toInt,
          math.pow(2, temporalResolution).toInt
        ).map(new java.lang.Integer(_)) 
      )

    new CompactHilbertCurve(dimensionSpec)
  }

  def binTime(key: SpaceTimeKey): Long =
    ((key.temporalKey.time.getMillis - startMillis) * temporalResolution) / timeWidth

  def toIndex(key: SpaceTimeKey): Long = {
    val bitVectors = 
      Array(
        BitVectorFactories.OPTIMAL.apply(spatialResolution),
        BitVectorFactories.OPTIMAL.apply(spatialResolution),
        BitVectorFactories.OPTIMAL.apply(temporalResolution)
      )

    bitVectors(0).copyFrom(key.spatialKey.col.toLong)
    bitVectors(1).copyFrom(key.spatialKey.row.toLong)
    bitVectors(2).copyFrom(binTime(key))

    val hilbertBitVector = BitVectorFactories.OPTIMAL.apply(chc.getSpec.sumBitsPerDimension)

    chc.index(bitVectors, 0, hilbertBitVector)

    hilbertBitVector.toExactLong
  }

  def indexRanges(keyRange: (SpaceTimeKey, SpaceTimeKey)): Seq[(Long, Long)] = {

    val ranges: java.util.List[LongRange] = 
      List(
        LongRange.of(keyRange._1.spatialKey.col, keyRange._2.spatialKey.col),
        LongRange.of(keyRange._1.spatialKey.row, keyRange._2.spatialKey.row),
        LongRange.of(binTime(keyRange._1), binTime(keyRange._2))
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
        0,
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
