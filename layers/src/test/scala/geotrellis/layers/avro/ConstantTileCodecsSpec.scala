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

package geotrellis.layers.avro

import geotrellis.layers.avro.codecs.Implicits._
import org.scalatest._
import geotrellis.raster._
import geotrellis.layers.avro.AvroTools._

class ConstantTileCodecsSpec extends FunSpec with Matchers with AvroTools  {
  describe("ConstantTileCodecs") {
    it("encodes BitConstantTile"){
      roundTripWithNoDataCheck(BitConstantTile(1,10,15))
    }
    it("encodes ByteConstantTile"){
      roundTripWithNoDataCheck(ByteConstantTile(127,10,15))
    }
    it("encodes UByteConstantTile"){
      roundTripWithNoDataCheck(UByteConstantTile(127,10,15))
    }
    it("encodes ShortConstantTile"){
      roundTripWithNoDataCheck(ShortConstantTile(45,10,15))
    }
    it("encodes UShortConstantTile"){
      roundTripWithNoDataCheck(UShortConstantTile(45,10,15))
    }
    it("encodes IntConstantTile"){
      roundTripWithNoDataCheck(IntConstantTile(45,10,15))
    }
    it("encodes FloatConstantTile"){
      roundTripWithNoDataCheck(FloatConstantTile(532.4f,10,15))
    }
    it("encodes DoubleConstantTile"){
      roundTripWithNoDataCheck(DoubleConstantTile(53232322.4,10,15))
    }

    it("encodes ArrayMultibandTile"){
      val tiles= for (i <- 0 to 3) yield DoubleConstantTile(53232322.4,10,15)
      val thing = ArrayMultibandTile(tiles): MultibandTile
      roundTripWithNoDataCheck(thing)
//      val bytes = AvroEncoder.toBinary(thing)
//      val fromBytes = AvroEncoder.fromBinary[MultibandTile](bytes)
//      fromBytes should be equals (thing)
    }
  }

  describe("No NoData TileCodecs") {
    it("encodes ByteConstantTile"){
      roundTripWithNoDataCheck(ByteConstantTile(127,10,15, ByteCellType))
    }
    it("encodes UByteConstantTile"){
      roundTripWithNoDataCheck(UByteConstantTile(127,10,15, UByteCellType))
    }
    it("encodes ShortConstantTile"){
      roundTripWithNoDataCheck(ShortConstantTile(45,10,15, ShortCellType))
    }
    it("encodes UShortConstantTile"){
      roundTripWithNoDataCheck(UShortConstantTile(45,10,15, UShortCellType))
    }
    it("encodes IntConstantTile"){
      roundTripWithNoDataCheck(IntConstantTile(45,10,15, IntCellType))
    }
    it("encodes FloatConstantTile"){
      roundTripWithNoDataCheck(FloatConstantTile(532.4f,10,15, FloatCellType))
    }
    it("encodes DoubleConstantTile"){
      roundTripWithNoDataCheck(DoubleConstantTile(53232322.4,10,15, DoubleCellType))
    }
    it("encodes ArrayMultibandTile"){
      val tiles= for (i <- 0 to 3) yield DoubleConstantTile(53232322.4,10,15, DoubleCellType)
      val thing = ArrayMultibandTile(tiles): MultibandTile
      roundTripWithNoDataCheck(thing)
    }
  }

  describe("UserDefined TileCodecs") {
    it("encodes ByteConstantTile"){
      roundTripWithNoDataCheck(ByteConstantTile(127,10,15, ByteUserDefinedNoDataCellType(123)))
    }
    it("encodes UByteConstantTile"){
      roundTripWithNoDataCheck(UByteConstantTile(127,10,15, UByteUserDefinedNoDataCellType(123)))
    }
    it("encodes ShortConstantTile"){
      roundTripWithNoDataCheck(ShortConstantTile(45,10,15, ShortUserDefinedNoDataCellType(123)))
    }
    it("encodes UShortConstantTile"){
      roundTripWithNoDataCheck(UShortConstantTile(45,10,15, UShortUserDefinedNoDataCellType(123)))
    }
    it("encodes IntConstantTile"){
      roundTripWithNoDataCheck(IntConstantTile(45,10,15, IntUserDefinedNoDataCellType(123)))
    }
    it("encodes FloatConstantTile"){
      roundTripWithNoDataCheck(FloatConstantTile(532.4f,10,15, FloatUserDefinedNoDataCellType(2.2F)))
    }
    it("encodes DoubleConstantTile"){
      roundTripWithNoDataCheck(DoubleConstantTile(53232322.4,10,15, DoubleUserDefinedNoDataCellType(2.2)))
    }
    it("encodes ArrayMultibandTile"){
      val tiles= for (i <- 0 to 3) yield DoubleConstantTile(53232322.4,10,15, DoubleUserDefinedNoDataCellType(42.23))
      val thing = ArrayMultibandTile(tiles): MultibandTile
      roundTripWithNoDataCheck(thing)
    }
  }
}
