package geotrellis.spark.accumulo


import geotrellis.raster._
import org.apache.accumulo.core.data.{Key, Mutation, Value, Range => ARange}
import org.apache.hadoop.io.Text


/**
 * This class is used to convert a 3d addressed tile to a row Mutation
 * and AccumuloRow into a 3d addressed tile
 */
trait TileAccumuloFormat[INDEX] extends Serializable {
  def write(id: INDEX, tile: Tile): Mutation
  def write(tup: (INDEX, Tile)): Mutation = write(tup._1, tup._2)

  def read(key: Key, value: Value): (INDEX, Tile)
  def read(row: (Key, Value)): (INDEX, Tile) = read(row._1, row._2)
}


class TmsTilingAccumuloFormat(name: String, zoom: Int) extends TileAccumuloFormat[Long] {
  val rowIdRx = """(\d+)_(\d+)""".r

  override def write(id: Long, tile: Tile): Mutation = {
    val rowID = new Text(s"${zoom}_${id}")
    val colFam = new Text(name)
    val colQual = new Text(TileBytes.tileTag(tile))
    val timestamp = System.currentTimeMillis()
    val value = new Value(tile.toBytes())

    val mutation = new Mutation(rowID)
    mutation.put(colFam, colQual, timestamp, value)
    mutation
  }

  override def read(key: Key, value: Value): (Long, Tile) = {
    val rowIdRx(zoom, id) = key.getRow.toString
    val tile = TileBytes.fromBytes(key.getColumnQualifier.toString, value.get)

    id.toLong -> tile
  }
}