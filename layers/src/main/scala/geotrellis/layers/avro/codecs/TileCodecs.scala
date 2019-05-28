/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.layers.avro.codecs

import java.nio.ByteBuffer

import geotrellis.raster._
import geotrellis.layers.avro._
import geotrellis.layers.avro.codecs.Implicits._

import org.apache.avro.SchemaBuilder
import org.apache.avro.generic._

import scala.collection.JavaConverters._
import scala.util.Try

trait TileCodecs {
  implicit def shortArrayTileCodec: AvroRecordCodec[ShortArrayTile] = new AvroRecordCodec[ShortArrayTile] {
    def schema = SchemaBuilder
      .record("ShortArrayTile").namespace("geotrellis.raster")
      .fields()
      .name("cols").`type`().intType().noDefault()
      .name("rows").`type`().intType().noDefault()
      .name("cells").`type`().array().items().intType().noDefault()
      .name("noDataValue").`type`().unionOf().intType().and().nullType().endUnion().intDefault(Short.MinValue.toInt)
      .endRecord()

    def encode(tile: ShortArrayTile, rec: GenericRecord) = {
      rec.put("cols", tile.cols)
      rec.put("rows", tile.rows)
      // _* expansion is important, otherwise we get List[Array[Short]] instead of List[Short]
      rec.put("cells", java.util.Arrays.asList(tile.array:_*))
      tile.cellType match {
        case ShortConstantNoDataCellType => rec.put("noDataValue", Short.MinValue.toInt)
        case ShortUserDefinedNoDataCellType(nd) => rec.put("noDataValue", nd.toInt)
        case ShortCellType => rec.put("noDataValue", null)
        case _ => sys.error(s"Cell type ${tile.cellType} was unexpected")
      }
    }

    def decode(rec: GenericRecord) = {
      val array  = rec.get("cells")
        .asInstanceOf[java.util.Collection[Int]]
        .asScala // notice that Avro does not have native support for Short primitive
        .map(_.toShort)
        .toArray
      val cellType = Option(rec[Int]("noDataValue")) match {
        case Some(nd) if nd == Short.MinValue.toInt => ShortConstantNoDataCellType
        case Some(nd) => ShortUserDefinedNoDataCellType(nd.toShort)
        case None => ShortCellType
      }

      ShortArrayTile(array, rec[Int]("cols"), rec[Int]("rows"), cellType)
    }
  }

  implicit def uShortArrayTileCodec: AvroRecordCodec[UShortArrayTile] = new AvroRecordCodec[UShortArrayTile] {
    def schema = SchemaBuilder
      .record("UShortArrayTile").namespace("geotrellis.raster")
      .fields()
      .name("cols").`type`().intType().noDefault()
      .name("rows").`type`().intType().noDefault()
      .name("cells").`type`().array().items().intType().noDefault()
      .name("noDataValue").`type`().unionOf().intType().and().nullType().endUnion().intDefault(0)
      .endRecord()

    def encode(tile: UShortArrayTile, rec: GenericRecord) = {
      rec.put("cols", tile.cols)
      rec.put("rows", tile.rows)
      // _* expansion is important, otherwise we get List[Array[Short]] instead of List[Short]
      rec.put("cells", java.util.Arrays.asList(tile.array:_*))
      tile.cellType match {
        case UShortConstantNoDataCellType => rec.put("noDataValue", 0)
        case UShortUserDefinedNoDataCellType(nd) => rec.put("noDataValue", nd.toInt)
        case UShortCellType => rec.put("noDataValue", null)
        case _ => sys.error(s"Cell type ${tile.cellType} was unexpected")
      }
    }

    def decode(rec: GenericRecord) = {
      val array  = rec.get("cells")
        .asInstanceOf[java.util.Collection[Int]]
        .asScala // notice that Avro does not have native support for Short primitive
        .map(_.toShort)
        .toArray
      val cellType = Option(rec[Int]("noDataValue")) match {
        case Some(nd) if nd == 0 => UShortConstantNoDataCellType
        case Some(nd) => UShortUserDefinedNoDataCellType(nd.toShort)
        case None => UShortCellType
      }
      UShortArrayTile(array, rec[Int]("cols"), rec[Int]("rows"), cellType)
    }
  }

