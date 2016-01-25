package geotrellis.spark.io.index

import geotrellis.spark._
import geotrellis.spark.io.index.zcurve._
import ZSpaceTimeKeyIndex._

import com.github.nscala_time.time.Imports._

private[index] trait ZCurveKeyIndexMethod

object ZCurveKeyIndexMethod extends ZCurveKeyIndexMethod {
  implicit def spatialKeyIndexMethod(m: ZCurveKeyIndexMethod): KeyIndexMethod[SpatialKey] =
    new KeyIndexMethod[SpatialKey] {
      def createIndex(keyBounds: KeyBounds[SpatialKey]) =
        new ZSpatialKeyIndex(keyBounds)
    }

  def byYear =
    new KeyIndexMethod[SpaceTimeKey] {
      def createIndex(keyBounds: KeyBounds[SpaceTimeKey]) = ZSpaceTimeKeyIndex.byYear(keyBounds)
    }

  def byMonth =
    new KeyIndexMethod[SpaceTimeKey] {
      def createIndex(keyBounds: KeyBounds[SpaceTimeKey]) = ZSpaceTimeKeyIndex.byMonth(keyBounds)
    }

  def byDay =
    new KeyIndexMethod[SpaceTimeKey] {
      def createIndex(keyBounds: KeyBounds[SpaceTimeKey]) = ZSpaceTimeKeyIndex.byDay(keyBounds)
    }

  def byHour =
    new KeyIndexMethod[SpaceTimeKey] {
      def createIndex(keyBounds: KeyBounds[SpaceTimeKey]) = ZSpaceTimeKeyIndex.byHour(keyBounds)
    }

  def byMinute =
    new KeyIndexMethod[SpaceTimeKey] {
      def createIndex(keyBounds: KeyBounds[SpaceTimeKey]) = ZSpaceTimeKeyIndex.byMinute(keyBounds)
    }

  def bySecond =
    new KeyIndexMethod[SpaceTimeKey] {
      def createIndex(keyBounds: KeyBounds[SpaceTimeKey]) = ZSpaceTimeKeyIndex.bySecond(keyBounds)
    }

  def byMillisecondResolution(millis: Long) =
    new KeyIndexMethod[SpaceTimeKey] {
      def createIndex(keyBounds: KeyBounds[SpaceTimeKey]) = ZSpaceTimeKeyIndex.byMillisecondResolution(keyBounds, millis)
    }

}
