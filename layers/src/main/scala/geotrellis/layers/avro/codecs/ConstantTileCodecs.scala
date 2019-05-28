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

import geotrellis.raster._
import geotrellis.layers.avro._
import geotrellis.layers.avro.codecs.Implicits._

import org.apache.avro.SchemaBuilder
import org.apache.avro.generic._

import scala.collection.JavaConverters._
import scala.util.Try

trait ConstantTileCodecs {
  implicit def bitConstantTileCodec: AvroRecordCodec[BitConstantTile] = new AvroRecordCodec[BitConstantTile] {
    def schema = SchemaBuilder
      .record("BitConstantTile").namespace("geotrellis.raster")
      .fields()
      .name("cols").`type`().intType().noDefault()
      .name("rows").`type`().intType().noDefault()
      .name("cell").`type`().booleanType().noDefault()
      .endRecord()

    def encode(tile: BitConstantTile, rec: GenericRecord) = {
      rec.put("cols", tile.cols)
      rec.put("rows", tile.rows)
      rec.put("cell", tile.v)
    }

    def decode(rec: GenericRecord) =
      BitConstantTile(rec[Boolean]("cell"), rec[Int]("cols"), rec[Int]("rows"))
  }

  implicit def byteConstantTileCodec: AvroRecordCodec[ByteConstantTile] = new AvroRecordCodec[ByteConstantTile] {
    def schema = SchemaBuilder
      .record("ByteConstantTile").namespace("geotrellis.raster")
      .fields()
      .name("cols").`type`().intType().noDefault()
      .name("rows").`type`().intType().noDefault()
      .name("cell").`type`().intType().noDefault()
      .name("noDataValue").`type`().unionOf().intType().and().nullType().endUnion().intDefault(Byte.MinValue.toInt)
      .endRecord()

    def encode(tile: ByteConstantTile, rec: GenericRecord) = {
      rec.put("cols", tile.cols)
      rec.put("rows", tile.rows)
      rec.put("cell", tile.v.toInt)
      tile.cellType match {
        case ByteConstantNoDataCellType => rec.put("noDataValue", byteNODATA.toInt)
        case ByteUserDefinedNoDataCellType(nd) => rec.put("noDataValue", nd.toInt)
        case ByteCellType => rec.put("noDataValue", null)
        case _ => sys.error(s"Cell type ${tile.cellType} was unexpected")
      }
    }

    def decode(rec: GenericRecord) = {
      val cellType = Option(rec[Int]("noDataValue")) match {
        case Some(nd) if nd == Byte.MinValue.toInt => ByteConstantNoDataCellType
        case Some(nd) => ByteUserDefinedNoDataCellType(nd.toByte)
        case None => ByteCellType
      }
      ByteConstantTile(rec[Int]("cell").toByte, rec[Int]("cols"), rec[Int]("rows"), cellType)
    }
  }

  implicit def uByteConstantTileCodec: AvroRecordCodec[UByteConstantTile] = new AvroRecordCodec[UByteConstantTile] {
    def schema = SchemaBuilder
      .record("UByteConstantTile").namespace("geotrellis.raster")
      .fields()
      .name("cols").`type`().intType().noDefault()
      .name("rows").`type`().intType().noDefault()
      .name("cell").`type`().intType().noDefault()
      .name("noDataValue").`type`().unionOf().intType().and().nullType().endUnion().intDefault(0)
      .endRecord()

    def encode(tile: UByteConstantTile, rec: GenericRecord) = {
      rec.put("cols", tile.cols)
      rec.put("rows", tile.rows)
      rec.put("cell", tile.v.toInt)
      tile.cellType match {
        case UByteConstantNoDataCellType => rec.put("noDataValue", 0)
        case UByteUserDefinedNoDataCellType(nd) => rec.put("noDataValue", nd.toInt)
        case UByteCellType => rec.put("noDataValue", null)
        case _ => sys.error(s"Cell type ${tile.cellType} was unexpected")
      }
    }

    def decode(rec: GenericRecord) = {
      val cellType = Option(rec[Int]("noDataValue")) match {
        case Some(nd) if nd == 0 => UByteConstantNoDataCellType
        case Some(nd) => UByteUserDefinedNoDataCellType(nd.toByte)
        case None => UByteCellType
      }
      UByteConstantTile(rec[Int]("cell").toByte, rec[Int]("cols"), rec[Int]("rows"), cellType)
    }
  }

