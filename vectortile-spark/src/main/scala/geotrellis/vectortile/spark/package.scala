package geotrellis.vectortile

import java.nio.ByteBuffer

import geotrellis.spark.io.avro._
import geotrellis.spark.io.avro.codecs.Implicits._
import geotrellis.vector.Extent
import org.apache.avro._
import org.apache.avro.generic._

// --- //

package object spark {

  /** Encode a [[VectorTile]] via Avro. This is the glue for Layer IO.
    * At the moment, it assumes a Protobuf backend.
    */
  implicit val vectorTileCodec = new AvroRecordCodec[VectorTile] {
    def schema: Schema = SchemaBuilder
      .record("VectorTile").namespace("geotrellis.vectortile")
      .fields()
      .name("bytes").`type`().bytesType().noDefault()
      .name("extent").`type`(extentCodec.schema).noDefault()
      .endRecord()

    def encode(tile: VectorTile, rec: GenericRecord): Unit = {
      rec.put("bytes", ByteBuffer.wrap(tile.toBytes))
      rec.put("extent", extentCodec.encode(tile.tileExtent))
    }

    def decode(rec: GenericRecord): VectorTile = {
      val bytes: Array[Byte] = rec[ByteBuffer]("bytes").array
      val extent: Extent = extentCodec.decode(rec[GenericRecord]("extent"))

      VectorTile.fromBytes(bytes, extent)
    }
  }

}
