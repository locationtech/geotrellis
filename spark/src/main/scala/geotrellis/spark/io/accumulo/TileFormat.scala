package geotrellis.spark.io.accumulo

import geotrellis.spark._
import geotrellis.spark.rdd.LayerMetaData
import geotrellis.spark.tiling._
import org.apache.accumulo.core.data.{Key, Mutation, Value, Range => ARange}

/** Names a layer and a zoom level */
case class Layer(name: String, zoom: Int) {
  def asString = name
}

/**
 * Provides conversion between elements of RasterRDD and Accumulo Key,Value pairs.
 *
 * Note:
 * In the future it may be desirable to have a type parameter for Tile tuple.
 * Currently the only possible representation as of now is TmsTile.
 */
@deprecated("Working on a better way to do it", "September")
trait TileFormat extends Serializable {
  type LAYER = Layer

  def ranges(layer: LAYER, metaData: LayerMetaData, bounds: Option[(TileBounds, TileCoordScheme)]): Seq[ARange]

  def write(layer: LAYER, tile: TmsTile): Mutation
  def read(key: Key, value: Value): TmsTile
}