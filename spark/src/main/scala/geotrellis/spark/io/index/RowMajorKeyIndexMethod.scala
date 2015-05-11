package geotrellis.spark.io.index

import geotrellis.spark._
import geotrellis.spark.io.index.rowmajor._

import com.github.nscala_time.time.Imports._

private[index] trait RowMajorKeyIndexMethod

object RowMajorKeyIndexMethod extends RowMajorKeyIndexMethod {
  implicit def spatialKeyIndexMethod(m: RowMajorKeyIndexMethod): KeyIndexMethod[SpatialKey] =
    new KeyIndexMethod[SpatialKey] {
      def createIndex(keyBounds: KeyBounds[SpatialKey]): KeyIndex[SpatialKey] = 
        new RowMajorSpatialKeyIndex(keyBounds)
    }
}
