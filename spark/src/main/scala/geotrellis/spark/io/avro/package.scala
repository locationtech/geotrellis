package geotrellis.spark.io

import geotrellis.raster.Tile
import geotrellis.spark.io.avro.TileCodecs._
import org.apache.avro.SchemaBuilder
import org.apache.avro.generic.GenericRecord
import scala.collection.JavaConverters._

package object avro {
  val tileUnionCodec = new AvroUnionCodec[Tile](ByteArrayTileCodec, FloatArrayTileCodec, DoubleArrayTileCodec, IntArrayTileCodec)

  implicit def tupleCodec[A: AvroRecordCodec, B: AvroRecordCodec] = new TupleCodec[A, B]

  def recordCodec[K, V](implicit keyCodec: AvroRecordCodec[K], valCodec: AvroRecordCodec[V]) = {
    val pairCodec = tupleCodec[K,V]

    new AvroRecordCodec[Vector[(K, V)]]() {
      def schema = SchemaBuilder
        .record("TileRecord").namespace("geotrellis.spark.io")
        .fields()
        .name("pairs").`type`().array().items.`type`(pairCodec.schema).noDefault()
        .endRecord()

      def encode(t: Vector[(K, V)], rec: GenericRecord) = {
        rec.put("pairs", t.map(pairCodec.encode).asJavaCollection)
      }

      def decode(rec: GenericRecord) = {
        rec.get("pairs")
          .asInstanceOf[java.util.Collection[GenericRecord]]
          .asScala
          .map(pairCodec.decode)
          .toVector
      }
    }
  }
}
