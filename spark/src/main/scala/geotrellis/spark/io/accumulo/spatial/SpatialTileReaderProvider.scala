package geotrellis.spark.io.accumulo.spatial

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.accumulo._
import geotrellis.spark.io.index._
import geotrellis.spark.utils._
import geotrellis.raster._

import org.apache.hadoop.io.Text
import org.apache.accumulo.core.security.Authorizations
import org.apache.accumulo.core.data.{Range => ARange}

import scala.collection.JavaConversions._

object SpatialTileReaderProvider extends TileReaderProvider[SpatialKey] {
  import SpatialRasterRDDIndex._

  def index(tileLayout: TileLayout, keyBounds: KeyBounds[SpatialKey]): KeyIndex[SpatialKey] =
    new RowMajorSpatialKeyIndex(tileLayout.layoutCols)

  def reader(instance: AccumuloInstance, layerId: LayerId, accumuloLayerMetaData: AccumuloLayerMetaData, index: KeyIndex[SpatialKey]): Reader[SpatialKey, Tile] = {
    val AccumuloLayerMetaData(rasterMetaData, _, _, tileTable) = accumuloLayerMetaData
    new Reader[SpatialKey, Tile] {
      def read(key: SpatialKey): Tile = {
        val scanner  = instance.connector.createScanner(tileTable, new Authorizations())
        scanner.setRange(new ARange(rowId(layerId, index.toIndex(key))))
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

        val (_, tileBytes) = KryoSerializer.deserialize[(SpatialKey, Array[Byte])](value.get)

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
