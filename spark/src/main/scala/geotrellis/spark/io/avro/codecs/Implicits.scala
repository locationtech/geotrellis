package geotrellis.spark.io.avro.codecs

import geotrellis.raster.Tile
import geotrellis.spark.io.avro.{AvroRecordCodec, AvroUnionCodec}

object Implicits extends Implicits

trait Implicits
    extends TileCodecs
    with TileFeatureCodec
    with KeyCodecs {
  implicit def tileUnionCodec = new AvroUnionCodec[Tile](
    byteArrayTileCodec,
    floatArrayTileCodec,
    doubleArrayTileCodec,
    shortArrayTileCodec,
    intArrayTileCodec,
    bitArrayTileCodec,
    uByteArrayTileCodec,
    uShortArrayTileCodec)

  implicit def tupleCodec[A: AvroRecordCodec, B: AvroRecordCodec]: TupleCodec[A, B] = TupleCodec[A, B]
}