  implicit def intArrayTileCodec: AvroRecordCodec[IntArrayTile] = new AvroRecordCodec[IntArrayTile] {
    def schema = SchemaBuilder
      .record("IntArrayTile").namespace("geotrellis.raster")
      .fields()
      .name("cols").`type`().intType().noDefault()
      .name("rows").`type`().intType().noDefault()
      .name("cells").`type`().array().items().intType().noDefault()
      .name("noDataValue").`type`().unionOf().intType().and().nullType().endUnion().intDefault(Int.MinValue)
      .endRecord()

    def encode(tile: IntArrayTile, rec: GenericRecord) = {
      rec.put("cols", tile.cols)
      rec.put("rows", tile.rows)
      rec.put("cells", java.util.Arrays.asList(tile.array:_*))
      tile.cellType match {
        case IntConstantNoDataCellType => rec.put("noDataValue", NODATA)
        case IntUserDefinedNoDataCellType(nd) => rec.put("noDataValue", nd)
        case IntCellType => rec.put("noDataValue", null)
        case _ => sys.error(s"Cell type ${tile.cellType} was unexpected")
      }
    }

    def decode(rec: GenericRecord) = {
      val array  = rec.get("cells")
        .asInstanceOf[java.util.Collection[Int]]
        .asScala
        .toArray[Int]
      val cellType = Option(rec[Int]("noDataValue")) match {
        case Some(nd) if isNoData(nd) => IntConstantNoDataCellType
        case Some(nd) => IntUserDefinedNoDataCellType(nd)
        case None => IntCellType
      }
      IntArrayTile(array, rec[Int]("cols"), rec[Int]("rows"), cellType)
    }
  }

  /** Avro serialization doesn't support Float.NaN or Double.NaN. Whereas a
    * union of number and null is sufficient in cases where the nodata value
    * for some domain is can be serialized (Int.MinValue is just another
    * integer and, therefore, serializable without difficulty), we are in need
    * of an alternative strategy for floating point serialization.
    *
    * To this end, we've serialized with a union of boolean and floating point
    * values.
    * noDataValue can either be:
    * 1. true (and, therefore, ConstantNoData)
    * 2. false (NoNoData)
    * - OR -
    * 3. a floating point value (which is a UserDefinedNoDataValue's value)
    */
  implicit def floatArrayTileCodec: AvroRecordCodec[FloatArrayTile] = new AvroRecordCodec[FloatArrayTile] {
    def schema = SchemaBuilder
      .record("FloatArrayTile").namespace("geotrellis.raster")
      .fields()
      .name("cols").`type`().intType().noDefault()
      .name("rows").`type`().intType().noDefault()
      .name("cells").`type`().array().items().floatType().noDefault()
      .name("noDataValue").`type`().unionOf().booleanType().and().floatType().endUnion().booleanDefault(true)
      .endRecord()

    def encode(tile: FloatArrayTile, rec: GenericRecord) = {
      rec.put("cols", tile.cols)
      rec.put("rows", tile.rows)
      rec.put("cells", java.util.Arrays.asList(tile.array:_*))
      tile.cellType match {
        case FloatConstantNoDataCellType => rec.put("noDataValue", true)
        case FloatUserDefinedNoDataCellType(nd) => rec.put("noDataValue", nd)
        case FloatCellType => rec.put("noDataValue", false)
        case _ => sys.error(s"Cell type ${tile.cellType} was unexpected")
      }
    }

    def decode(rec: GenericRecord) = {
      val array  = rec.get("cells")
        .asInstanceOf[java.util.Collection[Float]]
        .asScala
        .toArray[Float]
      val cellType =
        Try {
          if (rec[Boolean]("noDataValue") == true) FloatConstantNoDataCellType
          else FloatCellType
        } getOrElse {
          FloatUserDefinedNoDataCellType(rec[Float]("noDataValue"))
        }
      FloatArrayTile(array, rec[Int]("cols"), rec[Int]("rows"), cellType)
    }
  }

