package geotrellis.spark.io.index

import geotrellis.spark._
import geotrellis.spark.io.index.hilbert._

import com.github.nscala_time.time.Imports._


private[index] trait HilbertKeyIndexMethod

object HilbertKeyIndexMethod extends HilbertKeyIndexMethod {
  implicit def spatialKeyIndexIndex(m: HilbertKeyIndexMethod): KeyIndexMethod[SpatialKey] =
    new KeyIndexMethod[SpatialKey] {
      def createIndex(keyBounds: KeyBounds[SpatialKey]) = {
        val xResolution = resolution(keyBounds.maxKey.col, keyBounds.minKey.col)
        val yResolution = resolution(keyBounds.maxKey.row, keyBounds.minKey.row)
        HilbertSpatialKeyIndex(keyBounds, xResolution, yResolution)
      }
    }

  def apply(temporalResolution: Int): KeyIndexMethod[SpaceTimeKey] =
    new KeyIndexMethod[SpaceTimeKey] {
      def createIndex(keyBounds: KeyBounds[SpaceTimeKey]) = {
        val xResolution = resolution(keyBounds.maxKey.col, keyBounds.minKey.col)
        val yResolution = resolution(keyBounds.maxKey.row, keyBounds.minKey.row)
        HilbertSpaceTimeKeyIndex(keyBounds, xResolution, yResolution, temporalResolution)
      }
    }

  def apply(minDate: DateTime, maxDate: DateTime, temporalResolution: Int): KeyIndexMethod[SpaceTimeKey] =
    new KeyIndexMethod[SpaceTimeKey] {
      def createIndex(keyBounds: KeyBounds[SpaceTimeKey]): KeyIndex[SpaceTimeKey] = {
        val adjustedKeyBounds = {
          val minKey = keyBounds.minKey
          val maxKey = keyBounds.maxKey
          KeyBounds[SpaceTimeKey](SpaceTimeKey(minKey.col, minKey.row, minDate), SpaceTimeKey(maxKey.col, maxKey.row, maxDate))
        }
        val xResolution = resolution(keyBounds.maxKey.col, keyBounds.minKey.col)
        val yResolution = resolution(keyBounds.maxKey.row, keyBounds.minKey.row)
        HilbertSpaceTimeKeyIndex(adjustedKeyBounds, xResolution, yResolution, temporalResolution)
      }
    }
}
