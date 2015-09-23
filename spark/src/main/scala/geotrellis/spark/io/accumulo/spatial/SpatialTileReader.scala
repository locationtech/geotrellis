package geotrellis.spark.io.accumulo.spatial

import geotrellis.spark._
import geotrellis.spark.io.accumulo._
import geotrellis.spark.io.avro.codecs.KeyCodecs
import KeyCodecs._
import geotrellis.spark.io.index._
import geotrellis.spark.utils._
import geotrellis.raster._

import org.apache.hadoop.io.Text
import org.apache.accumulo.core.security.Authorizations
import org.apache.accumulo.core.data.{Range => ARange, Key => AccumuloKey, Value => AccumuloValue}

import scala.collection.JavaConversions._

object SpatialTileReader extends TileReader[SpatialKey] {
  def collectTile(
    instance: AccumuloInstance,
    layerId: LayerId,
    kIndex: KeyIndex[SpatialKey],
    tileTable: String,
    key: SpatialKey
  ): List[AccumuloValue] = {
    val scanner  = instance.connector.createScanner(tileTable, new Authorizations())
    scanner.setRange(new ARange(rowId(layerId, kIndex.toIndex(key))))
    scanner.fetchColumnFamily(new Text(layerId.name))
    scanner.iterator.toList.map(_.getValue)
  }
}