  /** Avro serialization doesn't support Float.NaN or Double.NaN. Whereas a
    * union of number and null is sufficient in cases where the nodata value
    * for some domain is can be serialized (Int.MinValue is just another
    * integer and, therefore, serializable without difficulty), we are in need
    * of an alternative strategy for floating point serialization.
    *
    * To this end, we've serialized with a union of boolean and floating point
    * values.
    * noDataValue can either be:
    * 1. true (and, therefore, ConstantNoData)
    * 2. false (NoNoData)
    * - OR -
    * 3. a floating point value (which is a UserDefinedNoDataValue's value)
    */
  implicit def doubleArrayTileCodec: AvroRecordCodec[DoubleArrayTile] = new AvroRecordCodec[DoubleArrayTile] {
    def schema = SchemaBuilder
      .record("DoubleArrayTile").namespace("geotrellis.raster")
      .fields()
      .name("cols").`type`().intType().noDefault()
      .name("rows").`type`().intType().noDefault()
      .name("cells").`type`().array().items().doubleType().noDefault()
      .name("noDataValue").`type`().unionOf().booleanType().and().doubleType().endUnion().booleanDefault(true)
      .endRecord()

    def encode(tile: DoubleArrayTile, rec: GenericRecord) = {
      rec.put("cols", tile.cols)
      rec.put("rows", tile.rows)
      rec.put("cells", java.util.Arrays.asList(tile.array:_*))
      tile.cellType match {
        case DoubleConstantNoDataCellType => rec.put("noDataValue", true)
        case DoubleUserDefinedNoDataCellType(nd) => rec.put("noDataValue", nd)
        case DoubleCellType => rec.put("noDataValue", false)
        case _ => sys.error(s"Cell type ${tile.cellType} was unexpected")
      }
    }

    def decode(rec: GenericRecord) = {
      val array  = rec.get("cells")
        .asInstanceOf[java.util.Collection[Double]]
        .asScala
        .toArray[Double]
      val cellType =
        Try {
          if (rec[Boolean]("noDataValue") == true) DoubleConstantNoDataCellType
          else DoubleCellType
        } getOrElse {
          DoubleUserDefinedNoDataCellType(rec[Double]("noDataValue"))
        }
      DoubleArrayTile(array, rec[Int]("cols"), rec[Int]("rows"), cellType)
    }
  }

  implicit def byteArrayTileCodec: AvroRecordCodec[ByteArrayTile] = new AvroRecordCodec[ByteArrayTile] {
    def schema = SchemaBuilder
      .record("ByteArrayTile").namespace("geotrellis.raster")
      .fields()
      .name("cols").`type`().intType().noDefault()
      .name("rows").`type`().intType().noDefault()
      .name("cells").`type`().bytesType().noDefault()
      .name("noDataValue").`type`().unionOf().intType().and().nullType().endUnion().intDefault(Byte.MinValue.toInt)
      .endRecord()

    def encode(tile: ByteArrayTile, rec: GenericRecord) = {
      rec.put("cols", tile.cols)
      rec.put("rows", tile.rows)
      rec.put("cells", ByteBuffer.wrap(tile.array))
      tile.cellType match {
        case ByteConstantNoDataCellType => rec.put("noDataValue", byteNODATA.toInt)
        case ByteUserDefinedNoDataCellType(nd) => rec.put("noDataValue", nd.toInt)
        case ByteCellType => rec.put("noDataValue", null)
        case _ => sys.error(s"Cell type ${tile.cellType} was unexpected")
      }
    }

    def decode(rec: GenericRecord) = {
      val array  = rec.get("cells")
        .asInstanceOf[ByteBuffer]
        .array()
      val cellType = Option(rec[Int]("noDataValue")) match {
        case Some(nd) if nd == Byte.MinValue.toInt => ByteConstantNoDataCellType
        case Some(nd) => ByteUserDefinedNoDataCellType(nd.toByte)
        case None => ByteCellType
      }
      ByteArrayTile(array, rec[Int]("cols"), rec[Int]("rows"), cellType)
    }
  }

