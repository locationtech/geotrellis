package geotrellis.spark.io.avro

import org.scalatest._
import TileCodecs._
import geotrellis.raster._

class TileCodecsSpec extends FunSpec {
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
  }
}
