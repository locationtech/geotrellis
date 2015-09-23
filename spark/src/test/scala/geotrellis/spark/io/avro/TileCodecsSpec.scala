package geotrellis.spark.io.avro

import geotrellis.spark.io.avro.codecs.TileCodecs
import org.scalatest._
import TileCodecs._
import geotrellis.raster._

class TileCodecsSpec extends FunSpec with Matchers with AvroTools  {
  describe("TileCodecs") {
    it("encodes ShortArrayTile"){
      roundTrip(ShortArrayTile.fill(45,10,15))
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
    it("encodes ByteArrayTile"){
      roundTrip(ByteArrayTile.fill(127,10,15))
    }
    it("encodes ArrayMultiBandTile"){
      val tiles= for (i <- 0 to 3) yield DoubleArrayTile.fill(53232322.4,10,15)
      val thing = ArrayMultiBandTile(tiles): MultiBandTile
      roundTrip(thing)
//      val bytes = AvroEncoder.toBinary(thing)
//      val fromBytes = AvroEncoder.fromBinary[MultiBandTile](bytes)
//      fromBytes should be equals (thing)
    }
  }
}
