package geotrellis.spark.io.s3

import geotrellis.spark._
import geotrellis.spark.io.index._
import org.joda.time.format.ISODateTimeFormat

package object spacetime {
  private val fmt = ISODateTimeFormat.dateTime()

  private[spacetime]
  val encodeKey = (key: SpaceTimeKey, ki: KeyIndex[SpaceTimeKey], max: Int) => {
    val index = encodeIndex(ki.toIndex(key), max)
    val isoTime: String = fmt.print(key.time)
    f"${index}-${isoTime}"
  }

  private[spacetime]
  val encodeIndex = (index: Long, max: Int) => {
    index.toString.reverse.padTo(max, '0').reverse
  }
}
