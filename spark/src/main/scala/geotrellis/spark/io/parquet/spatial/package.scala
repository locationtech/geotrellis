package geotrellis.spark.io.parquet

import geotrellis.spark._
import geotrellis.spark.io.index._
import org.joda.time.format.ISODateTimeFormat

import org.apache.spark.sql.Row

package object spatial {
  private[spatial]
  val createTileRow = (layerId: LayerId, key: SpatialKey, index: Long, bytes: Array[Byte]) =>
    Row(layerId.zoom, layerId.name, key.row, key.col, 0L, index, bytes)
}
