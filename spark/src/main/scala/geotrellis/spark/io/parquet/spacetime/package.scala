package geotrellis.spark.io.parquet

import geotrellis.spark._
import geotrellis.spark.io.index._
import org.joda.time.format.ISODateTimeFormat

import org.apache.spark.sql.Row

package object spacetime {
  private[spacetime]
  val createTileRow = (layerId: LayerId, key: SpaceTimeKey, index: Long, bytes: Array[Byte]) => {
    Row(layerId.zoom, layerId.name, key.row, key.col, key.time.getMillis, index, bytes)
  }
}