  implicit def shortConstantTileCodec: AvroRecordCodec[ShortConstantTile] = new AvroRecordCodec[ShortConstantTile] {
    def schema = SchemaBuilder
      .record("ShortConstantTile").namespace("geotrellis.raster")
      .fields()
      .name("cols").`type`().intType().noDefault()
      .name("rows").`type`().intType().noDefault()
      .name("cell").`type`().intType().noDefault()
      .name("noDataValue").`type`().unionOf().intType().and().nullType().endUnion().intDefault(0)
      .endRecord()

    def encode(tile: ShortConstantTile, rec: GenericRecord) = {
      rec.put("cols", tile.cols)
      rec.put("rows", tile.rows)
      rec.put("cell", tile.v)
      tile.cellType match {
        case ShortConstantNoDataCellType => rec.put("noDataValue", Short.MinValue.toInt)
        case ShortUserDefinedNoDataCellType(nd) => rec.put("noDataValue", nd.toInt)
        case ShortCellType => rec.put("noDataValue", null)
        case _ => sys.error(s"Cell type ${tile.cellType} was unexpected")
      }
    }

    def decode(rec: GenericRecord) = {
      val cellType = Option(rec[Int]("noDataValue")) match {
        case Some(nd) if nd == Short.MinValue.toInt => ShortConstantNoDataCellType
        case Some(nd) => ShortUserDefinedNoDataCellType(nd.toShort)
        case None => ShortCellType
      }
      ShortConstantTile(rec[Int]("cell").toShort, rec[Int]("cols"), rec[Int]("rows"), cellType)
    }
  }

  implicit def uShortConstantTileCodec: AvroRecordCodec[UShortConstantTile] = new AvroRecordCodec[UShortConstantTile] {
    def schema = SchemaBuilder
      .record("UShortConstantTile").namespace("geotrellis.raster")
      .fields()
      .name("cols").`type`().intType().noDefault()
      .name("rows").`type`().intType().noDefault()
      .name("cell").`type`().intType().noDefault()
      .name("noDataValue").`type`().unionOf().intType().and().nullType().endUnion().intDefault(0)
      .endRecord()

    def encode(tile: UShortConstantTile, rec: GenericRecord) = {
      rec.put("cols", tile.cols)
      rec.put("rows", tile.rows)
      rec.put("cell", tile.v)
      tile.cellType match {
        case UShortConstantNoDataCellType => rec.put("noDataValue", 0)
        case UShortUserDefinedNoDataCellType(nd) => rec.put("noDataValue", nd.toInt)
        case UShortCellType => rec.put("noDataValue", null)
        case _ => sys.error(s"Cell type ${tile.cellType} was unexpected")
      }
    }

    def decode(rec: GenericRecord) = {
      val cellType = Option(rec[Int]("noDataValue")) match {
        case Some(nd) if nd == 0 => UShortConstantNoDataCellType
        case Some(nd) => UShortUserDefinedNoDataCellType(nd.toShort)
        case None => UShortCellType
      }
      UShortConstantTile(rec[Int]("cell").toShort, rec[Int]("cols"), rec[Int]("rows"), cellType)
    }
  }

