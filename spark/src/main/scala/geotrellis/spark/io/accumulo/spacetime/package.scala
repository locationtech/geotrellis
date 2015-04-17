package geotrellis.spark.io.accumulo

import geotrellis.spark._

import org.apache.hadoop.io.Text
import org.joda.time.{DateTimeZone, DateTime}

package object spacetime {
  private[spacetime] def timeChunk(time: DateTime): String =
    time.getYear.toString

  private[spacetime] def timeText(key: SpaceTimeKey): Text =
    new Text(key.temporalKey.time.withZone(DateTimeZone.UTC).toString)

  private[spacetime] def rowId(id: LayerId, index: Long): String =
    f"${id.zoom}%02d_${index}%019d"
}