  implicit def uByteArrayTileCodec: AvroRecordCodec[UByteArrayTile] = new AvroRecordCodec[UByteArrayTile] {
    def schema = SchemaBuilder
      .record("UByteArrayTile").namespace("geotrellis.raster")
      .fields()
      .name("cols").`type`().intType().noDefault()
      .name("rows").`type`().intType().noDefault()
      .name("cells").`type`().bytesType().noDefault()
      .name("noDataValue").`type`().unionOf().intType().and().nullType().endUnion().intDefault(0)
      .endRecord()

    def encode(tile: UByteArrayTile, rec: GenericRecord) = {
      rec.put("cols", tile.cols)
      rec.put("rows", tile.rows)
      rec.put("cells", ByteBuffer.wrap(tile.array))
      tile.cellType match {
        case UByteConstantNoDataCellType => rec.put("noDataValue", 0)
        case UByteUserDefinedNoDataCellType(nd) => rec.put("noDataValue", nd.toInt)
        case UByteCellType => rec.put("noDataValue", null)
        case _ => sys.error(s"Cell type ${tile.cellType} was unexpected")
      }
    }

    def decode(rec: GenericRecord) = {
      val array  = rec.get("cells")
        .asInstanceOf[ByteBuffer]
        .array()
      val cellType = Option(rec[Int]("noDataValue")) match {
        case Some(nd) if nd == 0 => UByteConstantNoDataCellType
        case Some(nd) => UByteUserDefinedNoDataCellType(nd.toByte)
        case None => UByteCellType
      }
      UByteArrayTile(array, rec[Int]("cols"), rec[Int]("rows"), cellType)
    }
  }

  implicit def bitArrayTileCodec: AvroRecordCodec[BitArrayTile] = new AvroRecordCodec[BitArrayTile] {
    def schema = SchemaBuilder
      .record("BitArrayTile").namespace("geotrellis.raster")
      .fields()
      .name("cols").`type`().intType().noDefault()
      .name("rows").`type`().intType().noDefault()
      .name("cells").`type`().bytesType().noDefault()
      .endRecord()

    def encode(tile: BitArrayTile, rec: GenericRecord) = {
      rec.put("cols", tile.cols)
      rec.put("rows", tile.rows)
      rec.put("cells", ByteBuffer.wrap(tile.array))
    }

    def decode(rec: GenericRecord) = {
      val array  = rec.get("cells").asInstanceOf[ByteBuffer].array()
      BitArrayTile(array, rec[Int]("cols"), rec[Int]("rows"))
    }
  }

  implicit def multibandTileCodec: AvroRecordCodec[MultibandTile] = new AvroRecordCodec[MultibandTile] {
    def schema = SchemaBuilder
      .record("ArrayMultibandTile").namespace("geotrellis.raster")
      .fields()
      .name("bands").`type`().array().items.`type`(tileUnionCodec.schema).noDefault()
      .endRecord()

    def encode(tile: MultibandTile, rec: GenericRecord) = {
      val bands = for (i <- 0 until tile.bandCount) yield tile.band(i)
      rec.put("bands", bands.map(tileUnionCodec.encode).asJavaCollection)
    }

    def decode(rec: GenericRecord) = {
      val bands = rec.get("bands")
        .asInstanceOf[java.util.Collection[GenericRecord]]
        .asScala // notice that Avro does not have native support for Short primitive
        .map(tileUnionCodec.decode)
        .toArray

      ArrayMultibandTile(bands)
    }
  }
}

object TileCodecs extends TileCodecs
