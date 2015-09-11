package geotrellis.spark.io.accumulo.spatial

import geotrellis.spark._
import geotrellis.spark.io.accumulo._
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.index._
import geotrellis.spark.utils._
import geotrellis.raster._

import org.apache.accumulo.core.data.{Key, Value}
import org.apache.spark.rdd.RDD

object SpatialRasterRDDWriter extends RasterRDDWriter[SpatialKey] {

  lazy val writeCodec = KryoWrapper(TupleCodec[SpatialKey, Tile])

  def rowId(id: LayerId, index: Long): String  = spatial.rowId (id, index)

  def encode(
    layerId: LayerId,
    raster: RasterRDD[SpatialKey],
    index: KeyIndex[SpatialKey]
  ): RDD[(Key, Value)] = {
    def getKey(id: LayerId, key: SpatialKey): Key =
      new Key(rowId(id, index.toIndex(key)), id.name)

    raster
      .map { case tuple @ (key, _) =>
        val value = AvroEncoder.toBinary(tuple)(writeCodec.value)
        getKey(layerId, key) -> new Value(value)
      }
  }
}
