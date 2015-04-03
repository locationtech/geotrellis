package geotrellis.spark.io.accumulo.spatial

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.accumulo._
import geotrellis.raster._

import org.apache.hadoop.io.Text
import org.apache.accumulo.core.security.Authorizations
import org.apache.accumulo.core.data.{Range => ARange}

import scala.collection.JavaConversions._

object SpatialTileReaderProvider extends TileReaderProvider[SpatialKey] {
  import SpatialRasterRDDIndex._

  def reader(instance: AccumuloInstance, layerId: LayerId, accumuloLayerMetaData: AccumuloLayerMetaData): Reader[SpatialKey, Tile] = {
    val AccumuloLayerMetaData(layerMetaData, tileTable) = accumuloLayerMetaData
    val metaData = layerMetaData.rasterMetaData
    new Reader[SpatialKey, Tile] {
      def read(key: SpatialKey): Tile = {
        val scanner  = instance.connector.createScanner(tileTable, new Authorizations())
        scanner.setRange(new ARange(rowId(layerId, key)))
        scanner.fetchColumnFamily(new Text(layerId.name))
        val values = scanner.iterator.toList.map(_.getValue)
        val value =
          if(values.size == 0) {
            sys.error(s"Tile with key $key not found for layer $layerId")
          } else if(values.size > 1) {
            sys.error(s"Multiple tiles found for $key for layer $layerId")
          } else {
            values.head
          }

        ArrayTile.fromBytes(
          value.get,
          metaData.cellType,
          metaData.tileLayout.tileCols,
          metaData.tileLayout.tileRows
        )
      }
    }
  }

}
