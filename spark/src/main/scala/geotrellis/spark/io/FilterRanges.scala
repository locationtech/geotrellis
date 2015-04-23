package geotrellis.spark.io

import geotrellis.spark._
import geotrellis.spark.io.index._
import geotrellis.raster.GridBounds

import scala.collection.mutable

import org.joda.time.DateTime

object FilterRanges {

  def spatial(
    filterSet: FilterSet[SpatialKey],
    keyBounds: KeyBounds[SpatialKey],
    keyIndex: KeyIndex[SpatialKey]
  ): Seq[(Long, Long)] = {
    var spaceFilters = filterSet.filtersWithKey[SpatialKey].map {
      case SpaceFilter(bounds) => Some(bounds)
      case _ => None
    }.flatten

    if(spaceFilters.isEmpty) {
      val minKey = keyBounds.minKey
      val maxKey = keyBounds.maxKey
      spaceFilters = Seq(GridBounds(minKey.col, minKey.row, maxKey.col, maxKey.row))
    }

    (for {
      bounds <- spaceFilters
    } yield {
      val p1 = SpatialKey(bounds.colMin, bounds.rowMin)
      val p2 = SpatialKey(bounds.colMax, bounds.rowMax)

      val i1 = keyIndex.toIndex(p1)
      val i2 = keyIndex.toIndex(p2)

      keyIndex.indexRanges((p1, p2))
    }).flatten
  }

  def spatiotemporal(
    filterSet: FilterSet[SpaceTimeKey],
    keyBounds: KeyBounds[SpaceTimeKey],
    keyIndex: KeyIndex[SpaceTimeKey]
  ): Seq[(Long, Long)] = {
    var spaceFilters = filterSet.filtersWithKey[SpatialKey].map {
      case SpaceFilter(bounds) => Some(bounds)
      case _ => None
    }.flatten
    if (spaceFilters.isEmpty) {
      val minKey = keyBounds.minKey.spatialKey
      val maxKey = keyBounds.maxKey.spatialKey
      spaceFilters = Seq(GridBounds(minKey.col, minKey.row, maxKey.col, maxKey.row))
    }

    var timeFilters = filterSet.filtersWithKey[SpaceTimeKey].map {
      case TimeFilter(start, end) => Some((start, end))
      case _ => None
    }.flatten
    if (timeFilters.isEmpty) {
      val minKey = keyBounds.minKey.temporalKey
      val maxKey = keyBounds.maxKey.temporalKey
      timeFilters = Seq((minKey.time, maxKey.time))
    }

    (for {
      bounds <- spaceFilters
      (timeStart, timeEnd) <- timeFilters
    } yield {
      val p1 = SpaceTimeKey(bounds.colMin, bounds.rowMin, timeStart)
      val p2 = SpaceTimeKey(bounds.colMax, bounds.rowMax, timeEnd)

      val i1 = keyIndex.toIndex(p1)
      val i2 = keyIndex.toIndex(p2)

      keyIndex.indexRanges((p1, p2))
    }).flatten
  }
}
