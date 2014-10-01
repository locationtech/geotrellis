package geotrellis.spark.io.accumulo

import geotrellis.spark._
import geotrellis.spark.rdd.LayerMetaData
import geotrellis.spark.tiling._
import org.apache.accumulo.core.client.{BatchWriterConfig, Scanner, Connector}
import org.apache.accumulo.core.data.{Key, Mutation, Value, Range => ARange}
import org.apache.hadoop.io.Text



object TmsTileFormat extends TileFormat {

  val rowIdRx = """(\d+)_(\d+)""".r // (zoom)_(TmsTilingId)

  def rowId(id: Long, layer: Layer) = new Text(s"${layer.zoom}_${id}")

  override def ranges(layer: Layer, metaData: LayerMetaData, bounds: Option[(TileBounds, TileCoordScheme)]): Seq[ARange] = {
    bounds match {
      case None =>
        new ARange(
          new Text(s"${layer.zoom}_0"),
          new Text(s"${layer.zoom}_9")
        ) :: Nil
      case Some((tileBounds, coordScheme)) =>
        metaData.transform
          .withCoordScheme(coordScheme)
          .tileToIndex(tileBounds)
          .spans
          .map { ts =>
            new ARange(rowId(ts._1, layer), rowId(ts._2, layer))
          }
    }
  }

  override def write(layer: Layer, tup: TmsTile): Mutation = {
    val mutation = new Mutation(rowId(tup.id, layer))
    mutation.put(
      new Text(layer.name), new Text(TileBytes.tileTag(tup.tile)),
      System.currentTimeMillis(),
      new Value(tup.tile.toBytes()))
    mutation
  }

  override def read(key: Key, value: Value): TmsTile = {
    val rowIdRx(zoom, id) = key.getRow.toString
    val tile = TileBytes.fromBytes(key.getColumnQualifier.toString, value.get)
    TmsTile(id.toLong, tile)
  }
}
