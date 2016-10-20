package geotrellis.spark.io.avro.codecs

import geotrellis.spark.io.avro._
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.io.avro.codecs.Implicits._
import geotrellis.vector.Extent
import geotrellis.vectortile.VectorTile
import geotrellis.vectortile.protobuf._

import org.apache.avro._
import org.apache.avro.generic._

import java.nio.ByteBuffer

// --- //

trait VectorTileCodec {
  /** Encode a [[VectorTile]] via Avro. This is the glue for Layer IO.
    * At the moment, it assumes a Protobuf backend.
    */
  implicit def vectorTileCodec = new AvroRecordCodec[VectorTile] {
    def schema: Schema = SchemaBuilder
      .record("VectorTile").namespace("geotrellis.vectortile")
      .fields()
      .name("bytes").`type`().bytesType().noDefault()
      .name("extent").`type`(extentCodec.schema).noDefault()
      .endRecord()

    def encode(tile: VectorTile, rec: GenericRecord): Unit = {
      tile match {
        case t: ProtobufTile => {
          rec.put("bytes", ByteBuffer.wrap(t.toBytes))
          rec.put("extent", extentCodec.encode(t.tileExtent))
        }
      }
    }

    def decode(rec: GenericRecord): VectorTile = {
      val bytes: Array[Byte] = rec[ByteBuffer]("bytes").array
      val extent: Extent = extentCodec.decode(rec[GenericRecord]("extent"))

      ProtobufTile.fromBytes(bytes, extent)
    }
  }
}
