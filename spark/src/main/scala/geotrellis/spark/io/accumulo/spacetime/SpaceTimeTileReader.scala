package geotrellis.spark.io.accumulo.spacetime

import geotrellis.spark._
import geotrellis.spark.io.accumulo._
import geotrellis.spark.io.avro.KeyCodecs._
import geotrellis.spark.io.index._

import org.apache.hadoop.io.Text
import org.apache.accumulo.core.security.Authorizations
import org.apache.accumulo.core.data.{Range => ARange, Value => AccumuloValue}

import scala.collection.JavaConversions._

object SpaceTimeTileReader extends TileReader[SpaceTimeKey] {
  def collectTile(
    instance: AccumuloInstance,
    layerId: LayerId,
    kIndex: KeyIndex[SpaceTimeKey],
    tileTable: String,
    key: SpaceTimeKey
  ): List[AccumuloValue] = {
    val scanner = instance.connector.createScanner(tileTable, new Authorizations())
    val i = kIndex.toIndex(key)
    scanner.setRange(new ARange(rowId(layerId, i)))
    scanner.fetchColumn(new Text(layerId.name), timeText(key))
    scanner.iterator.toList.map(_.getValue)
  }
}
