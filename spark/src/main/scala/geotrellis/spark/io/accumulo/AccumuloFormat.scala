package geotrellis.spark.io.accumulo

import geotrellis.spark._
import geotrellis.raster._
import geotrellis.spark.tiling._
import org.apache.accumulo.core.data.{Key, Mutation, Value, Range => ARange}
import org.apache.hadoop.io.Text

/**
 * This class is used to convert a 3d addressed tile to a row Mutation
 * and AccumuloRow into a 3d addressed tile
 */
trait AccumuloFormat[INDEX, LAYER] extends Serializable {
  type Params

  def ranges(layer: LAYER, spans: Option[Seq[(TileId, TileId)]]): Seq[ARange]

  def write(layer: LAYER, id: INDEX, tile: Tile): Mutation
  def write(layer: LAYER, tup: (INDEX, Tile)): Mutation = write(layer, tup._1, tup._2)

  def read(key: Key, value: Value): (INDEX, Tile)
  def read(row: (Key, Value)): (INDEX, Tile) = read(row._1, row._2)
}

case class TmsLayer(name: String, levelId: Int)

class TmsTilingAccumuloFormat extends AccumuloFormat[TileId, TmsLayer] {
  val rowIdRx = """(\d+)_(\d+)""".r // (zoom)_(TmsTilingId)
  val layerRx = """(\w+):(\d+)""".r

  def rowId(id: Long, layer: TmsLayer) = new Text(s"${layer.levelId}_${id}")
  
  override def ranges(layer: TmsLayer, spans: Option[Seq[(TileId, TileId)]]): Seq[ARange] = {
    spans match {
      case None     => new ARange() :: Nil
      case Some(s) => 
        s.map{ ts => 
          new ARange(rowId(ts._1, layer), rowId(ts._2, layer))
        }
    }
  }

  override def write(layer: TmsLayer, id: Long, tile: Tile): Mutation = {
    val rowID = rowId(id, layer)
    val colFam = new Text(layer.name)
    val colQual = new Text(TileBytes.tileTag(tile))
    val timestamp = System.currentTimeMillis()
    val value = new Value(tile.toBytes())

    val mutation = new Mutation(rowID)
    mutation.put(colFam, colQual, timestamp, value)
    mutation
  }

  //TODO: this probably needs the layer as well
  override def read(key: Key, value: Value): (Long, Tile) = {
    val rowIdRx(zoom, id) = key.getRow.toString
    val tile = TileBytes.fromBytes(key.getColumnQualifier.toString, value.get)

    id.toLong -> tile
  }
}
