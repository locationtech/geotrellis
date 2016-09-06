package geotrellis.vectortile.spark

import geotrellis.spark.io.avro._
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.io.avro.codecs.Implicits._
import geotrellis.vector.Extent
import geotrellis.vectortile.VectorTile
import geotrellis.vectortile.protobuf._

import org.apache.avro._
import org.apache.avro.generic._

// --- //

object Implicits {
  /** Encode a [[VectorTile]] via Avro. This is the glue for Layer IO.
    * At the moment, it assumes a Protobuf backend.
    */
  implicit def vectorTileCodec =
    new AvroRecordCodec[VectorTile] {
      def schema: Schema = SchemaBuilder
        .record("VectorTile").namespace("geotrellis.vectortile")
        .fields()
        .name("bytes").`type`().bytesType().noDefault()
        .name("extent").`type`(extentCodec.schema).noDefault()
        .endRecord()

      def encode(tile: VectorTile, rec: GenericRecord): Unit = {
        tile match {
          case t: ProtobufTile => {
            rec.put("bytes", t.toBytes)
            rec.put("extent", extentCodec.encode(t.tileExtent))
          }
        }
      }

      def decode(rec: GenericRecord): VectorTile = {
        val bytes = rec[Array[Byte]]("bytes")
        val extent: Extent = extentCodec.decode(rec[GenericRecord]("extent"))

        ProtobufTile.fromBytes(bytes, extent)
      }
    }
}
