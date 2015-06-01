package geotrellis.spark.io.index

import geotrellis.spark._
import geotrellis.spark.io.index.zcurve._

import com.github.nscala_time.time.Imports._

private[index] trait ZCurveKeyIndexMethod

object ZCurveKeyIndexMethod extends ZCurveKeyIndexMethod {
  implicit def spatialKeyIndexMethod(m: ZCurveKeyIndexMethod): KeyIndexMethod[SpatialKey] =
    new KeyIndexMethod[SpatialKey] {
      def createIndex(keyBounds: KeyBounds[SpatialKey]): KeyIndex[SpatialKey] = 
        new ZSpatialKeyIndex()
    }

  def byYear = 
    new KeyIndexMethod[SpaceTimeKey] {
      def createIndex(keyBounds: KeyBounds[SpaceTimeKey]) = ZSpaceTimeKeyIndex.byYear()
    }

  def byPattern(pattern: String) =
    new KeyIndexMethod[SpaceTimeKey] {
      def createIndex(keyBounds: KeyBounds[SpaceTimeKey]) = ZSpaceTimeKeyIndex.byPattern(pattern)
    }

  /** Note: the function timeToGrid have to be in a project scope. */
  def by(timeToGrid: DateTime => Int) = 
    new KeyIndexMethod[SpaceTimeKey] {
      def createIndex(keyBounds: KeyBounds[SpaceTimeKey]) = ZSpaceTimeKeyIndex(timeToGrid)
    }
}
