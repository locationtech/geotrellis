package geotrellis.spark.io.accumulo.spacetime

import geotrellis.spark._
import geotrellis.spark.io.accumulo._
import geotrellis.spark.io.avro.{TupleCodec, AvroEncoder}
import geotrellis.spark.io.avro.KeyCodecs._
import geotrellis.spark.io.index._
import geotrellis.spark.utils._
import geotrellis.raster._

import org.apache.accumulo.core.data.{Key, Value}
import org.apache.spark.rdd.RDD

object SpaceTimeRasterRDDWriter extends RasterRDDWriter[SpaceTimeKey] {
  import geotrellis.spark.io.accumulo.stringToText

  lazy val writeCodec = KryoWrapper(TupleCodec[SpaceTimeKey, Tile])

  def rowId(id: LayerId, index: Long): String  = spacetime.rowId (id, index)

  def encode(
    layerId: LayerId,
    raster: RasterRDD[SpaceTimeKey],
    index: KeyIndex[SpaceTimeKey]
  ): RDD[(Key, Value)] = {
    def getKey(id: LayerId, key: SpaceTimeKey): Key =
      new Key(rowId(id, index.toIndex(key)), id.name, timeText(key))

    raster
      .map { case tuple @ (key, _) => {
        val value = AvroEncoder.toBinary(tuple)(writeCodec.value)
        getKey(layerId, key) -> new Value(value)
      }}
  }
}
