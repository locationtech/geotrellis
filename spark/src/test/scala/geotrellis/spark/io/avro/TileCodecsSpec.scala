package geotrellis.spark.io.avro

import geotrellis.spark.io.avro.codecs.TileCodecs
import org.scalatest._
import TileCodecs._
import geotrellis.raster._

class TileCodecsSpec extends FunSpec with Matchers with AvroTools  {
  describe("TileCodecs") {
    it("encodes ByteArrayTile"){
      roundTrip(ByteArrayTile.fill(127,10,15))
    }
    it("encodes UByteArrayTile"){
      roundTrip(UByteArrayTile.fill(127,10,15))
    }
    it("encodes ShortArrayTile"){
      roundTrip(ShortArrayTile.fill(45,10,15))
    }
    it("encodes UShortArrayTile"){
      roundTrip(UShortArrayTile.fill(45,10,15))
    }
    it("encodes IntArrayTile"){
      roundTrip(IntArrayTile.fill(45,10,15))
    }
    it("encodes FloatArrayTile"){
      roundTrip(FloatArrayTile.fill(532.4f,10,15))
    }
    it("encodes DoubleArrayTile"){
      roundTrip(DoubleArrayTile.fill(53232322.4,10,15))
    }

    it("encodes ArrayMultibandTile"){
      val tiles= for (i <- 0 to 3) yield DoubleArrayTile.fill(53232322.4,10,15)
      val thing = ArrayMultibandTile(tiles): MultibandTile
      roundTrip(thing)
//      val bytes = AvroEncoder.toBinary(thing)
//      val fromBytes = AvroEncoder.fromBinary[MultibandTile](bytes)
//      fromBytes should be equals (thing)
    }
  }

  describe("No NoData TileCodecs") {
    it("encodes ByteArrayTile"){
      roundTrip(ByteArrayTile.fill(127,10,15, ByteCellType))
    }
    it("encodes UByteArrayTile"){
      roundTrip(UByteArrayTile.fill(127,10,15, UByteCellType))
    }
    it("encodes ShortArrayTile"){
      roundTrip(ShortArrayTile.fill(45,10,15, ShortCellType))
    }
    it("encodes UShortArrayTile"){
      roundTrip(UShortArrayTile.fill(45,10,15, UShortCellType))
    }
    it("encodes IntArrayTile"){
      roundTrip(IntArrayTile.fill(45,10,15, IntCellType))
    }
    it("encodes FloatArrayTile"){
      roundTrip(FloatArrayTile.fill(532.4f,10,15, FloatCellType))
    }
    it("encodes DoubleArrayTile"){
      roundTrip(DoubleArrayTile.fill(53232322.4,10,15, DoubleCellType))
    }
    it("encodes ArrayMultibandTile"){
      val tiles= for (i <- 0 to 3) yield DoubleArrayTile.fill(53232322.4,10,15, DoubleCellType)
      val thing = ArrayMultibandTile(tiles): MultibandTile
      roundTrip(thing)
    }
  }

  describe("UserDefined TileCodecs") {
    it("encodes ByteArrayTile"){
      roundTrip(ByteArrayTile.fill(127,10,15, ByteUserDefinedNoDataCellType(123)))
    }
    it("encodes UByteArrayTile"){
      roundTrip(UByteArrayTile.fill(127,10,15, UByteUserDefinedNoDataCellType(123)))
    }
    it("encodes ShortArrayTile"){
      roundTrip(ShortArrayTile.fill(45,10,15, ShortUserDefinedNoDataCellType(123)))
    }
    it("encodes UShortArrayTile"){
      roundTrip(UShortArrayTile.fill(45,10,15, UShortUserDefinedNoDataCellType(123)))
    }
    it("encodes IntArrayTile"){
      roundTrip(IntArrayTile.fill(45,10,15, IntUserDefinedNoDataCellType(123)))
    }
    it("encodes FloatArrayTile"){
      roundTrip(FloatArrayTile.fill(532.4f,10,15, FloatUserDefinedNoDataCellType(2.2F)))
    }
    it("encodes DoubleArrayTile"){
      roundTrip(DoubleArrayTile.fill(53232322.4,10,15, DoubleUserDefinedNoDataCellType(2.2)))
    }
    it("encodes ArrayMultibandTile"){
      val tiles= for (i <- 0 to 3) yield DoubleArrayTile.fill(53232322.4,10,15, DoubleUserDefinedNoDataCellType(42.23))
      val thing = ArrayMultibandTile(tiles): MultibandTile
      roundTrip(thing)
    }
  }
}
