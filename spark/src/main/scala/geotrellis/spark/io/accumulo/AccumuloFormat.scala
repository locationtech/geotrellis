package geotrellis.spark.io.accumulo

import geotrellis.spark._
import geotrellis.raster._
import geotrellis.spark.rdd.LayerMetaData
import geotrellis.spark.tiling._
import org.apache.accumulo.core.client.{BatchWriterConfig, Scanner, Connector}
import org.apache.accumulo.core.data.{Key, Mutation, Value, Range => ARange}
import org.apache.accumulo.core.security.Authorizations
import org.apache.hadoop.io.Text
import geotrellis.spark.json._
import spray.json._

/**
 * This class is used to convert a 3d addressed tile to a row Mutation
 * and AccumuloRow into a 3d addressed tile
 */
object AccumuloFormat{
  /**
   * I can think of only one way to write MetaData from RasterRDD
   */
  def encodeMetaData(md: LayerMetaData, layer: Layer): Mutation = {
    val rowID = new Text("__metadata")
    val colFam = new Text(layer.asString)
    val colQual = new Text("json")
    val timestamp = System.currentTimeMillis()
    val value = new Value(md.toJson.prettyPrint.getBytes)

    val mutation = new Mutation(rowID)
    mutation.put(colFam, colQual, timestamp, value)
    mutation
  }

  def decodeMetaData(key: Key, value: Value): LayerMetaData = {
    new String(value.get().map(_.toChar)).parseJson.convertTo[LayerMetaData]
  }

  def saveMetaData(md: LayerMetaData, layer: Layer, table: String, conn: Connector): Unit = {
    val cfg = new BatchWriterConfig()
    val writer = conn.createBatchWriter(table, cfg)
    writer.addMutation(encodeMetaData(md, layer))
    writer.close()
  }

  def loadMetaData(layer: Layer, table: String, conn: Connector): Option[LayerMetaData] ={
    val scan = conn.createScanner(table, new Authorizations())
    scan.setRange(new ARange("__metadata"))
    scan.fetchColumn(new Text(layer.asString), new Text("json"))

    val iter = scan.iterator
    val result =
      if (iter.hasNext) {
        Some(iter.next.getValue.toString.parseJson.convertTo[LayerMetaData])
      } else {
        None
      }

    scan.close()
    result
  }
}

trait AccumuloFormat[INDEX, LAYER] extends Serializable {
  type Params

  def ranges(layer: LAYER, metaData: LayerMetaData, bounds: Option[(TileBounds, TileCoordScheme)]): Seq[ARange]

  def write(layer: LAYER, id: INDEX, tile: Tile): Mutation
  def write(layer: LAYER, tup: (INDEX, Tile)): Mutation = write(layer, tup._1, tup._2)

  def read(key: Key, value: Value): (INDEX, Tile)
  def read(row: (Key, Value)): (INDEX, Tile) = read(row._1, row._2)
}

trait Layer {
  def asString: String
}
case class TmsLayer(name: String, levelId: Int) extends Layer {
  def asString = name
}

class TmsTilingAccumuloFormat extends AccumuloFormat[TileId, TmsLayer] {
  val rowIdRx = """(\d+)_(\d+)""".r // (zoom)_(TmsTilingId)
  val layerRx = """(\w+):(\d+)""".r

  def rowId(id: Long, layer: TmsLayer) = new Text(s"${layer.levelId}_${id}")

  override def ranges(layer: TmsLayer, metaData: LayerMetaData, bounds: Option[(TileBounds, TileCoordScheme)]): Seq[ARange] = {
    bounds match {
      case None =>
        new ARange() :: Nil
      case Some((tileBounds, coordScheme)) =>
        metaData.transform
          .withCoordScheme(coordScheme)
          .tileToIndex(tileBounds)
          .spans
          .map { ts =>  new ARange(rowId(ts._1, layer), rowId(ts._2, layer)) }
    }
  }

  override def write(layer: TmsLayer, id: TileId, tile: Tile): Mutation = {
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