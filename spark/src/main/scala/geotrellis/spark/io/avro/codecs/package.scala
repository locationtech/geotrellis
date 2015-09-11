package geotrellis.spark.io.avro

import geotrellis.raster.Tile

package object codecs extends TileCodecs with KeyCodecs {
  implicit def tileUnionCodec = new AvroUnionCodec[Tile](
    ByteArrayTileCodec,
    FloatArrayTileCodec,
    DoubleArrayTileCodec,
    ShortArrayTileCodec,
    IntArrayTileCodec)

  implicit def tupleCodec[A: AvroRecordCodec, B: AvroRecordCodec]: TupleCodec[A, B] = TupleCodec[A, B]
}

