package geotrellis.spark.io.index

import geotrellis.spark._
import geotrellis.spark.io.index.hilbert._

import com.github.nscala_time.time.Imports._


private[index] trait HilbertKeyIndexMethod

object HilbertKeyIndexMethod extends HilbertKeyIndexMethod {
  implicit def spatialKeyIndexIndex(m: HilbertKeyIndexMethod): KeyIndexMethod[GridKey] =
    new KeyIndexMethod[GridKey] {
      def createIndex(keyBounds: KeyBounds[GridKey]) = {
        val xResolution = resolution(keyBounds.maxKey.col, keyBounds.minKey.col)
        val yResolution = resolution(keyBounds.maxKey.row, keyBounds.minKey.row)
        HilbertGridKeyIndex(keyBounds, xResolution, yResolution)
      }
    }

  def apply(temporalResolution: Int): KeyIndexMethod[GridTimeKey] =
    new KeyIndexMethod[GridTimeKey] {
      def createIndex(keyBounds: KeyBounds[GridTimeKey]) = {
        val xResolution = resolution(keyBounds.maxKey.col, keyBounds.minKey.col)
        val yResolution = resolution(keyBounds.maxKey.row, keyBounds.minKey.row)
        HilbertGridTimeKeyIndex(keyBounds, xResolution, yResolution, temporalResolution)
      }
    }

  def apply(minDate: DateTime, maxDate: DateTime, temporalResolution: Int): KeyIndexMethod[GridTimeKey] =
    new KeyIndexMethod[GridTimeKey] {
      def createIndex(keyBounds: KeyBounds[GridTimeKey]): KeyIndex[GridTimeKey] = {
        val adjustedKeyBounds = {
          val minKey = keyBounds.minKey
          val maxKey = keyBounds.maxKey
          KeyBounds[GridTimeKey](GridTimeKey(minKey.col, minKey.row, minDate), GridTimeKey(maxKey.col, maxKey.row, maxDate))
        }
        val xResolution = resolution(keyBounds.maxKey.col, keyBounds.minKey.col)
        val yResolution = resolution(keyBounds.maxKey.row, keyBounds.minKey.row)
        HilbertGridTimeKeyIndex(adjustedKeyBounds, xResolution, yResolution, temporalResolution)
      }
    }
}
