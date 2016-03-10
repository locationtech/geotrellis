package geotrellis.spark.io.avro

import geotrellis.spark.GridKey
import geotrellis.spark.io.avro.codecs.{KeyValueRecordCodec, TileCodecs, KeyCodecs}
import TileCodecs._
import KeyCodecs._
import org.scalatest._
import geotrellis.raster._

class TileRecordSpec extends FunSpec with AvroTools with Matchers {
  describe("TileRecordCodecs") {
    it("encodes (key,tile) pairs"){
      val pairs = Vector(
        GridKey(1,2) -> DoubleArrayTile.fill(1,10,12),
        GridKey(3,6) -> DoubleArrayTile.fill(2,10,12),
        GridKey(3,4) -> DoubleArrayTile.fill(3,10,12)
      )

      val codec = new KeyValueRecordCodec[GridKey, DoubleArrayTile]

      roundTrip(pairs)(codec)
    }
  }
}
