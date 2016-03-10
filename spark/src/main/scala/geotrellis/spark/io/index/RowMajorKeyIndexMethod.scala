package geotrellis.spark.io.index

import geotrellis.spark._
import geotrellis.spark.io.index.rowmajor._

import com.github.nscala_time.time.Imports._

private[index] trait RowMajorKeyIndexMethod

object RowMajorKeyIndexMethod extends RowMajorKeyIndexMethod {
  implicit def spatialKeyIndexMethod(m: RowMajorKeyIndexMethod): KeyIndexMethod[GridKey] =
    new KeyIndexMethod[GridKey] {
      def createIndex(keyBounds: KeyBounds[GridKey]): KeyIndex[GridKey] =
        new RowMajorGridKeyIndex(keyBounds)
    }
}
