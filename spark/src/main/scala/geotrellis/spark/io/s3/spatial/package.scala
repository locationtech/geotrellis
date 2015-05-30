package geotrellis.spark.io.s3

import geotrellis.spark._
import geotrellis.spark.io.index._
import org.joda.time.format.ISODateTimeFormat

package object spatial {

  private[spatial]
  val encodeKey = (key: SpatialKey, ki: KeyIndex[SpatialKey], max: Int) => {
    ki.toIndex(key).toString.reverse.padTo(max, '0').reverse
  }

  private[spatial]
  val encodeIndex = (index: Long, max: Int) => {
    index.toString.reverse.padTo(max, '0').reverse
  }
}
