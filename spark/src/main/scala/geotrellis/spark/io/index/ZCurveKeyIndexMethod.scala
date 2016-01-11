package geotrellis.spark.io.index

import geotrellis.spark._
import geotrellis.spark.io.index.zcurve._

import com.github.nscala_time.time.Imports._

private[index] trait ZCurveKeyIndexMethod

object ZCurveKeyIndexMethod extends ZCurveKeyIndexMethod {
  implicit def spatialKeyIndexMethod(m: ZCurveKeyIndexMethod): KeyIndexMethod[SpatialKey, ZSpatialKeyIndex] =
    new KeyIndexMethod[SpatialKey, ZSpatialKeyIndex] {
      def createIndex(keyBounds: KeyBounds[SpatialKey]) =
        new ZSpatialKeyIndex()
    }

  def byYear = 
    new KeyIndexMethod[SpaceTimeKey, ZSpaceTimeKeyIndex] {
      def createIndex(keyBounds: KeyBounds[SpaceTimeKey]) = ZSpaceTimeKeyIndex.byYear()
    }

  def byMonth =
    new KeyIndexMethod[SpaceTimeKey, ZSpaceTimeKeyIndex] {
      def createIndex(keyBounds: KeyBounds[SpaceTimeKey]) = ZSpaceTimeKeyIndex.byMonth()
    }

  def byDay =
    new KeyIndexMethod[SpaceTimeKey, ZSpaceTimeKeyIndex] {
      def createIndex(keyBounds: KeyBounds[SpaceTimeKey]) = ZSpaceTimeKeyIndex.byDay()
    }

  def byPattern(pattern: String) =
    new KeyIndexMethod[SpaceTimeKey, ZSpaceTimeKeyIndex] {
      def createIndex(keyBounds: KeyBounds[SpaceTimeKey]) = ZSpaceTimeKeyIndex.byPattern(pattern)
    }

  /** Note: the function timeToGrid have to be in a project scope. */
  def by(timeToGrid: DateTime => Int) = 
    new KeyIndexMethod[SpaceTimeKey, ZSpaceTimeKeyIndex] {
      def createIndex(keyBounds: KeyBounds[SpaceTimeKey]) = new ZSpaceTimeKeyIndex("")
    }
}
