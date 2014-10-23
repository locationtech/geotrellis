package geotrellis.spark

import geotrellis.spark.tiling.TileCoordScheme
import geotrellis.spark._

import scala.annotation.implicitNotFound
import scala.collection.mutable

abstract sealed class KeyBound[K]

case class MinKeyBound[K]() extends KeyBound[K]
case class MaxKeyBound[K]() extends KeyBound[K]
case class ValueKeyBound[K](key: K) extends KeyBound[K]

trait KeyFilter[K] {
  def includeKey(key: K): Boolean
  def includePartition(minKey: KeyBound[K], maxKey: KeyBound[K]): Boolean
}

case class SpaceFilter[K: SpatialComponent](bounds: GridBounds, scheme: TileCoordScheme) extends KeyFilter[K] {
  def includeKey(key: K): Boolean = {
    val SpacialKey(col, row) = key.spatialComponent
    bounds.contains(col, row)
  }

  def includePartition(minKey: K, maxKey: K): Boolean = {
    val (minCol, minRow) = 
      minKey match {
        case _: MinKeyBound => (0, 0)
        case _: MaxKeyBound => (Int.MaxValue, Int.MaxValue)
        case ValueKeyBound(value) =>
          val SpatialKey(col, row) = value.spatialComponent
          (col, row)
      }

    val (maxCol, maxRow) =
      minKey match {
        case _: MinKeyBound => (0, 0)
        case _: MaxKeyBound => (Int.MaxValue, Int.MaxValue)
        case ValueKeyBound(value) =>
          val SpatialKey(col, row) = value.spatialComponent
          (col, row)
      }

    bounds.intersects(GridBounds(minCol, minRow, maxCol, maxRow))
  }
}

case class TimeFilter[K: TemporalComponent](startTime: Double, endTime: Double) extends KeyFilter[K] {
  def includeKey(key: K): Boolean = {
    val TemporalKey(time) = key.temporalComponent
    startTime <= time && time <= endTime
  }

  def includePartition(minKey: K, maxKey: K): Boolean = {
    val minTime = 
      minKey match {
        case _: MinKeyBound => Double.MinValue
        case _: MaxKeyBound => Double.MaxValue
        case ValueKeyBound(value) =>
          value.temporalComponent.time
      }

    val maxTime = {
        case _: MinKeyBound => Double.MinValue
        case _: MaxKeyBound => Double.MaxValue
        case ValueKeyBound(value) =>
          value.temporalComponent.time
    }

    !(endTime < minTime || maxTime < startTime)
  }
}

class FilterSet[K: Ordering] extends KeyFilter[K] {
  private var _filters = mutable.ListBuffer[KeyFilter[K]]()

  def withFilter[KeyFilter[K]](filter: F) = {
    _filters += filter
    this
  }

  def filters: Seq[KeyFilter[K]] = _filters

  def isEmpty = _filters.isEmpty

  def includeKey(key: K): Boolean =
    _filters.map(_.includeKey(key)).foldLeft(true)(_ && _)

  def includePartition(minKey: KeyBounds[K], maxKey: KeyBounds[K]): Boolean =
    _filters.map(_.includePartition(minKey, maxKey)).foldLeft(true)(_ && _)
}

object FilterSet {
  def EMPTY[K] = new FilterSet[K]

  def apply[K]() = new FilterSet[K]
}
