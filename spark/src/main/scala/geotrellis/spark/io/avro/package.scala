package geotrellis.spark.io

import geotrellis.raster.Tile
import geotrellis.spark.io.avro.TileCodecs._
import org.apache.avro.generic.GenericRecord

package object avro {
  implicit class GenericRecordMethods(rec: GenericRecord) {
    def apply[X](name: String) = rec.get(name).asInstanceOf[X]
  }

  implicit def tileUnionCodec = new AvroUnionCodec[Tile](
    ByteArrayTileCodec,
    FloatArrayTileCodec,
    DoubleArrayTileCodec,
    ShortArrayTileCodec,
    IntArrayTileCodec)

  implicit def tupleCodec[A: AvroRecordCodec, B: AvroRecordCodec] = TupleCodec[A, B]
}