  implicit def intConstantTileCodec: AvroRecordCodec[IntConstantTile] = new AvroRecordCodec[IntConstantTile] {
    def schema = SchemaBuilder
      .record("IntConstantTile").namespace("geotrellis.raster")
      .fields()
      .name("cols").`type`().intType().noDefault()
      .name("rows").`type`().intType().noDefault()
      .name("cell").`type`().intType().noDefault()
      .name("noDataValue").`type`().unionOf().intType().and().nullType().endUnion().intDefault(Int.MinValue)
      .endRecord()

    def encode(tile: IntConstantTile, rec: GenericRecord) = {
      rec.put("cols", tile.cols)
      rec.put("rows", tile.rows)
      rec.put("cell", tile.v)
      tile.cellType match {
        case IntConstantNoDataCellType => rec.put("noDataValue", NODATA)
        case IntUserDefinedNoDataCellType(nd) => rec.put("noDataValue", nd)
        case IntCellType => rec.put("noDataValue", null)
        case _ => sys.error(s"Cell type ${tile.cellType} was unexpected")
      }
    }

    def decode(rec: GenericRecord) = {
      val cellType = Option(rec[Int]("noDataValue")) match {
        case Some(nd) if isNoData(nd) => IntConstantNoDataCellType
        case Some(nd) => IntUserDefinedNoDataCellType(nd)
        case None => IntCellType
      }
      IntConstantTile(rec[Int]("cell"), rec[Int]("cols"), rec[Int]("rows"), cellType)
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
  implicit def floatConstantTileCodec: AvroRecordCodec[FloatConstantTile] = new AvroRecordCodec[FloatConstantTile] {
    def schema = SchemaBuilder
      .record("FloatConstantTile").namespace("geotrellis.raster")
      .fields()
      .name("cols").`type`().intType().noDefault()
      .name("rows").`type`().intType().noDefault()
      .name("cell").`type`().floatType().noDefault()
      .name("noDataValue").`type`().unionOf().booleanType().and().floatType().endUnion().booleanDefault(true)
      .endRecord()

    def encode(tile: FloatConstantTile, rec: GenericRecord) = {
      rec.put("cols", tile.cols)
      rec.put("rows", tile.rows)
      rec.put("cell", tile.v)
      tile.cellType match {
        case FloatConstantNoDataCellType => rec.put("noDataValue", true)
        case FloatUserDefinedNoDataCellType(nd) => rec.put("noDataValue", nd)
        case FloatCellType => rec.put("noDataValue", false)
        case _ => sys.error(s"Cell type ${tile.cellType} was unexpected")
      }
    }

    def decode(rec: GenericRecord) = {
      val cellType =
        Try {
          if (rec[Boolean]("noDataValue") == true) FloatConstantNoDataCellType
          else FloatCellType
        } getOrElse {
          FloatUserDefinedNoDataCellType(rec[Float]("noDataValue"))
        }
      FloatConstantTile(rec[Float]("cell"), rec[Int]("cols"), rec[Int]("rows"), cellType)
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
  implicit def doubleConstantTileCodec: AvroRecordCodec[DoubleConstantTile] = new AvroRecordCodec[DoubleConstantTile] {
    def schema = SchemaBuilder
      .record("DoubleConstantTile").namespace("geotrellis.raster")
      .fields()
      .name("cols").`type`().intType().noDefault()
      .name("rows").`type`().intType().noDefault()
      .name("cell").`type`().doubleType().noDefault()
      .name("noDataValue").`type`().unionOf().booleanType().and().doubleType().endUnion().booleanDefault(true)
      .endRecord()

    def encode(tile: DoubleConstantTile, rec: GenericRecord) = {
      rec.put("cols", tile.cols)
      rec.put("rows", tile.rows)
      rec.put("cell", tile.v)
      tile.cellType match {
        case DoubleConstantNoDataCellType => rec.put("noDataValue", true)
        case DoubleUserDefinedNoDataCellType(nd) => rec.put("noDataValue", nd)
        case DoubleCellType => rec.put("noDataValue", false)
        case _ => sys.error(s"Cell type ${tile.cellType} was unexpected")
      }
    }

    def decode(rec: GenericRecord) = {
      val cellType =
        Try {
          if (rec[Boolean]("noDataValue") == true) DoubleConstantNoDataCellType
          else DoubleCellType
        } getOrElse {
          DoubleUserDefinedNoDataCellType(rec[Double]("noDataValue"))
        }
      DoubleConstantTile(rec[Double]("cell"), rec[Int]("cols"), rec[Int]("rows"), cellType)
    }
  }
}

object ConstantTileCodecs extends ConstantTileCodecs
