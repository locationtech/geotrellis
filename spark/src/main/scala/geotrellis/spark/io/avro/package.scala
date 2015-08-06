package geotrellis.spark.io

import geotrellis.raster.Tile
import geotrellis.spark.io.avro.TileCodecs._

package object avro {
  val tileUnionCodec = new AvroUnionCodec[Tile](
    ByteArrayTileCodec,
    FloatArrayTileCodec,
    DoubleArrayTileCodec,
    ShortArrayTileCodec,
    IntArrayTileCodec)

  implicit def tupleCodec[A: AvroRecordCodec, B: AvroRecordCodec] = new TupleCodec[A, B]
  def recordCodec[K: AvroRecordCodec, V: AvroRecordCodec] = new TileRecordCodec[K, V]
}
