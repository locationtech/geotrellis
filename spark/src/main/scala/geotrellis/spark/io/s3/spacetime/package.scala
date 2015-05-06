package geotrellis.spark.io.s3

import geotrellis.spark._
import geotrellis.spark.io.index._
import org.joda.time.format.ISODateTimeFormat

package object spacetime {
  private val fmt = ISODateTimeFormat.dateTime()

  private[spacetime]
  val encodeKey = (key: SpaceTimeKey, ki: KeyIndex[SpaceTimeKey], max: Int) => {
    val index: String = ki.toIndex(key).toString.reverse.padTo(max, '0').reverse
    val isoTime: String = fmt.print(key.time)
    f"${index}-${isoTime}"
  }
}