package geotrellis.spark.io.avro.codecs

import java.nio.ByteBuffer

import geotrellis.raster._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.avro.codecs.Implicits._

import org.apache.avro.SchemaBuilder
import org.apache.avro.generic._

import scala.collection.JavaConverters._
import scala.util.Try


trait TileFeatureCodec {

  implicit def tileFeatureCodec[
    T <: Tile: AvroRecordCodec,
    D: AvroRecordCodec
  ]: AvroRecordCodec[TileFeature[T,D]] = new AvroRecordCodec[TileFeature[T,D]] {
    def schema = SchemaBuilder
      .record("TileFeature").namespace("geotrellis.raster")
      .fields()
      .name("tile").`type`(implicitly[AvroRecordCodec[T]].schema).noDefault
      .name("data").`type`(implicitly[AvroRecordCodec[D]].schema).noDefault
      .endRecord()

    def encode(tileFeature: TileFeature[T, D], rec: GenericRecord): Unit = {
      rec.put("tile", implicitly[AvroRecordCodec[T]].encode(tileFeature.tile))
      rec.put("data", implicitly[AvroRecordCodec[D]].encode(tileFeature.data))
    }

    def decode(rec: GenericRecord): TileFeature[T,D] = {
      val tile: T = implicitly[AvroRecordCodec[T]].decode(rec.get("tile").asInstanceOf[GenericRecord])
      val data: D = implicitly[AvroRecordCodec[D]].decode(rec.get("data").asInstanceOf[GenericRecord])
      TileFeature(tile, data)
    }

  }

}
