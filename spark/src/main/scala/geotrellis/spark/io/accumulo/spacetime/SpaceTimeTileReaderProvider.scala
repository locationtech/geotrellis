package geotrellis.spark.io.accumulo.spacetime

import geotrellis.spark._
import geotrellis.spark.utils._
import geotrellis.spark.io._
import geotrellis.spark.io.accumulo._
import geotrellis.spark.io.index._
import geotrellis.raster._

import org.apache.hadoop.io.Text
import org.apache.accumulo.core.security.Authorizations
import org.apache.accumulo.core.data.{Range => ARange}

import scala.collection.JavaConversions._

object SpaceTimeTileReaderProvider extends TileReaderProvider[SpaceTimeKey] {
  import SpaceTimeRasterRDDIndex._

  def index(tileLayout: TileLayout, keyBounds: KeyBounds[SpaceTimeKey]): KeyIndex[SpaceTimeKey] =
    new YearZSpaceTimeKeyIndex

  def reader(instance: AccumuloInstance, layerId: LayerId, accumuloLayerMetaData: AccumuloLayerMetaData, index: KeyIndex[SpaceTimeKey]): Reader[SpaceTimeKey, Tile] = {
    val AccumuloLayerMetaData(rasterMetaData, _, _, tileTable) = accumuloLayerMetaData
    new Reader[SpaceTimeKey, Tile] {
      def read(key: SpaceTimeKey): Tile = {
        val scanner  = instance.connector.createScanner(tileTable, new Authorizations())
        val i = index.toIndex(key)
        scanner.setRange(new ARange(rowId(layerId, i)))
        scanner.fetchColumn(new Text(layerId.name), timeText(key))
        val values = scanner.iterator.toList.map(_.getValue)
        val value =
          if(values.size == 0) {
            sys.error(s"Tile with key $key not found for layer $layerId")
          } else if(values.size > 1) {
            sys.error(s"Multiple tiles found for $key for layer $layerId")
          } else {
            values.head
          }

        val (readKey, tileBytes) = KryoSerializer.deserialize[(SpaceTimeKey, Array[Byte])](value.get)

        ArrayTile.fromBytes(
          tileBytes,
          rasterMetaData.cellType,
          rasterMetaData.tileLayout.tileCols,
          rasterMetaData.tileLayout.tileRows
        )
      }
    }
  }

}
